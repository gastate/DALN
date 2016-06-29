import com.sun.deploy.net.HttpResponse;
import com.sun.xml.internal.ws.wsdl.writer.document.Part;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.json.simple.JSONObject;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.*;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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
public class FileUploader
{
    public void upload(String postID)
    {
        /**Extract metadata from text file**/
        File metadata = new File("downloads/"+postID+"/Post #"+postID+" Data.txt");
        Scanner readMetadata = null;
        try {
            readMetadata = new Scanner(metadata); //Scanner object to read file
        } catch (FileNotFoundException e) {
            System.out.println("This post hasn't been downloaded yet or does not exist.");
            System.exit(1);
        }
        String link = readMetadata.nextLine();
        link = link.substring(link.indexOf(':')+1).trim(); //extracts everything after ": "
        String title = readMetadata.nextLine();
        title = title.substring(title.indexOf(':')+1).trim();
        String description = readMetadata.nextLine();
        description = description.substring(description.indexOf(':')+1).trim();
        String author = readMetadata.nextLine();
        author = author.substring(author.indexOf(':')+1).trim();
        String date = readMetadata.nextLine();
        date = date.substring(date.indexOf(':')+1).trim();
        String files = readMetadata.nextLine();
        int numOfFiles = Integer.parseInt(files.substring(files.indexOf(':')+1).trim());
        ArrayList<String> fileNames = new ArrayList<>();
        while(readMetadata.hasNextLine())
            fileNames.add(readMetadata.nextLine().trim());

        /**Iterate through each file contained in the post, then upload to the service based on its extension**/
        for(String fileName : fileNames) {

            if (fileName.contains(".mov") || fileName.contains(".mp4") || fileName.contains(".wav") || fileName.contains(".avi")) {
                //upload to SproutVideo
                System.out.println("You are uploading " + fileName + " to SproutVideo.");
                String fileNameNoExt = fileName.substring(0, fileName.lastIndexOf('.'));

                //These are used for inputs of the SproutVideo upload
                String fullTitle = title + " - " + fileNameNoExt;
                String fullDescription = "Original Post Link: " + link
                                      +"\nFile Name: " + fileName
                                      +"\nDescription: " + description
                                      +"\nAuthor: " + author
                                      +"\nOriginal Date Posted: " + date;

                //Connecting to the service
                CloseableHttpClient httpClient = HttpClients.createDefault();
                HttpPost uploadFile = new HttpPost("https://api.sproutvideo.com/v1/videos");
                uploadFile.addHeader("SproutVideo-Api-Key", "26983756be70c3d641fc84d7746a2895");

                //SproutVideo API uploads only accepts Multipart or Formdata as its format, so I chose Multipart
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                //Inputs for the video submission
                builder.addTextBody("title", fullTitle, ContentType.TEXT_PLAIN);
                builder.addTextBody("description", fullDescription, ContentType.TEXT_PLAIN);
                builder.addBinaryBody("source_video", new File("downloads/" + postID + "/" + fileName), ContentType.APPLICATION_OCTET_STREAM, fileName);
                HttpEntity multipart = builder.build();
                uploadFile.setEntity(multipart);

                CloseableHttpResponse response = null;
                try {
                    response = httpClient.execute(uploadFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                HttpEntity responseEntity = response.getEntity();
                //Checks the response, will update later for more meaningful messages
                System.out.println(responseEntity);

            } else {
                //upload all other files to S3
                System.out.println("You are uploading " + fileName + " to S3.");
            }
        }
    }


}
