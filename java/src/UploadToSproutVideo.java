import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Shakib on 7/16/2016.
 *
 * This class handles the task of uploading a video to SproutVideo and retrieving its download location.
 * The constructor extracts all needed values from the HashMap and places them into variables. The values
 * will be used as inputs for the upload.
 */
public class UploadToSproutVideo
{
    private HashMap<String,Object> postDetails;
    private String dalnId, originalLink, title, description, author, date, fileName, assetID, fullDescription;
    private CloseableHttpClient httpClient;
    private HttpPost uploadFile;

    public UploadToSproutVideo(HashMap<String, Object> postDetails) throws IOException {
        connectToSpoutVideo();

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

        uploadVideo();
    }

    public void connectToSpoutVideo() throws IOException {
        /**Connect to SproutVideo**/
        System.out.println("Connecting to SproutVideo...");
        GetPropertyValues properties = new GetPropertyValues();
        httpClient = HttpClients.createDefault();
        uploadFile = new HttpPost("https://api.sproutvideo.com/v1/videos");
        uploadFile.addHeader("SproutVideo-Api-Key", properties.getSproutVideoApiKey());
    }

    public void uploadVideo()
    {
        System.out.print("Uploading the video file " + fileName + " as " + assetID + " to SproutVideo...");
        //SproutVideo API uploads accept Multipart or Formdata as its format
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        //For the video submission
        builder.addTextBody("title", assetID, ContentType.TEXT_PLAIN);
        builder.addTextBody("description", fullDescription, ContentType.TEXT_PLAIN);
        builder.addTextBody("privacy", 2 + "", ContentType.TEXT_PLAIN);
        builder.addBinaryBody("source_video", new File("downloads/" + dalnId + "/" + fileName), ContentType.APPLICATION_OCTET_STREAM, fileName);
        HttpEntity multipart = builder.build();
        uploadFile.setEntity(multipart);

        CloseableHttpResponse postResponse = null;
        try {
            postResponse = httpClient.execute(uploadFile);
        } catch (IOException e) {
            System.out.println("\n"+fileName + " could not be uploaded to SproutVideo.");
        }

    }

    public String getSpoutVideoLocation() {
        //The HTTP Response must be parsed to retrieve the location of the uploaded video
        HttpGet getFile = new HttpGet("https://api.sproutvideo.com/v1/videos?order_by=title");
        getFile.addHeader("SproutVideo-Api-Key", System.getenv().get("SproutApiKey"));

        String videoLocation = "";
        String uploadedVideoTitle = postDetails.get("Current Asset ID").toString();
        CloseableHttpResponse getResponse = null;
        //The getResponse must be parsed as a JSON to retrieve the downloaded location
        try {
            getResponse = httpClient.execute(getFile);
            String jsonString = EntityUtils.toString(getResponse.getEntity());
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(jsonString);
            JSONArray jsonArray = (JSONArray) jsonObject.get("videos");
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject videoInfo = (JSONObject) jsonArray.get(i);
                String videoTitle = videoInfo.get("title").toString();
                if (videoTitle.equals(uploadedVideoTitle)) {
                    String videoID = videoInfo.get("id").toString();
                    videoLocation = "https://gsu-7zy7zle.vids.io/videos/" + videoID + "/"+uploadedVideoTitle;
                    return videoLocation;
                }
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
        return videoLocation;
    }


}
