import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClient;
import com.amazonaws.services.cloudsearchdomain.model.ContentType;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsRequest;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsResult;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.document.Attribute;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.xspec.ScanExpressionSpec;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Attr;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Created by Shakib on 1/17/2017.
 */
public class UploadToCloudSearch
{
    DynamoDB dynamoDB;
    AmazonDynamoDB amazonDynamoDB;//used for mapper
    DynamoDBMapper mapper;
    AmazonCloudSearchDomainClient cloudSearchClient;
    Table table;

    public UploadToCloudSearch() throws IOException, ParseException {
        GetPropertyValues propertyValues = new GetPropertyValues();
        final HashMap<String, String> credentials =  propertyValues.getAWSCredentials();
        final HashMap<String, String> endpoints = propertyValues.getEndpoints();

        /**Authenticate clients**/
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(credentials.get("AWSAccessKey"), credentials.get("AWSSecretKey"));
        dynamoDB = new DynamoDB(new AmazonDynamoDBClient(awsCredentials));
        table = dynamoDB.getTable("DALN-Posts");
        amazonDynamoDB = new AmazonDynamoDBClient(awsCredentials);
        mapper = new DynamoDBMapper((amazonDynamoDB));
        cloudSearchClient = new AmazonCloudSearchDomainClient(awsCredentials);
        System.out.println(endpoints.get("documentEndpoint"));
        cloudSearchClient.setEndpoint(endpoints.get("documentEndpoint"));

    }

    /**Convert single post to SDF**/
    public JSONObject convertDynamoEntryToAddSDF(String postID) throws ParseException, IOException {
        //Retrieve the post
        Item post = table.getItem("PostId", postID);

        JSONParser parser = new JSONParser();
        JSONObject postAsJSON = (JSONObject) parser.parse(post.toJSON());
        JSONObject postAsSDF = new JSONObject();

        postAsSDF.put("type", "add");
        postAsSDF.put("id", postID);
        JSONObject fields = new JSONObject();

        List assetList = post.getList("assetList");
        JSONArray assetNames = new JSONArray();
        JSONArray assetDescriptions = new JSONArray();
        JSONArray assetTypes = new JSONArray();
        JSONArray assetIDs = new JSONArray();
        JSONArray assetS3Links = new JSONArray();
        JSONArray assetEmbedLinks = new JSONArray();
        JSONArray assetLocations = new JSONArray();


        for (int i = 0; i < assetList.size(); i++) {
            Map asset = (Map) assetList.get(i);
            assetNames.add(asset.get("assetName"));
            assetDescriptions.add(asset.get("assetDescription"));
            assetTypes.add(asset.get("assetType"));
            assetIDs.add(asset.get("assetID"));
            if(asset.get("assetS3Link") != null)
                assetS3Links.add(asset.get("assetS3Link"));
            else
                assetS3Links.add("");
            assetEmbedLinks.add(asset.get("assetEmbedLink"));
            assetLocations.add(asset.get("assetLocation"));
        }



        fields.put("areallfilesuploaded", post.getInt("areAllFilesUploaded"));
        try {
            if (post.getInt("isPostNotApproved") == 0)
                fields.put("ispostnotapproved", post.getInt("isPostNotApproved"));
        }
        catch(NumberFormatException e)
        {
            System.out.println(postID + " isPostNotApproved is null");
        }
        fields.put("assetdescription", assetDescriptions);
        fields.put("assetembedlink", assetEmbedLinks);
        fields.put("assets3link", assetS3Links);
        fields.put("assetid", assetIDs);
        fields.put("assetlocation", assetLocations);
        fields.put("assetname", assetNames);
        fields.put("assettype", assetTypes);

        Iterator iterator = postAsJSON.keySet().iterator();
        while (iterator.hasNext()) {
            String attribute = (String) iterator.next();
            String sdfAttributeName = attribute.toLowerCase();

            if (attribute.equals("assetList"))
                continue;

            Object value = post.get(attribute);
            if (value instanceof String) {
                fields.put(sdfAttributeName, value.toString());
            } else {
                fields.put(sdfAttributeName, value);
            }
        }

        postAsSDF.put("fields", fields);

        return postAsSDF;

    }

    public JSONObject convertDynamoEntryToDeleteSDF(String postID) throws ParseException, IOException
    {
        JSONObject postAsSDF = new JSONObject();

        postAsSDF.put("type", "delete");
        postAsSDF.put("id", postID);

        return postAsSDF;
    }

    public void uploadSingleDocument(JSONObject documentAsSDF)
    {
        JSONArray document = new JSONArray();
        document.add(documentAsSDF);
        byte[] bytes = document.toJSONString().getBytes();
        long contentLength = bytes.length;
        System.out.println("document length:" + bytes.length);

        InputStream inputStream = new ByteArrayInputStream(bytes);
        UploadDocumentsRequest uploadDocumentsRequest = new UploadDocumentsRequest();
        uploadDocumentsRequest.setDocuments(inputStream);
        uploadDocumentsRequest.setContentType(ContentType.Applicationjson);
        uploadDocumentsRequest.setContentLength(contentLength);
        UploadDocumentsResult uploadDocumentsResult = cloudSearchClient.uploadDocuments(uploadDocumentsRequest);
        System.out.println("Document upload status: " + uploadDocumentsResult.getStatus());
    }

    public void uploadDocumentBatch() throws IOException, ParseException {

        List<Post> allPosts = mapper.scan(Post.class, new DynamoDBScanExpression().withProjectionExpression("PostId"));
        JSONArray documentBatch = new JSONArray();
        //for(Post post : allPosts)
        for(int i = 6000; i < 7097; i++)
        {
            Post post = allPosts.get(i);
            System.out.println(post.getPostId());
            documentBatch.add(convertDynamoEntryToAddSDF(post.getPostId()));

        }

        /*for(int i = 3500; i < allPosts.size(); i++)
        {
            Post post = allPosts.get(i);
            System.out.println(post.getPostId());
            documentBatch.add(convertDynamoEntryToAddSDF(post.getPostId()));

        }*/

        //5242880 = 5mb
        byte[] bytes = documentBatch.toJSONString().getBytes();
        long contentLength = bytes.length;

        InputStream inputStream = new ByteArrayInputStream(bytes);
        UploadDocumentsRequest uploadDocumentsRequest = new UploadDocumentsRequest();
        uploadDocumentsRequest.setDocuments(inputStream);
        uploadDocumentsRequest.setContentType(ContentType.Applicationjson);
        uploadDocumentsRequest.setContentLength(contentLength);
        UploadDocumentsResult uploadDocumentsResult = cloudSearchClient.uploadDocuments(uploadDocumentsRequest);
        System.out.println(uploadDocumentsResult.getStatus());
    }







}
