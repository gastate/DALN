
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;

/**
 * Created by Shakib on 6/25/2016.
 */
public class Tester {

    public static void main(String[] args) throws IOException, ParseException {
        Scanner scan = new Scanner(System.in);
        System.out.println("OSU's DALN Post Importer and File Uploader" +
                "\nThis program takes a post ID as " +
                "input, downloads its contents to your working directory, then uploads" +
                " the contents of the file to S3 and SpoutVideo. ");

        PostImporter videoImporter = new PostImporter();
        FileUploader fileUploader = new FileUploader();
        DynamoDBClient client = new DynamoDBClient();

        System.out.println("Enter post ID to download: ");

        String postID1 = scan.next();
        videoImporter.importPost(postID1);

        //System.out.println("Enter post ID to upload files: ");
        //String postID2 = scan.next();
        fileUploader.upload(postID1);

        //client.insertPost(postID1);
    }
}



