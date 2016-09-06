import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTimeZone;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
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

    private AmazonS3 s3Client;
    private DynamoDBClient client;
    private File metadata;
    private HashMap<String, Object> postDetails;
    private String postID;
    private StatusMessages message;
    private boolean verboseOutput;
    private String[] singleEntryFields, multiEntryFields;

    public FileUploader(String postID, boolean verboseOutput) throws IOException {
        this.verboseOutput = verboseOutput;
        message = new StatusMessages();
        if(!verboseOutput) message.FileUploadPostBeginLog(postID);
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



        /**Connect to DynamoDB**/
        client = new DynamoDBClient();

        PostImporter postImporter = new PostImporter();
        postImporter.initializeFields();
        singleEntryFields = postImporter.getSingleEntryFields();
        multiEntryFields = postImporter.getMultiEntryFields();

        this.postID = postID;
        postDetails = new HashMap<>();
        postDetails = getPostDetails();
        uploadPost();
    }



    /**Extract fields from the metadata generated from main.PostImporter so that we can use them as inputs for uploads
     * and DB entry. All the needed information is stored in a single HashMap.**/
    public HashMap<String, Object> getPostDetails()
    {
        String metadataPath = "downloads/" + postID + "/Post" + postID + ".xml";
        metadata = new File(metadataPath);
        Document doc = null;
        try {
            doc = Jsoup.parse(metadata, "UTF-8"); //Scanner object to read file
        } catch (IOException e) {
            if(verboseOutput) message.DALNPostDoesNotExist();
            System.exit(1);
        }

        //main.Post and file details are gathered from the xml metadata
        //Details will all be stored in a single hashmap
        Element root = doc.child(0);

        Element field;
        for(String fieldName : singleEntryFields)
            if ((field = root.select(fieldName).first()) != null)
                postDetails.put(fieldName, field.text());

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

        postDetails.put("DalnId", postID);
        postDetails.put("NumberOfAssets", numberOfAssets);
        postDetails.put("fileNames", fileNames);
        postDetails.put("fileTypes", fileTypes);
        postDetails.put("fileDescriptions", fileDescriptions);

        return postDetails;
    }

    public void uploadPost() throws IOException {
        if(client.checkIfIDAlreadyExistsInDB(postID))
        {
            if(verboseOutput) message.PostAlreadyExistsInDB(); else message.FileUploadPostErrorLog(postID);
            System.exit(0);
        }

        if(verboseOutput) message.BeginPostUpload(postID);

       /**The first step is to create a folder in S3 specific to this post and include its metadata**/
        if(verboseOutput) message.CreateS3Data();
        uploadPostFolder();

        /**Every file that the post contains will now be uploaded to a service based on its file type. Each
         * file will be issued a random UUID and each file will return a location after its upload. These two lists
         * will be added to the postDetails HashMap once all the files are uploaded, as these two details are not
         * defined currently.**/
        ArrayList<String> fileUUIDs = new ArrayList<>();
        ArrayList<String> fileLocations = new ArrayList<>();
        ArrayList<Boolean> fileUploadStatuses = new ArrayList<>();

        ArrayList<String> fileNames = (ArrayList<String>)postDetails.get("fileNames");
        ArrayList<String> fileTypes = (ArrayList<String>)postDetails.get("fileTypes");
        ArrayList<String> fileDescriptions = (ArrayList<String>)postDetails.get("fileDescriptions");
        int numberOfAssets = (Integer)postDetails.get("NumberOfAssets");
        for (int i = 0; i < numberOfAssets; i++)
        {
            String currentFileName = fileNames.get(i);
            String assetID;
            do
                assetID = UUID.randomUUID().toString();
            while(client.checkIfUUIDExists(assetID));

            fileUUIDs.add(assetID);
            if(!verboseOutput) message.FileUploadAssetBeginLog(assetID);

            postDetails.put("Current File", currentFileName);
            postDetails.put("Current Asset ID", assetID);

            if (fileTypes.get(i).equals("Audio/Video"))
            {
                if(verboseOutput) message.UploadingToSproutVideo(currentFileName, assetID);
                UploadToSproutVideo SVUploader = new UploadToSproutVideo(postDetails);
                fileLocations.add(SVUploader.getSpoutVideoLocation());
                if(verboseOutput)System.out.println("Done."); else message.FileUploadAssetCompleteLog(assetID);
            }
            else if(fileTypes.get(i).equals("Audio"))
            {
                if(verboseOutput) message.UploadingToSoundCloud(currentFileName, assetID);
                UploadToSoundCloud SCUploader = new UploadToSoundCloud(postDetails);
                fileLocations.add(SCUploader.getSoundLocation());
                if(verboseOutput)System.out.println("Done."); else message.FileUploadAssetCompleteLog(assetID);
            }
            else {
                //upload all other files to S3
                if(verboseOutput) message.UploadingToS3(currentFileName);
                UploadToS3 S3Uploader = new UploadToS3(postDetails);
                fileLocations.add(S3Uploader.getS3FileLocation());
                if(verboseOutput)System.out.println("Done."); else message.FileUploadAssetCompleteLog(assetID);
            }
        }

        /**Gather the generated information about the post's assets to be inserted into the DB**/
        //All of the other post details were gathered from the metadata earlier. The last field we need
        //for the DB entry is the asset list, which contains details for each asset.
        List<HashMap<String,String>> assetList = new ArrayList<>();
        for(int j = 0; j < numberOfAssets; j++)
        {
            HashMap<String,String> asset = new HashMap<>();
            asset.put("Asset ID", fileUUIDs.get(j));
            asset.put("Asset Location", fileLocations.get(j));
            asset.put("Asset Description", fileDescriptions.get(j));
            asset.put("Asset Type", fileTypes.get(j));
            assetList.add(asset);

            if(fileLocations.get(j).equals("") || fileLocations.get(j)==null)
                fileUploadStatuses.add(false);
            else
                fileUploadStatuses.add(true);
        }
        postDetails.put("assetList", assetList); //this list includes generated asset IDs, locations, and filetypes
        postDetails.put("fileUploadStatuses", fileUploadStatuses);

        //the insert post method returns the randomly generated post UUID.
        client.insertPost(postDetails);

        if(verboseOutput) message.FileUploadPostCompleteVerbose(postID); else message.FileUploadPostCompleteLog(postID);
    }

    /**Upload post folder and metadata to S3**/
    private void uploadPostFolder()
    {
        //data for folder
        ObjectMetadata folderMetadata = new ObjectMetadata();
        folderMetadata.setContentLength(0);
        InputStream emptyContent = new ByteArrayInputStream(new byte[0]);

        //PutObjectRequest used for creating an object to be uploaded
        PutObjectRequest putObjectRequest = new PutObjectRequest("daln",
                "Posts/" + postID + "/", emptyContent, folderMetadata);
        // send request to S3 to create folder
        s3Client.putObject(putObjectRequest);

        // upload metadata to folder and set it to public
        s3Client.putObject(new PutObjectRequest("daln", "Posts/" + postID + "/" + metadata.getName(), metadata)
                .withCannedAcl(CannedAccessControlList.PublicRead));

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
            if(verboseOutput)message.NoFileType(fileName);else message.FileUploadPostErrorLog(postID);
            System.exit(1);

        }
        return fileName.substring(fileName.lastIndexOf('.')+1).toUpperCase() + " File";
    }
}