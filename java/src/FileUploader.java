import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.*;
import com.sun.xml.internal.ws.spi.db.PropertyAccessor;
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
import org.joda.time.DateTime;
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
    public FileUploader() {

    }

    public void upload(String postID) {
        System.out.println("You are uploading post #" + postID + ".");

        /**Extract metadata from text file**/
        File metadata = new File("downloads/" + postID + "/Post #" + postID + " Data.txt");
        Scanner readMetadata = null;
        try {
            readMetadata = new Scanner(metadata); //Scanner object to read file
        } catch (FileNotFoundException e) {
            System.out.println("This post hasn't been downloaded yet or does not exist.");
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
        int numOfFiles = Integer.parseInt(files.substring(files.indexOf(':') + 1).trim());
        ArrayList<String> fileNames = new ArrayList<>();
        while (readMetadata.hasNextLine())
            fileNames.add(readMetadata.nextLine().trim());
        ArrayList<String> fileTypes = new ArrayList<>();
        for (String fileName : fileNames) {
            fileTypes.add(checkFiletype(fileName));
        }
        ArrayList<String> fileLocations = new ArrayList<>();


        /**Connect to SproutVideo**/
        System.out.println("Connecting to SproutVideo...");
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost uploadFile = new HttpPost("https://api.sproutvideo.com/v1/videos");
        HttpGet getFile = new HttpGet("https://api.sproutvideo.com/v1/videos?order_by=title");
        uploadFile.addHeader("SproutVideo-Api-Key", System.getenv().get("SproutApiKey"));
        getFile.addHeader("SproutVideo-Api-Key", System.getenv().get("SproutApiKey"));

        /**Connect to S3**/
        System.out.println("Connecting to S3...");
        //access key and secret access key stored in credentials file on local machine
        //https://blogs.aws.amazon.com/security/post/Tx3D6U6WSFGOK2H/A-New-and-Standardized-Way-to-Manage-Credentials-in-the-AWS-SDKs
        AmazonS3 s3Client = new AmazonS3Client(new ProfileCredentialsProvider("daln"));

        /**Upload post folder and metadata to S3**/
        String bucketName = "daln";
        String folderName = postID;

        System.out.println("Creating post folder in S3");

        //data for folder
        ObjectMetadata folderMetadata = new ObjectMetadata();
        folderMetadata.setContentLength(0);
        InputStream emptyContent = new ByteArrayInputStream(new byte[0]);

        //PutObjectRequest used for creating an object to be uploaded
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName,
                "Posts/" + folderName + "/", emptyContent, folderMetadata);
        // send request to S3 to create folder
        s3Client.putObject(putObjectRequest);

        System.out.println("Uploading post metadata");

        // upload metadata to folder and set it to public
        s3Client.putObject(new PutObjectRequest(bucketName, "Posts/" + postID + "/" + metadata.getName(), metadata)
                .withCannedAcl(CannedAccessControlList.PublicRead));

        DynamoDBClient client = new DynamoDBClient();

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
        //get asset locations
        assetDetails.put("DalnId", postID);
        ArrayList<String> allAssetUUIDs = client.insertAsset(assetDetails);

        int i = 0;
        /**Iterate through each file contained in the post, then upload to the service based on its extension**/
        for (String fileName : fileNames) {

            if (fileTypes.get(i).equals("Audio/Video")) {

                //upload to SproutVideo

                String fileNameNoExt = fileName.substring(0, fileName.lastIndexOf('.'));

                //These are used for inputs of the SproutVideo upload
                String fullTitle = allAssetUUIDs.get(i);
                System.out.println("Uploading " + fileName + " as " + fullTitle + " to SproutVideo");

                // String fullTitle = title + " - " + fileNameNoExt;

                String fullDescription = "Original Post Link: " + link
                        + "\nFile Name: " + fileName
                        + "\nDescription: " + description
                        + "\nAuthor: " + author
                        + "\nOriginal Date Posted: " + date;

                //SproutVideo API uploads accepts Multipart or Formdata as its format
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                //Inputs for the video submission
                builder.addTextBody("title", fullTitle, ContentType.TEXT_PLAIN);
                builder.addTextBody("description", fullDescription, ContentType.TEXT_PLAIN);
                builder.addTextBody("privacy", 2 + "", ContentType.TEXT_PLAIN);
                builder.addBinaryBody("source_video", new File("downloads/" + postID + "/" + fileName), ContentType.APPLICATION_OCTET_STREAM, fileName);
                HttpEntity multipart = builder.build();
                uploadFile.setEntity(multipart);

                CloseableHttpResponse response = null;
                try {
                    httpClient.execute(uploadFile);
                    response = httpClient.execute(getFile);

                } catch (IOException e) {
                    System.out.println(fileName + " could not be uploaded to SproutVideo.");
                }
                //HttpEntity responseEntity = response.getEntity();

                fileLocations.add(getSpoutVideoLocation(response, fullTitle));
                System.out.println(fileName + " uploaded to SproutVideo as " + fullTitle);

            } else {
                //upload all other files to S3
                try {
                    System.out.println("Uploading " + fileName + "...");
                    //upload file from working directory
                    File file = new File("downloads/" + postID + "/" + fileName);
                    //store file in specified s3 folder
                    s3Client.putObject(new PutObjectRequest(bucketName, "Posts/" + postID + "/" + fileName, file)
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

                fileLocations.add("some file location");
                System.out.println(fileName + " uploaded to S3.");
            }
            i++;
        }

        client.setAssetLocations(fileLocations);
        client.updatePostsAndAssets();
    }

    public String getSpoutVideoLocation(HttpResponse response, String uploadedVideoTitle) {
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
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return videoLocation;
    }

    public String checkFiletype(String fileName)
    {
        switch(fileName.substring(fileName.lastIndexOf('.')))
        {
            case ".doc": case ".docx":case ".rtf":case ".txt":case ".pdf":
                return "Text";
            case ".jpg": case ".jpeg":case ".gif":case ".png":case ".tiff":
                return "Image";
            case ".mp3":
                return "Audio";
            case ".mp4":case ".mov":case ".wav":case ".avi":
                return "Audio/Video";
            case ".htm":case ".html":
                return "Web";
        }
        return fileName.substring(fileName.lastIndexOf('.')+1).toUpperCase() + " File";
    }
}