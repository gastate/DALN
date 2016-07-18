package partialJythonImplementation;

import org.json.simple.JSONObject;

/**
 * Created by Shakib on 7/16/2016.
 */
public interface UploadToSoundCloudInterface
{
    public void initialize();
    public void upload(JSONObject postDetails);
    public String getSoundCloudURL();
}
