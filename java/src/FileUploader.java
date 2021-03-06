import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTimeZone;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by Shakib on 6/28/2016.
 *
 * The purpose of this class is to upload files that were downloaded to the working directory using the main.PostImporter
 * class. The upload function takes the post ID as input and searches for this post in the downloads folder
 * of the project directory (which is the default location of post downloads). It then determines what kind of
 * files are contained within each post, and then chooses which service to upload them to. Video files are uploaded to
 * SproutVideo, audio files are uploaded to SoundCloud, and the rest are uploaded to S3. Last, it inserts the information
 * about the post into the database using the main.DynamoDBClient.
 *
 */
public class FileUploader {

    private static Logger log = Logger.getLogger(FileUploader.class.getName());
    private AmazonS3 s3Client;
    private DynamoDBClient client;
    private File metadata;
    private HashMap<String, Object> postDetails;
    private String dalnId, s3Bucket;
    private StatusMessages message;
    private boolean verboseOutput;
    private String[] singleEntryFields, multiEntryFields;

    public FileUploader(String dalnId, boolean verboseOutput) throws IOException {
        this.verboseOutput = verboseOutput;
        message = new StatusMessages();
        if(!verboseOutput) log.info(message.FileUploadPostBeginLog(dalnId));
        /**Connect to S3**/
        GetPropertyValues propertyValues = new GetPropertyValues();
        final HashMap<String, String> credentials =  propertyValues.getAWSCredentials();

        s3Client = new AmazonS3Client(new AWSCredentials() {
            @Override
            public String getAWSAccessKeyId() {
                return credentials.get("AWSAccessKey");
            }

            @Override
            public String getAWSSecretKey() {
                return credentials.get("AWSSecretKey");
            }
        });

        s3Bucket = credentials.get("S3Bucket");

        /**Connect to DynamoDB**/
        client = new DynamoDBClient();

        PostImporter postImporter = new PostImporter();
        postImporter.initializeFields();
        singleEntryFields = postImporter.getSingleEntryFields();
        multiEntryFields = postImporter.getMultiEntryFields();

        this.dalnId = dalnId;
        postDetails = new HashMap<>();
        postDetails = getPostDetails();
        if(postDetails != null)
            uploadPost();
        else
            if (verboseOutput)log.error(message.CannotFindPostToUpload()); else log.error(message.FileUploadPostErrorLog(dalnId));
    }



    /**Extract fields from the metadata generated from main.PostImporter so that we can use them as inputs for uploads
     * and DB entry. All the needed information is stored in a single HashMap.**/
    public HashMap<String, Object> getPostDetails()
    {
        String metadataPath = "downloads/" + dalnId + "/Post" + dalnId + ".xml";
        metadata = new File(metadataPath);
        Document doc = null;
        try {
            doc = Jsoup.parse(metadata, "UTF-8"); //Scanner object to read file
        } catch (IOException e) {
            //if(verboseOutput) log.error(message.CannotFindPostToUpload());
            return null;
        }

        if(doc == null) return null;
        //Post and file details are gathered from the xml metadata
        //Details will all be stored in a single hashmap
        Element root = doc.child(0);

        Element field;
        for(String fieldName : singleEntryFields)
            if ((field = root.select(fieldName).first()) != null) {
                postDetails.put(fieldName, field.text());
            }

        for(String fieldName : multiEntryFields)
            if ((field = root.select(fieldName).first()) != null) {
                ArrayList<String> listOfValues = new ArrayList<>();
                for (Element child : field.children())
                    listOfValues.add(child.text());

                postDetails.put(fieldName, listOfValues);
            }

        ArrayList<String> fileNames = new ArrayList<>(), fileLinks = new ArrayList<>(),
                fileDescriptions = new ArrayList<>(), fileTypes = new ArrayList<>();
        int numberOfAssets;

        Elements allFiles = doc.select("files").first().children();
        numberOfAssets = allFiles.size();
        for(Element file : allFiles)
        {
            fileNames.add(file.select("fileName").first().text());
            //File types will determine which service to use for upload
            fileTypes.add(checkFiletype(file.select("fileName").first().text()));
            fileLinks.add(file.select("fileLink").first().text());

            Element fileDescriptionField = file.select("fileDescription").first();
            if(fileDescriptionField == null)
                fileDescriptions.add("None");
            else
                fileDescriptions.add(fileDescriptionField.text());
        }

        postDetails.put("DalnId", dalnId);
        postDetails.put("NumberOfAssets", numberOfAssets);
        postDetails.put("fileNames", fileNames);
        postDetails.put("fileTypes", fileTypes);
        postDetails.put("fileDescriptions", fileDescriptions);


        return postDetails;
    }

