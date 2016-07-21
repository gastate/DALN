import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

/**
 * Created by Shakib on 7/18/2016.
 *
 * This class retrieves the values from the config.properties file. That file contains the sensitive information
 * needed for the services used, such as the API keys for SproutVideo and SoundCloud, as well as login information
 * for SoundCloud.
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
        HashMap<String,String> soundCloudClientInfo = new HashMap<>();
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
