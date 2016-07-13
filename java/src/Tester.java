
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;


import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

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


        /**Connect to DynamoDB with credentials and initialize wrapper**/
        /*AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(new ProfileCredentialsProvider("daln"));
        DynamoDBMapper mapper = new DynamoDBMapper(dynamoDBClient);

        Post post = new Post();
        post.setTitle("some title");
        post.setAuthor("some author");
        post.setDate("some date");
        post.setDescription("some description");
        post.setDalnId("some daln id");

        HashMap<String,String> asset1 = new HashMap<>();
        asset1.put("File Type", "asset1 type");
        asset1.put("File Location", "asset1 location");

        HashMap<String,String> asset2 = new HashMap<>();
        asset2.put("File Type", "asset2 type");
        asset2.put("File Location", "asset2 location");

        List<HashMap<String,String>> assetList = new ArrayList<>();
        assetList.add(asset1);
        assetList.add(asset2);

        post.setAssetList(assetList);
        //Enter it into the DB
        mapper.save(post);*/



        PostImporter videoImporter = new PostImporter();
        FileUploader fileUploader = new FileUploader();
       // DynamoDBClient client = new DynamoDBClient();

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
                    fileUploader.upload(postID);
                    break;
                case 2:
                    videoImporter.importPost(postID);
                    break;
                case 3:
                    fileUploader.upload(postID);
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