    public void uploadPost() throws IOException {
        /*if(client.checkIfIDAlreadyExistsInDB(postID))
        {
            if(client.areAllFilesUploaded(postID))
            {
                if(verboseOutput) log.error(message.PostAlreadyExistsInDB()); else log.error(message.FileUploadPostErrorLog(postID));
                return;
            }else
            {
                log.info("This post exists but not all media is present. Re-uploading post.");
                client.deletePost(postID);
            }
        }*/
        Post post = client.getPostFromTableUsingDALNId(dalnId);
        boolean dalnIdAlreadyExists = false;
        String postId = "";
        if(post != null)
        {
            dalnIdAlreadyExists = true;
            postId = post.getPostId();
            //log.info("1:" + postId);
            if(client.areAllFilesUploaded(post))
            {
                if(verboseOutput) log.error(message.PostAlreadyExistsInDB()); else log.error(message.FileUploadPostErrorLog(dalnId));
                log.info("This post is fully uploaded, so the post's files will only be uploaded to S3 and the record will be updated.");
            }
            else
            {
                log.info("This post exists but not all media is present. Re-uploading post.");
                //client.deletePost(post);
            }
        }

            if (verboseOutput) log.info(message.BeginPostUpload(dalnId));

            /**The first step is to create a folder in S3 specific to this post and include its metadata**/
            if (verboseOutput) log.info(message.CreateS3Data());
            uploadPostFolder();

            /**Every file that the post contains will now be uploaded to a service based on its file type. Each
             * file will be issued a random UUID and each file will return a location after its upload. These two lists
             * will be added to the postDetails HashMap once all the files are uploaded, as these two details are not
             * defined currently.**/
            ArrayList<String> fileUUIDs = new ArrayList<>();
            ArrayList<String> fileLocations = new ArrayList<>();
            ArrayList<String> fileEmbedLinks = new ArrayList<>();
            ArrayList<String> fileS3Links = new ArrayList<>();
            ArrayList<Boolean> fileUploadStatuses = new ArrayList<>();

            ArrayList<String> fileNames = (ArrayList<String>) postDetails.get("fileNames");
            ArrayList<String> fileTypes = (ArrayList<String>) postDetails.get("fileTypes");
            ArrayList<String> fileDescriptions = (ArrayList<String>) postDetails.get("fileDescriptions");
            int numberOfAssets = (Integer) postDetails.get("NumberOfAssets");
            for (int i = 0; i < numberOfAssets; i++) {
                String currentFileName = fileNames.get(i);

                //if (!verboseOutput) log.info(message.FileUploadAssetBeginLog(assetID));

                postDetails.put("Current File", currentFileName);
                //upload every file to S3
                if (verboseOutput) message.UploadingToS3(currentFileName);
                UploadToS3 S3Uploader = new UploadToS3(postDetails);
                fileS3Links.add(S3Uploader.getS3FileLocation()[0]);


                //Generate a new asset ID only if it's a new post
                //Generating a new id for the asset
                String assetID = "";
                if(!dalnIdAlreadyExists)
                {
                    //do
                        assetID = UUID.randomUUID().toString();
                    //while (client.checkIfUUIDExists(assetID));
                    fileUUIDs.add(assetID);
                    postDetails.put("Current Asset ID", assetID);
                }
                else
                    fileUUIDs.add(post.getAssetList().get(i).get("assetID"));


                if (verboseOutput) log.info("File Uploaded to S3.");
                else log.info(message.FileUploadAssetCompleteLog(assetID));

                //if this post hasn't been uploaded yet, then upload to CDNs
                if(!dalnIdAlreadyExists)
                {
                    //if the file is a video, also upload it to sproutvideo
                    if (fileTypes.get(i).equals("Audio/Video")) {

                        if (verboseOutput) log.info(message.UploadingToSproutVideo(currentFileName, assetID));
                        UploadToSproutVideo SVUploader = new UploadToSproutVideo(postDetails);
                        fileLocations.add(SVUploader.getSpoutVideoLocation()[0]);
                        fileEmbedLinks.add(SVUploader.getSpoutVideoLocation()[1]);
                        if (verboseOutput) log.info("Video Uploaded.");
                        else log.info(message.FileUploadAssetCompleteLog(assetID));
                    } else if (fileTypes.get(i).equals("Audio")) {
                        //if the file is an audio, also upload it to soundcloud
                        if (verboseOutput) log.info(message.UploadingToSoundCloud(currentFileName, assetID));


                        UploadToSoundCloud SCUploader = new UploadToSoundCloud(postDetails);



                        fileLocations.add(SCUploader.getSoundLocation()[0]);
                        fileEmbedLinks.add(SCUploader.getSoundLocation()[1]);
                        if (verboseOutput) log.info("Audio Uploaded.");
                        else log.info(message.FileUploadAssetCompleteLog(assetID));
                    } else {
                        //if the file isn't a video or an audio, all 3 links are the same
                        fileLocations.add(S3Uploader.getS3FileLocation()[0]);
                        fileEmbedLinks.add(S3Uploader.getS3FileLocation()[1]);
                    }
                }
                else //if the post has already been uploaded, then get its current values
                {
                    fileLocations.add(post.getAssetList().get(i).get("assetLocation"));
                    fileEmbedLinks.add(post.getAssetList().get(i).get("assetEmbedLink"));
                }
            }

            /**Gather the generated information about the post's assets to be inserted into the DB**/
            //All of the other post details were gathered from the metadata earlier. The last field we need
            //for the DB entry is the asset list, which contains details for each asset.
            List<HashMap<String, String>> assetList = new ArrayList<>();
            for (int j = 0; j < numberOfAssets; j++) {
                HashMap<String, String> asset = new HashMap<>();
                asset.put("assetID", fileUUIDs.get(j));
                asset.put("assetName", fileNames.get(j));
                asset.put("assetLocation", fileLocations.get(j));
                asset.put("assetEmbedLink", fileEmbedLinks.get(j));
                asset.put("assetS3Link", fileS3Links.get(j));
                asset.put("assetDescription", fileDescriptions.get(j));
                asset.put("assetType", fileTypes.get(j));
                assetList.add(asset);

                if (fileLocations.get(j).equals("") || fileLocations.get(j) == null)
                    fileUploadStatuses.add(false);
                else
                    fileUploadStatuses.add(true);
            }
            postDetails.put("assetList", assetList); //this list includes generated asset IDs, locations, and filetypes
            postDetails.put("fileUploadStatuses", fileUploadStatuses);
            postDetails.put("isPostNotApproved", false);

            //the insert post method returns the randomly generated post UUID.
            if(post != null)
            {
                postDetails.put("postId", postId);
                //log.info("#2" + postId);
            }
            client.insertPost(postDetails);
            deleteFolder();

            if (verboseOutput) log.info(message.FileUploadPostCompleteVerbose(dalnId));
            else log.info(message.FileUploadPostCompleteLog(dalnId));
    }

