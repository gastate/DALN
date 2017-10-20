import com.soundcloud.api.CloudAPI;
import de.voidplus.soundcloud.Comment;
import de.voidplus.soundcloud.SoundCloud;
import de.voidplus.soundcloud.Track;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
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
    private static Logger log = Logger.getLogger(UploadToSoundCloud.class.getName());
    private HashMap<String,Object> postDetails;
    private String dalnId, originalLink, originalPostTitle, description, author, date, fileName, assetID, fullDescription, fullTitle;
    private SoundCloud soundcloud;
    private Track track;

    public UploadToSoundCloud(HashMap<String, Object> postDetails) throws IOException {
        //Connect to SoundCloud
        boolean isSoundCloudConnected;
        do {
            isSoundCloudConnected = connectToSoundCloud();
            if(!isSoundCloudConnected)
                System.out.println("SoundCloud connection failed. Retrying..." );
            else
                System.out.println("SoundCloud connection successful.");
        }
        while(!isSoundCloudConnected);

        this.postDetails = postDetails;
        dalnId = postDetails.get("DalnId").toString();
        originalLink = postDetails.get("identifierUri").toString();
        originalPostTitle = postDetails.get("title").toString();
        fileName = postDetails.get("Current File").toString();
        assetID = postDetails.get("Current Asset ID").toString();

        String fileNameNoExt = fileName.substring(0, fileName.lastIndexOf('.'));
        fullTitle = originalPostTitle + " - " + fileNameNoExt;
        fullDescription = "Original Post Link: " + originalLink
                + "\nFile Name: " + fileName;

        uploadSound();
    }

    public UploadToSoundCloud(){}
    public boolean connectToSoundCloud() throws IOException {
        /**Connect to SoundCloud**/
        GetPropertyValues properties = new GetPropertyValues();
        HashMap<String, String> credentials = properties.getSoundCloudClientInfo();

        soundcloud = new SoundCloud(
                credentials.get("SoundCloudClientID"),
                credentials.get("SoundCloudClientSecret"));
        soundcloud.login(
                credentials.get("SoundCloudUser"),
                credentials.get("SoundCloudPassword"));


        try {
            if (soundcloud.getMe().toString() == null)
                return false;
        }
        catch(NullPointerException e)
        {
            return false;
        }
        return true;
    }

    public void uploadSound()
    {
        File currentDirectory = null;
        try {
            currentDirectory = new File(new File(".").getCanonicalPath());
            String location = currentDirectory + "/downloads/"+dalnId+"/"+fileName;
            Track newTrack = new Track(fullTitle, location);
            newTrack.setTagList(assetID);
            track = soundcloud.postTrack(newTrack);
            //track.setDescription(fullDescription);
        } catch (IOException | NullPointerException e) {
            log.error("Problem uploading to SoundCloud.");
            e.printStackTrace();
        }
    }

    public String[] getSoundLocation()
    {
        String[] soundLocations = new String[2];
        soundLocations[0] = track.getPermalinkUrl();
        soundLocations[1] = track.getUri();
        return soundLocations;
    }
}
