import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.*;

/**
 * Created by Shakib on 6/28/2016.
 *
 * The purpose of this class is to upload files that were downloaded to the working directory using the PostImporter.java
 * class. The upload function also takes the post ID as input, but instead searches for this post in the downloads folder
 * of the project directory (which is the default location of post downloads). The method then determines what kind of
 * files are contained within each post, and then chooses which service to upload them to. Video files are uploaded to
 * SproutVideo, while the rest are uploaded to S3.
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

    public FileUploader()
    {
        /**Connect to SproutVideo**/
        System.out.println("Connecting to SproutVideo...");
        httpClient = HttpClients.createDefault();
        uploadFile = new HttpPost("https://api.sproutvideo.com/v1/videos");
        getFile = new HttpGet("https://api.sproutvideo.com/v1/videos?order_by=title");
        uploadFile.addHeader("SproutVideo-Api-Key", System.getenv().get("SproutApiKey"));
        getFile.addHeader("SproutVideo-Api-Key", System.getenv().get("SproutApiKey"));

        /**Connect to Clyp**/
        System.out.println("Connecting to Clyp...");
        uploadSound = new HttpPost("https://upload.clyp.it/upload");

        /**Connect to S3**/
        System.out.println("Connecting to S3...");
        //access key and secret access key stored in credentials file on local machine
        //https://blogs.aws.amazon.com/security/post/Tx3D6U6WSFGOK2H/A-New-and-Standardized-Way-to-Manage-Credentials-in-the-AWS-SDKs
        s3Client = new AmazonS3Client(new ProfileCredentialsProvider("daln"));

        /**Connect to DynamoDB**/
        client = new DynamoDBClient();
    }

    public void upload(String postID) {
        if(client.checkIfPostAlreadyExistsInDB(postID))
        {
            System.out.println("This post ID already exists in the database.");
            System.exit(0);
        }

        System.out.println("You are uploading post #" + postID + ".");

        /**Extract metadata from text file**/
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


        //HERE I SOMEHOW GET THE INFO FOR THE HIDDEN METADATA FOR EACH

        ArrayList<String> fileUUIDs = new ArrayList<>();
        ArrayList<String> fileNames = new ArrayList<>();
        ArrayList<String> fileTypes = new ArrayList<>();
        ArrayList<String> fileLocations = new ArrayList<>();
        String line;
        while (readMetadata.hasNextLine())
            if(!(line = readMetadata.nextLine()).equals(""))
                fileNames.add(line.trim());
            else
                break;
        for (String fileName : fileNames)
            fileTypes.add(checkFiletype(fileName));

        readMetadata.close();
        //call method that will upload the folder and post metadata to S3
        System.out.println("Creating post folder in S3 and uploading metadata.");
        uploadPostFolder(postID);

        int i = 0;
        /**Iterate through each file contained in the post, then upload to the service based on its extension**/
        for (String fileName : fileNames)
        {
            String newTitle = UUID.randomUUID().toString();
            fileUUIDs.add(newTitle);
            //String newTitle = postID + "_" + fileName;
            String fullDescription = "Original Post Link: " + link
                    + "\nFile Name: " + fileName
                    + "\nDescription: " + description
                    + "\nAuthor: " + author
                    + "\nOriginal Date Posted: " + date;

            if (fileTypes.get(i).equals("Audio/Video")) {
                //upload to SproutVideo
                System.out.print("Uploading the video file " + fileName + " as " + newTitle + " to SproutVideo...");

                //SproutVideo API uploads accept Multipart or Formdata as its format
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                //Inputs for the video submission
                builder.addTextBody("title", newTitle, ContentType.TEXT_PLAIN);
                builder.addTextBody("description", fullDescription, ContentType.TEXT_PLAIN);
                builder.addTextBody("privacy", 2 + "", ContentType.TEXT_PLAIN);
                builder.addBinaryBody("source_video", new File("downloads/" + postID + "/" + fileName), ContentType.APPLICATION_OCTET_STREAM, fileName);
                HttpEntity multipart = builder.build();
                uploadFile.setEntity(multipart);

                CloseableHttpResponse postResponse = null;
                CloseableHttpResponse getResponse = null;
                try {
                    postResponse = httpClient.execute(uploadFile);
                    getResponse = httpClient.execute(getFile);
                } catch (IOException e) {
                    System.out.println("\n"+fileName + " could not be uploaded to SproutVideo.");
                }

                fileLocations.add(getSpoutVideoLocation(getResponse, newTitle));
                System.out.println("Done.");

            }
            else if(fileTypes.get(i).equals("Audio"))
            {
                System.out.print("Uploading the audio file " + fileName + " to Clyp...");

                //Clyp API uploads accept Multipart
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                //Inputs for the audio submission
                //builder.addTextBody("description", description, ContentType.TEXT_PLAIN);
                builder.addBinaryBody("audioFile", new File("downloads/" + postID + "/" + fileName), ContentType.APPLICATION_OCTET_STREAM, fileName);
                HttpEntity multipart = builder.build();
                uploadSound.setEntity(multipart);

                CloseableHttpResponse getClypResponse = null;
                try {
                    getClypResponse = httpClient.execute(uploadSound);
                    String jsonResponse = EntityUtils.toString(getClypResponse.getEntity());
                    JSONParser parser = new JSONParser();
                    JSONObject jsonObject = (JSONObject) parser.parse(jsonResponse);
                    fileLocations.add(jsonObject.get("Mp3Url").toString());
                } catch (IOException e) {
                    System.out.println("\n"+fileName + " could not be uploaded to Clyp.");
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                System.out.println("Done.");
            }
            else {
                //upload all other files to S3
                try {
                    System.out.print("Uploading " + fileName + " to S3...");
                    //upload file from working directory
                    File file = new File("downloads/" + postID + "/" + fileName);
                    //store file in specified s3 folder
                    s3Client.putObject(new PutObjectRequest("daln", "Posts/" + postID + "/" + fileName, file)
                            .withCannedAcl(CannedAccessControlList.PublicRead));
                    fileLocations.add("https://s3-us-west-1.amazonaws.com/daln/Posts/" + postID + "/" + fileName);

                } catch (AmazonServiceException ase) {
                    System.out.println();
                    System.out.println("Caught an AmazonServiceException, which " +
                            "means your request made it " +
                            "to Amazon S3, but was rejected with an error response" +
                            " for some reason.");
                    System.out.println("Error Message:    " + ase.getMessage());
                    System.out.println("HTTP Status Code: " + ase.getStatusCode());
                    System.out.println("AWS Error Code:   " + ase.getErrorCode());
                    System.out.println("Error Type:       " + ase.getErrorType());
                    System.out.println("Request ID:       " + ase.getRequestId());
                } catch (AmazonClientException ace) {
                    System.out.println("Caught an AmazonClientException, which " +
                            "means the client encountered " +
                            "an internal error while trying to " +
                            "communicate with S3, " +
                            "such as not being able to access the network.");
                    System.out.println("Error Message: " + ace.getMessage());
                }
                System.out.println("Done");
            }
            i++;
        }

        //client.setAssetLocations(fileLocations);
        //client.updatePostsAndAssets();

        List<HashMap<String,String>> assetList = new ArrayList<>();
        for(int j = 0; j < numberOfAssets; j++)
        {
            HashMap<String,String> asset = new HashMap<>();
            asset.put("Asset ID", fileUUIDs.get(j));
            asset.put("Asset Location", fileLocations.get(j));
            asset.put("Asset Type", fileTypes.get(j));
            assetList.add(asset);
        }
        //Store the details of the post in a hashmap which will be passed on to the db client
        HashMap<String, Object> postDetails = new HashMap<>();
        postDetails.put("DalnId", postID);
        postDetails.put("Description", description);
        postDetails.put("Author", author);
        postDetails.put("Title", title);
        postDetails.put("UploadDate", date);
        postDetails.put("AssetList", assetList);

        //the insert post method returns the post ID.
        //It will be attributed to each of the assets that the post contains
        String postUUID = client.insertPost(postDetails);
        updatePostMetadata(postID, postUUID, fileNames, fileUUIDs, fileTypes, fileLocations);
        System.out.println("Post #" + postID + " successfully uploaded and added to database.");
    }

    /**Upload post folder and metadata to S3**/
    private void uploadPostFolder(String postID)
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
/**this method will update the post metadata text file in S3 with post and asset UUIDs, as well as asset locations. WIP**/
    private void updatePostMetadata(String postID, String postUUID, ArrayList<String> assetNames, ArrayList<String> assetUUIDs, ArrayList<String> assetTypes, ArrayList<String> assetLocations) {

    System.out.println("Updating metadata with database information.");
        try {
            List<String> lines = FileUtils.readLines(metadata, "utf-8");
            int dbInfoStart = lines.indexOf("");
            if(dbInfoStart != -1)
                for(int i = dbInfoStart; i < lines.size(); i++)
                    lines.remove(i);
            lines.add("\r\nPost UUID: " + postUUID);
            lines.add("Asset Info:");
            for(int i = 0; i < assetNames.size(); i++)
                lines.add("\t"+assetNames.get(i)+"\r\n\t"+assetUUIDs.get(i)+"\r\n\tType: "+assetTypes.get(i)+"\r\n\t"+assetLocations.get(i)+"\r\n\t");
            //metadata.setWritable(true);
            FileUtils.forceDelete(metadata);
            File newFile = new File("downloads/" + postID + "/Post #" + postID + " Data.txt");
            FileUtils.writeLines(newFile, lines);

        } catch (IOException e) {
            e.printStackTrace();
        }
        uploadPostFolder(postID);

    }

    private String getSpoutVideoLocation(HttpResponse response, String uploadedVideoTitle) {
        String videoLocation = "";
        try {
            String jsonString = EntityUtils.toString(response.getEntity());
            //System.out.println(jsonString);
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(jsonString);
            JSONArray jsonArray = (JSONArray) jsonObject.get("videos");
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject videoInfo = (JSONObject) jsonArray.get(i);
                String videoTitle = videoInfo.get("title").toString();
                if (videoTitle.equals(uploadedVideoTitle)) {
                    String videoID = videoInfo.get("id").toString();
                    videoLocation = "https://gsu-7zy7zle.vids.io/videos/" + videoID + "/"+uploadedVideoTitle;
                    return videoLocation;
                }
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
        return videoLocation;
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