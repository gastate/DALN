import de.voidplus.soundcloud.SoundCloud;
import de.voidplus.soundcloud.Track;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by Shakib on 7/18/2016.
 */
public class UploadToSoundCloud
{
    private HashMap<String,Object> postDetails;
    private String dalnId, originalLink, title, description, author, date, fileName, assetID, fullDescription;
    private SoundCloud soundcloud;
    private Track track;

    public UploadToSoundCloud(HashMap<String, Object> postDetails) throws IOException {
        connectToSoundCloud();

        this.postDetails = postDetails;
        dalnId = postDetails.get("DalnId").toString();
        originalLink = postDetails.get("OriginalLink").toString();
        title = postDetails.get("Title").toString();
        description = postDetails.get("Description").toString();
        author = postDetails.get("Author").toString();
        date = postDetails.get("UploadDate").toString();
        fileName = postDetails.get("Current File").toString();
        assetID = postDetails.get("Current Asset ID").toString();

        fullDescription = "Original Post Link: " + originalLink
                + "\nFile Name: " + fileName
                + "\nDescription: " + description
                + "\nAuthor: " + author
                + "\nOriginal Date Posted: " + date;

        uploadSound();
    }

    public void connectToSoundCloud() throws IOException {
        /**Connect to SoundCloud**/
        System.out.println("Connecting to SoundCloud...");
        GetPropertyValues properties = new GetPropertyValues();
        HashMap<String,String> credentials = properties.getSoundCloudClientInfo();
        soundcloud = new SoundCloud(
                credentials.get("SoundCloudClientID"),
                credentials.get("SoundCloudClientSecret"),
                credentials.get("SoundCloudUser"),
                credentials.get("SoundCloudPassword")
        );
    }

    public void uploadSound()
    {
        System.out.print("Uploading the audio file " + fileName + " as " + assetID + " to SoundCloud...");

        File currentDirectory = null;
        try {
            currentDirectory = new File(new File(".").getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        String location = currentDirectory + "\\downloads\\"+dalnId+"\\"+fileName;
        Track newTrack = new Track(assetID, location);
        newTrack.setDescription(fullDescription);
        track = soundcloud.postTrack(newTrack);
    }

    public String getSoundLocation()
    {
        return track.getPermalinkUrl();
    }
}
