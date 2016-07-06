import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

import java.util.*;

import com.amazonaws.services.dynamodbv2.datamodeling.*;

/**
 * Created by Shakib on 7/5/2016.
 */
public class DynamoDBClient {
    public String insertPost(HashMap postDetails)
    {
        /**Connect to DynamoDB with credentials and initialize wrapper**/
        AmazonDynamoDB dynamoDBClient = new AmazonDynamoDBClient(new ProfileCredentialsProvider("shakib"));
        DynamoDBMapper mapper = new DynamoDBMapper(dynamoDBClient);
        //DynamoDB dynamoDBClient = new DynamoDB(new AmazonDynamoDBClient(new ProfileCredentialsProvider("shakib")));
        //Table table = dynamoDBClient.getTable("DALN-Posts");

        //Create an instance of the post class, and fill it with information passed from the FileUploader class
        Post post = new Post();
        post.setTitle(postDetails.get("Title").toString());
        post.setAuthor(postDetails.get("Author").toString());
        post.setDate(postDetails.get("UploadDate").toString());
        post.setDescription(postDetails.get("Description").toString());
        post.setDalnId(postDetails.get("DalnId").toString());
        //Enter it into the DB
        mapper.save(post);

        return post.getPostId();
    }

    public void insertAsset(HashMap assetDetails)
    {
        /**Connect to DynamoDB with credentials and initialize wrapper**/
        AmazonDynamoDB dynamoDBClient = new AmazonDynamoDBClient(new ProfileCredentialsProvider("shakib"));
        DynamoDBMapper mapper = new DynamoDBMapper(dynamoDBClient);


        ArrayList<String> assetUUIDs = new ArrayList<>(); //create a list of all the assetIDs to be generated
        ArrayList<String> fileNames = (ArrayList<String>) assetDetails.get("AssetList"); //retrieve file names of method variable
        Integer numOfFiles = fileNames.size(); //retrieve size so that every file in a post will be visited

        for(int i = 0; i < numOfFiles; i++) //
        {
            Asset asset = new Asset();  //each asset is a new entry in the Assets table in the DB
            asset.setDalnId(assetDetails.get("DalnId").toString());
            //set other asset information here
            mapper.save(asset);
            assetUUIDs.add(asset.getAssetId());//save all asset UUIDs. They will be added to the post its associated with
        }

        Post post = mapper.load(Post.class, assetDetails.get("PostId")); //load the post that was created earlier
        updatePostsAndAssets(post, assetUUIDs);
    }

    public void updatePostsAndAssets(Post post, ArrayList<String> assetUUIDs)
    {
        /**Connect to DynamoDB with credentials and initialize wrapper**/
        AmazonDynamoDB dynamoDBClient = new AmazonDynamoDBClient(new ProfileCredentialsProvider("shakib"));
        DynamoDBMapper mapper = new DynamoDBMapper(dynamoDBClient);

        //Iterate through every asset UUID, and set its postID to the post it belongs to
        for(String assetUUID : assetUUIDs)
        {
            Asset asset = mapper.load(Asset.class, assetUUID);
            asset.setPostId(post.getPostId());
            mapper.save(asset);
        }

        //Store the list of asset UUIDs in a Set, and pass it on to the post that was entered into the DB earlier.
        //The post will now include a String set which includes every Asset UUID that is associated with it
        HashSet<String> assetList = new HashSet<>();
        assetList.addAll(assetUUIDs);
        post.setAssetList(assetList);
        mapper.save(post);
    }
}
