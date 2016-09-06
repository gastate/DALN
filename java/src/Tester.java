import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by Shakib on 6/25/2016.
 *
 * This is the starting point of the program. Parameters supplied are the post ID, a choice between download, upload, or both,
 * and an option for viewing a log output or a verbose output for debugging. It creates instances of the main.PostImporter and
 * main.FileUploader classes based on the option chosen.
 */
public class Tester {

    public static void main(String[] args) throws IOException, org.apache.http.ParseException, TransformerException, ParserConfigurationException {

        if(args.length > 0)
    {
        String postID = args[0];
        String programOption = "full";
        boolean verboseOutput = true;

        if(args.length >= 2) {
            switch(args[1])
            {
                case "download":
                    programOption = "download";
                    break;
                case "upload":
                    programOption = "upload";
                    break;
                case "log":
                    verboseOutput = false;
                    break;
                default:
                    programOption = "full";
                    verboseOutput = true;
            }

            if(args.length >= 3)
            {
                switch(args[2])
                {
                    case "log":
                        verboseOutput = false;
                        break;
                    default:
                        verboseOutput = true;
                }
            }
        }


        //System.out.println("postid: " + postID);
        //System.out.println("program option: " + programOption);
        //System.out.println("verbose output: " + verboseOutput);

        switch (programOption)
        {
            case "download":
                new PostImporter(postID, verboseOutput);
                break;
            case "upload":
                new FileUploader(postID, verboseOutput);
                break;
            case "full":
                new PostImporter(postID, verboseOutput);
                new FileUploader(postID, verboseOutput);
        }
    }
    else
    {
        System.out.println("You didn't enter any arguments. You must run the program as follows: "
        + "\njava tester postid [download|upload|full] [verbose|log]");
        System.exit(1);
    }
    }
}