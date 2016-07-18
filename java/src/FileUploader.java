import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.*;
import java.util.*;

/**
 * Created by Shakib on 6/28/2016.
 *
 * The purpose of this class is to upload files that were downloaded to the working directory using the PostImporter.java
 * class. The upload function takes the post ID as input and searches for this post in the downloads folder
 * of the project directory (which is the default location of post downloads). The method then determines what kind of
 * files are contained within each post, and then chooses which service to upload them to. Video files are uploaded to
 * SproutVideo, audio files are uploaded to Clyp, and the rest are uploaded to S3.
 */
public class FileUploader {

    private AmazonS3 s3Client;
    private CloseableHttpClient httpClient;
    private HttpPost uploadFile;
    private HttpPost uploadSound;
    private HttpGet getFile;
    private HttpGet getSound;
    private DynamoDBClient client;
    private File metadata;
    private HashMap<String, Object> postDetails;
    private String postID;

    public FileUploader(String postID) throws IOException {
        httpClient = HttpClients.createDefault();

        /**Connect to S3**/
        //access key and secret access key stored in credentials file on local machine
        //https://blogs.aws.amazon.com/security/post/Tx3D6U6WSFGOK2H/A-New-and-Standardized-Way-to-Manage-Credentials-in-the-AWS-SDKs
        s3Client = new AmazonS3Client(new ProfileCredentialsProvider("daln"));

        /**Connect to DynamoDB**/
        client = new DynamoDBClient();

        this.postID = postID;
        postDetails = new HashMap<>();
        postDetails = getPostDetails();
        uploadPost();
    }

    public HashMap<String, Object> getPostDetails()
    {
        /**Extract fields from metadata so that we can use them as inputs for uploads and DB entry**/
        metadata = new File("downloads/" + postID + "/Post #" + postID + " Data.txt");
        Scanner readMetadata = null;
        try {
            readMetadata = new Scanner(metadata); //Scanner object to read file
        } catch (FileNotFoundException e) {
            System.out.println("This post hasn't been downloaded yet or this post does not exist.");
            System.exit(1);
        }
        String link = readMetadata.nextLine();
        link = link.substring(link.indexOf(':') + 1).trim(); //extracts everything after ": "
        String title = readMetadata.nextLine();
        title = title.substring(title.indexOf(':') + 1).trim();
        String description = readMetadata.nextLine();
        description = description.substring(description.indexOf(':') + 1).trim();
        String author = readMetadata.nextLine();
        author = author.substring(author.indexOf(':') + 1).trim();
        String date = readMetadata.nextLine();
        date = date.substring(date.indexOf(':') + 1).trim();
        String files = readMetadata.nextLine();
        int numberOfAssets =Integer.parseInt(files.substring(files.indexOf(':')+1).trim());

        ArrayList<String> fileNames = new ArrayList<>();
        String line;
        while (readMetadata.hasNextLine())
        {
            //if lines after this point are empty, that means either the file is done
            //OR the file includes DB data already, which should be ignored
            line = readMetadata.nextLine();
            if (line.equals("**DATABASE INFORMATION**"))
                break;
            else
                fileNames.add(line.trim());
        }
        readMetadata.close();
        //HERE I SOMEHOW GET THE INFO FOR THE HIDDEN METADATA FOR EACH
        /**File types will determine which service to use for upload**/
        ArrayList<String> fileTypes = new ArrayList<>();
        for (String fileName : fileNames)
            fileTypes.add(checkFiletype(fileName));


        //Store the details of the post in a hashmap which will be passed on to the db client
        postDetails.put("DalnId", postID);
        postDetails.put("Description", description);
        postDetails.put("Author", author);
        postDetails.put("Title", title);
        postDetails.put("UploadDate", date);
        postDetails.put("OriginalLink", link);
        postDetails.put("NumberOfAssets", numberOfAssets);
        postDetails.put("FileNames", fileNames);
        postDetails.put("FileTypes", fileTypes);

        return postDetails;
    }

