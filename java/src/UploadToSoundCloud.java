import de.voidplus.soundcloud.SoundCloud;
import de.voidplus.soundcloud.Track;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Shakib on 7/18/2016.
 *
 * This class handles the task of uploading an audio to SoundCloud and retrieving its download location.
 * The constructor extracts all needed values from the HashMap and places them into variables. The values
 * will be used as inputs for the upload.
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
        originalLink = postDetails.get("identifierUri").toString();
        title = postDetails.get("title").toString();
        date = postDetails.get("dateCreated").toString();
        fileName = postDetails.get("Current File").toString();
        assetID = postDetails.get("Current Asset ID").toString();

        fullDescription = "Original Post Link: " + originalLink
                + "\nFile Name: " + fileName
                + "\nOriginal Date Posted: " + date;

        uploadSound();
    }

    public void connectToSoundCloud() throws IOException {
        /**Connect to SoundCloud**/
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
