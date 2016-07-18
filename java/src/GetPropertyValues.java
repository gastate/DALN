import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

/**
 * Created by Shakib on 7/18/2016.
 */
public class GetPropertyValues
{
    String propFileName = "config.properties";
    InputStream inputStream;

    public String getSproutVideoApiKey() throws IOException {

        String sproutVideoApiKey = "";
        try {
            Properties prop = new Properties();
            inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
            prop.load(inputStream);
            sproutVideoApiKey = prop.getProperty("SproutVideoApiKey");

        } catch (FileNotFoundException e)
        {
            System.out.println("Property file '" + propFileName + "' not found in the classpath.");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            inputStream.close();
        }
        return sproutVideoApiKey;
    }

    public HashMap<String,String> getSoundCloudClientInfo() throws IOException {
        HashMap<String,String> soundCloudClientInfo = null;
        try {
            Properties prop = new Properties();
            inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
            prop.load(inputStream);
            soundCloudClientInfo.put("SoundCloudClientID", prop.getProperty("SoundCloudClientID"));
            soundCloudClientInfo.put("SoundCloudClientSecret", prop.getProperty("SoundCloudClientSecret"));
            soundCloudClientInfo.put("SoundCloudUser", prop.getProperty("SoundCloudUser"));
            soundCloudClientInfo.put("SoundCloudPassword", prop.getProperty("SoundCloudPassword"));

        } catch (FileNotFoundException e)
        {
            System.out.println("Property file '" + propFileName + "' not found in the classpath.");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            inputStream.close();
        }
        return soundCloudClientInfo;
    }
}
