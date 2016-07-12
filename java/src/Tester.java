
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.Scanner;


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



