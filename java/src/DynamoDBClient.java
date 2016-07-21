import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

import java.io.IOException;
import java.util.*;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * Created by Shakib on 7/5/2016.
 *
 * The purpose of this class is to act as the client between the program and DynamoDB. In includes methods to insert
 * new posts and to scan for posts to prevent duplicate entries. Later on this class may be used to search the database.
 */

public class DynamoDBClient {
    AmazonDynamoDB dynamoDBClient;
    DynamoDBMapper mapper;

    public DynamoDBClient() {
        /**Connect to DynamoDB with credentials and initialize wrapper**/
        dynamoDBClient = new AmazonDynamoDBClient(new ProfileCredentialsProvider("daln"));
        mapper = new DynamoDBMapper(dynamoDBClient);
    }
    public String insertPost(HashMap postDetails)
    {
        //Create an instance of the post class, and fill it with information passed from the FileUploader class
        Post post = new Post();
        post.setTitle(postDetails.get("Title").toString());
        post.setAuthor(postDetails.get("Author").toString());
        post.setDate(postDetails.get("UploadDate").toString());
        post.setDescription(postDetails.get("Description").toString());
        post.setDalnId(postDetails.get("DalnId").toString());
        post.setAssetList((List<HashMap<String,String>>)postDetails.get("AssetList"));
        //Enter it into the DB
        mapper.save(post); //post UUID is generated once this function is called

        return post.getPostId(); //return the UUID generated from the insertion into DB
    }

    //This method checks if the post being uploaded already exists in the database by scanning for the same DALN ID in the Posts table.
    public boolean checkIfPostAlreadyExistsInDB(String postID)
    {
        Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
        eav.put(":val1", new AttributeValue().withS(postID));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("DalnId = :val1")
                .withExpressionAttributeValues(eav);

        List<Post> scanResults = mapper.scan(Post.class, scanExpression);

        //We only care if at least one result is found, so we check if the list size is 0
        boolean result = scanResults.size() != 0;
        return result;
    }
}
