
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

        PostImporter videoImporter = new PostImporter();
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



