import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

import java.util.*;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

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
        //Create an instance of the post class, and fill it with information passed from the main.FileUploader class
        Post post = new Post();

        if (postDetails.get("title") != null) post.setTitle(postDetails.get("title").toString());
        if (postDetails.get("description") != null) post.setDescription(postDetails.get("description").toString());
        if (postDetails.get("identifierUri") != null) post.setIdentifierUri(postDetails.get("identifierUri").toString());
        if (postDetails.get("dateAccessioned") != null) post.setDateAccessioned(postDetails.get("dateAccessioned").toString());
        if (postDetails.get("dateAvailable") != null) post.setDateAvailable(postDetails.get("dateAvailable").toString());
        if (postDetails.get("dateCreated") != null) post.setDateCreated(postDetails.get("dateCreated").toString());
        if (postDetails.get("dateIssued") != null) post.setDateIssued(postDetails.get("dateIssued").toString());
        if (postDetails.get("rightsConsent") != null) post.setRightsConsent(postDetails.get("rightsConsent").toString());
        if (postDetails.get("rightsRelease") != null) post.setRightsRelease(postDetails.get("rightsRelease").toString());
        if (postDetails.get("contributorAuthor") != null) post.setContributorAuthor((List<String>)postDetails.get("contributorAuthor"));
        if (postDetails.get("contributorInterviewer") != null) post.setContributorInterviewer((List<String>)postDetails.get("contributorInterviewer"));
        if (postDetails.get("creatorGender") != null) post.setCreatorGender((List<String>)postDetails.get("creatorGender"));
        if (postDetails.get("creatorRaceEthnicity") != null) post.setCreatorRaceEthnicity((List<String>)postDetails.get("creatorRaceEthnicity"));
        if (postDetails.get("creatorClass") != null) post.setCreatorClass((List<String>)postDetails.get("creatorClass"));
        if (postDetails.get("creatorYearOfBirth") != null) post.setCreatorYearOfBirth((List<String>)postDetails.get("creatorYearOfBirth"));
        if (postDetails.get("coverageSpatial") != null) post.setCoverageSpatial((List<String>)postDetails.get("coverageSpatial"));
        if (postDetails.get("coveragePeriod") != null) post.setCoveragePeriod((List<String>)postDetails.get("coveragePeriod"));
        if (postDetails.get("coverageRegion") != null) post.setCoverageRegion((List<String>)postDetails.get("coverageRegion"));
        if (postDetails.get("coverageStateProvince") != null) post.setCoverageStateProvince((List<String>)postDetails.get("coverageStateProvince"));
        if (postDetails.get("coverageNationality") != null) post.setCoverageNationality((List<String>)postDetails.get("coverageNationality"));
        if (postDetails.get("language") != null) post.setLanguage((List<String>)postDetails.get("language"));
        if (postDetails.get("subject") != null) post.setSubject((List<String>)postDetails.get("subject"));
        post.setDalnId(postDetails.get("DalnId").toString());
        post.setAssetList((List<HashMap<String,String>>)postDetails.get("assetList"));
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
                .withFilterExpression("dalnId = :val1")
                .withExpressionAttributeValues(eav);

        List<Post> scanResults = mapper.scan(Post.class, scanExpression);

        //We only care if at least one result is found, so we check if the list size is 0
        boolean result = scanResults.size() != 0;
        return result;
    }
}