    public void uploadPost() throws IOException {
        if(client.checkIfPostAlreadyExistsInDB(postID))
        {
            System.out.println("This post ID already exists in the database.");
           // System.exit(0);
        }
        System.out.println("You are uploading post #" + postID + ".");

       /**The first step is to create a folder in S3 specific to this post and include its metadata**/
        System.out.println("Creating post folder in S3 and uploading metadata.");
        uploadPostFolder();

        /**Every file that the post contains will now be uploaded to a service based on its file type. Each
         * file will be issued a random UUID and each file will return a location after its upload. These two lists
         * will be added to the postDetails HashMap once all the files are uploaded, as these two details are not
         * defined currently.**/
        ArrayList<String> fileUUIDs = new ArrayList<>();
        ArrayList<String> fileLocations = new ArrayList<>();

        ArrayList<String> fileNames = (ArrayList<String>)postDetails.get("FileNames");
        ArrayList<String> fileTypes = (ArrayList<String>)postDetails.get("FileTypes");
        int numberOfAssets = (Integer)postDetails.get("NumberOfAssets");
        for (int i = 0; i < numberOfAssets; i++)
        {
            String currentFileName = fileNames.get(i);
            String assetID = UUID.randomUUID().toString();
            fileUUIDs.add(assetID);

            postDetails.put("Current File", currentFileName);
            postDetails.put("Current Asset ID", assetID);

            if (fileTypes.get(i).equals("Audio/Video"))
            {
                UploadToSproutVideo SVUploader = new UploadToSproutVideo(postDetails);
                fileLocations.add(SVUploader.getSpoutVideoLocation());
                System.out.println("Done.");
            }
            else if(fileTypes.get(i).equals("Audio"))
            {
                UploadToSoundCloud SCUploader = new UploadToSoundCloud(postDetails);
                fileLocations.add(SCUploader.getSoundLocation());
                System.out.println("Done.");
            }
            else {
                //upload all other files to S3
                UploadToS3 S3Uploader = new UploadToS3(postDetails);
                fileLocations.add(S3Uploader.getS3FileLocation());
                System.out.println("Done.");
            }
        }

        /**Gather the generated information about the posts assets to be placed into the DB**/
        //All of the other post details were gathered from the metadata earlier. The last field we need
        //for the DB entry is the list of assets with details for each asset.
        List<HashMap<String,String>> assetList = new ArrayList<>();
        for(int j = 0; j < numberOfAssets; j++)
        {
            HashMap<String,String> asset = new HashMap<>();
            asset.put("Asset ID", fileUUIDs.get(j));
            asset.put("Asset Location", fileLocations.get(j));
            asset.put("Asset Type", fileTypes.get(j));
            assetList.add(asset);
        }
        postDetails.put("AssetList", assetList); //this list includes generated asset IDs, locations, and filetypes

        //the insert post method returns the randomly generated post UUID.
        String postUUID = client.insertPost(postDetails);

        //The metadata needs to be updated with information after the uploading and DB insertion is completed
        updatePostMetadata(postID, postUUID, fileNames, fileUUIDs, fileTypes, fileLocations);

        System.out.println("Post #" + postID + " successfully uploaded and added to database.");
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
    /**this method will update the post metadata text file in S3 with post and asset UUIDs, as well as asset locations.**/
    private void updatePostMetadata(String postID, String postUUID, ArrayList<String> assetNames, ArrayList<String> assetUUIDs, ArrayList<String> assetTypes, ArrayList<String> assetLocations) {

    System.out.println("Updating metadata with database information.");
        //The metadata file essentially will be recreated with added info
        try {
            List<String> lines = FileUtils.readLines(metadata, "utf-8");
            int indexToRemove = -1;
            for(int i = 0; i<lines.size(); i++)
            {
                if(lines.get(i).equals("**DATABASE INFORMATION**"))
                {
                    indexToRemove = i;
                    for(int j = indexToRemove; j <lines.size();)
                        lines.remove(j);
                    break;
                }
            }
            lines.add("**DATABASE INFORMATION**");
            lines.add("Post UUID: " + postUUID);
            lines.add("Asset Info:");
            for(int i = 0; i < assetNames.size(); i++)
                lines.add("\t"+assetNames.get(i)+"\r\n\t"+assetUUIDs.get(i)+"\r\n\tType: "+assetTypes.get(i)+"\r\n\t"+assetLocations.get(i)+"\r\n\t");
            FileUtils.forceDelete(metadata);
            File newFile = new File("downloads/" + postID + "/Post #" + postID + " Data.txt");
            FileUtils.writeLines(newFile, lines);

        } catch (IOException e) {
            e.printStackTrace();
        }
        uploadPostFolder();

    }



    private String checkFiletype(String fileName)
    {
        switch(fileName.substring(fileName.lastIndexOf('.')))
        {
            case ".doc": case ".docx":case ".rtf":case ".txt":case ".pdf":
                return "Text";
            case ".jpg": case ".jpeg":case ".gif":case ".png":case ".tiff":
                return "Image";
            case ".mp3":case ".wav":case ".m4a":
                return "Audio";
            case ".mp4":case ".mov":case ".avi":case ".wmv":case ".m4v":
                return "Audio/Video";
            case ".htm":case ".html":
                return "Web";
        }
        return fileName.substring(fileName.lastIndexOf('.')+1).toUpperCase() + " File";
    }
}