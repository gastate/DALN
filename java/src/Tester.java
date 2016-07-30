import org.json.simple.parser.ParseException;
import java.io.*;

/**
 * Created by Shakib on 6/25/2016.
 *
 * This is the starting point of the program. It asks for the post ID as input and provides the user the options
 * to download the post only, upload the post only, and download then upload the post. It creates instances
 * of the PostImporter and FileUploader classes based on the option chosen.
 */
public class Tester {

    public static void main(String[] args) throws IOException, ParseException {

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


        System.out.println("postid: " + postID);
        System.out.println("program option: " + programOption);
        System.out.println("verbose output: " + verboseOutput);

        PostImporter postImporter = new PostImporter();
        switch (programOption)
        {
            case "download":
                postImporter.importPost(postID, verboseOutput);
                break;
            case "upload":
                new FileUploader(postID, verboseOutput);
                break;
            case "full":
                postImporter.importPost(postID, verboseOutput);
                new FileUploader(postID, verboseOutput);
        }
    }
    else
    {
        System.out.println("You didn't enter any arguments. You must run the program as follows: "
        + "\njava tester postid [download|upload|full] [verbose|log]");
        System.exit(1);
    }










        /*Scanner scan = new Scanner(System.in);
        System.out.println("OSU's DALN Post Importer and File Uploader" +
                "\nThis program takes a post ID as " +
                "input, downloads its contents to your working directory, then uploads" +
                " the files contained within the post to SoundCloud, SpoutVideo, and S3. ");


        PostImporter videoImporter = new PostImporter();
        System.out.println("Enter the post ID: ");
        String postID = scan.next();
        System.out.println("\nOptions:" +
                "\n1) Download post #" + postID + " from DALN, upload, and add to database." +
                "\n2) Download post #" + postID + " from DALN only." +
                "\n3) Upload post #" + postID + " and add to database only." +
                "\nEnter option number (-1 to quit): ");
        int opt = 0;
        try {
            opt = Integer.parseInt(scan.next());
            switch (opt) {
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
        } catch (NumberFormatException e) {
            System.out.println("You have to enter a number.");
            System.exit(1);
        }*/

    }
}