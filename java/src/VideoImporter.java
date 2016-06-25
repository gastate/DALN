import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Created by Shakib on 6/25/2016.
 *
 * The purpose of this class is to retrieve videos stored on OSU's DALN website and upload them to SproutVideo.
 * Each record in the DALN has a unique identifier, so the method will take this ID as input, find the video(s)
 * posted in that record (if any), store it temporarily in a server, then upload to SproutVideo using its API.
 */
public class VideoImporter
{
    public void importVideo(String ID)
    {
        //Download video to local machine
        URL website = null;
        Path target = Paths.get("C:\\Users\\Shakib\\Desktop\\video.MOV");

        try {
            website = new URL("http://daln.osu.edu/bitstream/handle/2374.DALN/1543/Video%203.MP4");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }


        try (InputStream in = website.openStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
