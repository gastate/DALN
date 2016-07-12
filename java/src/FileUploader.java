import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

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
        readMetadata.nextLine(); //skips "Files" line

        //HERE I SOMEHOW GET THE INFO FOR THE HIDDEN METADATA FOR EACH

        ArrayList<String> fileNames = new ArrayList<>();
        ArrayList<String> fileTypes = new ArrayList<>();
        ArrayList<String> fileLocations = new ArrayList<>();
        while (readMetadata.hasNextLine())
            fileNames.add(readMetadata.nextLine().trim());
        for (String fileName : fileNames)
            fileTypes.add(checkFiletype(fileName));

        //call method that will upload the folder and post metadata to S3
        uploadPostFolder(postID);

        //Store the details of the post in a hashmap which will be passed on to the db client
        HashMap<String, Object> postDetails = new HashMap<>();
        postDetails.put("DalnId", postID);
        postDetails.put("Description", description);
        postDetails.put("Author", author);
        postDetails.put("Title", title);
        postDetails.put("UploadDate", date);

        //the insert post method returns the post ID.
        //It will be attributed to each of the assets that the post contains
        String postUUID = client.insertPost(postDetails);

        //Create a hashmap of the details of each file, which will be passed on to insert into the asset table in the db
        HashMap<String, Object> assetDetails = new HashMap<>();
        assetDetails.put("PostId", postUUID);
        assetDetails.put("AssetList", fileNames);
        assetDetails.put("FileType", fileTypes);
        assetDetails.put("DalnId", postID);

        //The insert assets method returns a list of the generated UUIDs for every asset. This will be used as titles
        //for video uploads
        ArrayList<String> allAssetUUIDs = client.insertAssets(assetDetails);

        int i = 0;
        /**Iterate through each file contained in the post, then upload to the service based on its extension**/
        for (String fileName : fileNames)
        {
            String newTitle = allAssetUUIDs.get(i);
            String fullDescription = "Original Post Link: " + link
                    + "\nFile Name: " + fileName
                    + "\nDescription: " + description
                    + "\nAuthor: " + author
                    + "\nOriginal Date Posted: " + date;

            if (fileTypes.get(i).equals("Audio/Video")) {
                //upload to SproutVideo
                System.out.println("Uploading the video file " + fileName + " as " + newTitle + " to SproutVideo");

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
                    System.out.println(fileName + " could not be uploaded to SproutVideo.");
                }

                fileLocations.add(getSpoutVideoLocation(getResponse, newTitle));
                System.out.println(fileName + " uploaded to SproutVideo as " + newTitle);

            }
            else if(fileTypes.get(i).equals("Audio"))
            {
                System.out.println("Uploading the audio file " + fileName + " to Clyp.");

                //Clyp API uploads accept Multipart
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                //Inputs for the audio submission
                builder.addTextBody("description", description, ContentType.TEXT_PLAIN);
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
                    System.out.println(fileName + " could not be uploaded to Clyp.");
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            else {
                //upload all other files to S3
                try {
                    System.out.println("Uploading " + fileName + " to S3.");
                    //upload file from working directory
                    File file = new File("downloads/" + postID + "/" + fileName);
                    //store file in specified s3 folder
                    s3Client.putObject(new PutObjectRequest("daln", "Posts/" + postID + "/" + fileName, file)
                            .withCannedAcl(CannedAccessControlList.PublicRead));
                    fileLocations.add("https://s3-us-west-1.amazonaws.com/daln/Posts/" + postID + "/" + fileName);

                } catch (AmazonServiceException ase) {
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
                System.out.println(fileName + " uploaded to S3.");
            }
            i++;
        }

        client.setAssetLocations(fileLocations);
        client.updatePostsAndAssets();
        System.out.println("Post #" + postID + " successfully uploaded and added to database.");
        //updatePostMetadata(postID, postUUID, fileNames, allAssetUUIDs, fileLocations);
    }

    /**Upload post folder and metadata to S3**/
    private void uploadPostFolder(String postID)
    {
        System.out.println("Creating post folder in S3");

        //data for folder
        ObjectMetadata folderMetadata = new ObjectMetadata();
        folderMetadata.setContentLength(0);
        InputStream emptyContent = new ByteArrayInputStream(new byte[0]);

        //PutObjectRequest used for creating an object to be uploaded
        PutObjectRequest putObjectRequest = new PutObjectRequest("daln",
                "Posts/" + postID + "/", emptyContent, folderMetadata);
        // send request to S3 to create folder
        s3Client.putObject(putObjectRequest);

        System.out.println("Uploading post metadata");

        // upload metadata to folder and set it to public
        s3Client.putObject(new PutObjectRequest("daln", "Posts/" + postID + "/" + metadata.getName(), metadata)
                .withCannedAcl(CannedAccessControlList.PublicRead));

    }
/**this method will update the post metadata text file in S3 with post and asset UUIDs, as well as asset locations. WIP**
    private void updatePostMetadata(String postID, String postUUID, ArrayList<String> assetNames, ArrayList<String> assetUUIDs, ArrayList<String> assetLocations) {
        String oldFileName = metadata.getName();
        String tmpFileName = "tmp_" + metadata.getName();

        BufferedReader br = null;
        BufferedWriter bw = null;
        try {
            br = new BufferedReader(new FileReader("downloads/" + postID + "/"+oldFileName));
            bw = new BufferedWriter(new FileWriter("downloads/" + postID + "/"+tmpFileName));
            String line;
            while ((line = br.readLine()) != null) {
                bw.write(line);
            }

            bw.write("Post UUID: " + postUUID + "\n");
            bw.write("Asset UUIDs:\n");
            int i = 0;
            for(String id : assetUUIDs)
            {
                bw.write("\t"+assetNames.get(i)+"\t"+id+"\t"+assetLocations.get(i)+"\n");
                i++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Once everything is complete, delete old file..
        File oldFile = new File(oldFileName);
        oldFile.delete();

        // And rename tmp file's name to old file name
        File newFile = new File(tmpFileName);
        newFile.renameTo(oldFile);
        uploadPostFolder(postID);

    }
*/
    private String getSpoutVideoLocation(HttpResponse response, String uploadedVideoTitle) {
        String videoLocation = "";
        try {
            String jsonString = EntityUtils.toString(response.getEntity());
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(jsonString);
            JSONArray jsonArray = (JSONArray) jsonObject.get("videos");
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject videoInfo = (JSONObject) jsonArray.get(i);
                String videoTitle = videoInfo.get("title").toString();
                if (videoTitle.equals(uploadedVideoTitle)) {
                    String videoID = videoInfo.get("id").toString();
                    videoLocation = "https://gsu.vids.io/videos/" + videoID + "/"+uploadedVideoTitle;
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
            case ".mp4":case ".mov":case ".avi":case ".wmv":
                return "Audio/Video";
            case ".htm":case ".html":
                return "Web";
        }
        return fileName.substring(fileName.lastIndexOf('.')+1).toUpperCase() + " File";
    }
}