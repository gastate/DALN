import java.util.Scanner;

/**
 * Created by Shakib on 6/25/2016.
 */
public class Tester {

    public static void main(String[] args)
    {
        System.out.println("Hello world");
        PostImporter videoImporter = new PostImporter();
        FileUploader fileUploader = new FileUploader();

        Scanner scan = new Scanner(System.in);
        System.out.println("Enter post ID to download: ");
        String postID1 = scan.next();
        videoImporter.importPost(postID1);

        System.out.println("Enter post ID to upload files: ");
        String postID2 = scan.next();
        fileUploader.upload(postID2);


    }
}
