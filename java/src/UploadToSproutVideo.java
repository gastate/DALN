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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

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
    private HttpGet getFile;
    private StatusMessages message;

    public UploadToSproutVideo(HashMap<String, Object> postDetails) throws IOException {
        message = new StatusMessages();
        connectToSpoutVideo();

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

        uploadVideo();
    }

    public void connectToSpoutVideo() throws IOException {
        /**Connect to SproutVideo**/
        GetPropertyValues properties = new GetPropertyValues();
        httpClient = HttpClients.createDefault();
        uploadFile = new HttpPost("https://api.sproutvideo.com/v1/videos");
        uploadFile.addHeader("SproutVideo-Api-Key", properties.getSproutVideoApiKey());
        getFile =  new HttpGet("https://api.sproutvideo.com/v1/videos?order_by=title");
        getFile.addHeader("SproutVideo-Api-Key", properties.getSproutVideoApiKey());
    }

    public void uploadVideo()
    {
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
            //System.out.println("\n"+fileName + " could not be uploaded to SproutVideo.");
            message.FileUploadAssetErrorLog(assetID);
        }

    }

    public String getSpoutVideoLocation() {
        //The HTTP Response must be parsed to retrieve the location of the uploaded video

        String videoLocation = "";
        String uploadedVideoTitle = postDetails.get("Current Asset ID").toString();
        CloseableHttpResponse getResponse = null;
        //The getResponse must be parsed as a JSON to retrieve the downloaded location
        try {
            getResponse = httpClient.execute(getFile);
            String jsonString = EntityUtils.toString(getResponse.getEntity());
            //System.out.println(jsonString);
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(jsonString);
            JSONArray jsonArray = (JSONArray) jsonObject.get("videos");
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject videoInfo = (JSONObject) jsonArray.get(i);
                String videoTitle = videoInfo.get("title").toString();
                if (videoTitle.equals(uploadedVideoTitle)) {
                    String videoID = videoInfo.get("id").toString();
                    //String embedCode = videoInfo.get("embed_code").toString();
                    //Element iframe = Jsoup.parse(embedCode).select("iframe").first();
                    //String videoSource = iframe.attr("src");
                    videoLocation = "https://mwharker.vids.io/videos/"+videoID+"/"+videoTitle;
                    return videoLocation;
                }
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
        return videoLocation;
    }


}
