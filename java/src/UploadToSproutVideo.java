import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
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
    private static Logger log = Logger.getLogger(UploadToSproutVideo.class.getName());
    private HashMap<String,Object> postDetails;
    private String dalnId, originalLink, originalPostTitle,fileName, assetID, fullDescription, fullTitle;
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

        originalPostTitle = postDetails.get("title").toString();
        fileName = postDetails.get("Current File").toString();
        assetID = postDetails.get("Current Asset ID").toString();

        String fileNameNoExt = fileName.substring(0, fileName.lastIndexOf('.'));
        fullTitle = originalPostTitle + " - " + fileNameNoExt;
        fullDescription = "Original Post Link: " + originalLink
                + "\nFile Name: " + fileName;


        uploadVideo();
    }

    public void connectToSpoutVideo() throws IOException {
        /**Connect to SproutVideo**/
        GetPropertyValues properties = new GetPropertyValues();
        httpClient = HttpClients.createDefault();
        uploadFile = new HttpPost("https://api.sproutvideo.com/v1/videos");
        uploadFile.addHeader("SproutVideo-Api-Key", properties.getSproutVideoApiKey());
        getFile =  new HttpGet("https://api.sproutvideo.com/v1/videos?order_by=created_at&order_dir=desc");
        getFile.addHeader("SproutVideo-Api-Key", properties.getSproutVideoApiKey());
    }

    public void uploadVideo()
    {

        //SproutVideo API uploads accept Multipart or Formdata as its format
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        //For the video submission

        builder.addTextBody("title", fullTitle, ContentType.TEXT_PLAIN); //combination
        builder.addTextBody("description", fullDescription, ContentType.TEXT_PLAIN);
        builder.addTextBody("privacy", 2 + "", ContentType.TEXT_PLAIN);
        builder.addTextBody("tag_names", assetID, ContentType.TEXT_PLAIN);
        builder.addBinaryBody("source_video", new File("downloads/" + dalnId + "/" + fileName), ContentType.APPLICATION_OCTET_STREAM, fileName);
        HttpEntity multipart = builder.build();
        uploadFile.setEntity(multipart);

        CloseableHttpResponse postResponse = null;
        try{
            postResponse = httpClient.execute(uploadFile);
       } catch (IOException e) {
            log.error("\n"+fileName + " could not be uploaded to SproutVideo.");
            log.error(message.FileUploadAssetErrorLog(assetID));
        }
    }

    public String[] getSpoutVideoLocation() {
        //The HTTP Response must be parsed to retrieve the location of the uploaded video

        String[] videoLocations = new String[2];
        //String uploadedVideoTitle = postDetails.get("Current Asset ID").toString();

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
               if (videoTitle.equals(fullTitle)) {
                    String videoID = videoInfo.get("id").toString();
                    String embedCode = videoInfo.get("embed_code").toString();
                    Element iframe = Jsoup.parse(embedCode).select("iframe").first();
                    videoLocations[0] = "https://mwharker.vids.io/videos/"+videoID+"/"+videoTitle;
                    videoLocations[1] = iframe.attr("src");

                    return videoLocations;
                }
            }
        } catch (ParseException | IOException | NullPointerException e) {
            log.error("Problem getting video location.");
            e.printStackTrace();
        }
        return videoLocations;
    }


}
