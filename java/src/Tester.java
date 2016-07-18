
import com.soundcloud.api.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.security.cert.Certificate;
import java.util.*;

import de.voidplus.soundcloud.*;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

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

        HashMap<String, Object> postDetails = new HashMap<>();
        postDetails.put("Current File", "essay3 audio.m4a");
        postDetails.put("Current Asset ID", "some asset id");
        postDetails.put("DalnId", "5672");
        JSONObject jsonMap = new JSONObject(postDetails);

        /*ApiWrapper wrapper = new ApiWrapper("34f3a0291f39c81b9fe952a0a0508ed6", "ea8e7de9b471b03de72f0276c2b87993", null, null);
        wrapper.login("rahmed8@gsu.edu", "shakibsoundcloud");

        String location = "C:\\Users\\Shakib\\Documents\\Programming\\IdeaProjects\\DALN\\downloads\\sample.mp3";
        //HttpResponse resp = wrapper.get(Request.to("/me"));
        File file = new File(location);
        HttpResponse resp = wrapper.post(Request.to(Endpoints.TRACKS)
                        .add(Params.Track.TITLE,     file.getName())
                        .add(Params.Track.TAG_LIST, "demo upload")
                        .withFile(Params.Track.ASSET_DATA, file));
        try {
            System.out.println("\n" + Http.getJSON(resp).toString(4));
        } catch (JSONException e) {
            e.printStackTrace();
        }*/

        GetPropertyValues properties = new GetPropertyValues();
        //properties.getSproutVideoApiKey();
        System.out.println(properties.getSproutVideoApiKey());
        PostImporter videoImporter = new PostImporter();
        //FileUploader fileUploader = new FileUploader();

        System.out.println("Enter post ID: ");
        String postID = scan.next();
        System.out.println("\nOptions:" +
                "\n1) Download post #" + postID + " from DALN, upload, and add to database." +
                "\n2) Download post #" + postID + " from DALN only." +
                "\n3) Upload post #" + postID + " and add to database only." +
                "\nEnter option number (-1 to quit): ");
        int opt = 0;
        try
        {
            opt = Integer.parseInt(scan.next());
            switch(opt)
            {
                case 1:
                    videoImporter.importPost(postID);
                    new FileUploader(postID);
                    break;
                case 2:
                    videoImporter.importPost(postID);
                    break;
                case 3:
                    new FileUploader(postID);
                    break;
                case -1:
                    System.exit(1);
                default:
                    System.out.println("You didn't enter a valid option.");
                    System.exit(1);
            }
        }
        catch(NumberFormatException e)
        {
            System.out.println("You have to enter a number.");
            System.exit(1);
        }
    }
}