    /**Upload post folder and metadata to S3**/
    private void uploadPostFolder()
    {
        //data for folder
        ObjectMetadata folderMetadata = new ObjectMetadata();
        folderMetadata.setContentLength(0);
        InputStream emptyContent = new ByteArrayInputStream(new byte[0]);

        //PutObjectRequest used for creating an object to be uploaded
        PutObjectRequest putObjectRequest = new PutObjectRequest(s3Bucket,
                "Posts/" + dalnId + "/", emptyContent, folderMetadata);
        // send request to S3 to create folder
        s3Client.putObject(putObjectRequest);

        // upload metadata to folder and set it to public
        s3Client.putObject(new PutObjectRequest(s3Bucket, "Posts/" + dalnId + "/" + metadata.getName(), metadata)
                .withCannedAcl(CannedAccessControlList.PublicRead));

    }

    private void deleteFolder() throws IOException {
        File folder = new File("downloads/" + dalnId);
        FileUtils.deleteDirectory(folder);

    }

    private String checkFiletype(String fileName) {
        try {
            switch (fileName.substring(fileName.lastIndexOf('.')).toLowerCase()) {
                case ".doc":
                case ".docx":
                case ".rtf":
                case ".txt":
                case ".pdf":
                case ".odt":
                    return "Text";
                case ".jpg":
                case ".jpeg":
                case ".gif":
                case ".png":
                case ".tiff":
                    return "Image";
                case ".mp3":
                case ".wav":
                case ".m4a":
                    return "Audio";
                case ".mp4":
                case ".mov":
                case ".avi":
                case ".wmv":
                case ".m4v":
                    return "Audio/Video";
                case ".htm":
                case ".html":
                    return "Web";
            }
        }catch(StringIndexOutOfBoundsException e)
        {
            if(verboseOutput) log.error(message.NoFileType(fileName)); else log.error(message.FileUploadPostErrorLog(dalnId));
            //System.exit(1);

        }
        return fileName.substring(fileName.lastIndexOf('.')+1).toUpperCase() + " File";
    }
}