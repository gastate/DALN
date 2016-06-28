import java.util.Scanner;

/**
 * Created by Shakib on 6/25/2016.
 */
public class Tester {

    public static void main(String[] args)
    {
        System.out.println("Hello world");
        VideoImporter videoImporter = new VideoImporter();

        Scanner scan = new Scanner(System.in);
        System.out.println("Enter post ID: ");
        String postID = scan.next();
        videoImporter.importVideo(postID);

    }
}
