import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

import java.util.*;

import com.amazonaws.services.dynamodbv2.datamodeling.*;

/**
 * Created by Shakib on 7/5/2016.
 */
public class DynamoDBClient {
    AmazonDynamoDB dynamoDBClient;
    DynamoDBMapper mapper;
    ArrayList<String> allAssetUUIDs, videoAssetUUIDs, assetLocations;
    Post currentPost;

    public DynamoDBClient()
    {
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
        //Enter it into the DB
        mapper.save(post);

        return post.getPostId(); //return the UUID generated from the insertion into DB
    }

    public ArrayList<String> insertAsset(HashMap assetDetails)
    {
        allAssetUUIDs = new ArrayList<>(); //create a list of all the assetIDs to be generated

        ArrayList<String> fileNames = (ArrayList<String>) assetDetails.get("AssetList"); //retrieve file names from the method variable
        ArrayList<String> fileTypes = (ArrayList<String>) assetDetails.get("FileType");
        Integer numOfFiles = fileNames.size(); //retrieve size so that every file in a post will be visited

        for(int i = 0; i < numOfFiles; i++) //
        {
            Asset asset = new Asset();  //each asset is a new entry in the Assets table in the DB
            asset.setDalnId(assetDetails.get("DalnId").toString());
            asset.setAssetType(fileTypes.get(i));
            //set other asset information here
            mapper.save(asset);
            allAssetUUIDs.add(asset.getAssetId());//save all asset UUIDs. They will be added to the post its associated with
        }
        currentPost = mapper.load(Post.class, assetDetails.get("PostId")); //load the post that was created earlier
        return allAssetUUIDs;
    }

    public void setAssetLocations(ArrayList<String> assetLocations)
    {
        this.assetLocations = assetLocations;
    }

    public void updatePostsAndAssets()
    {
        //Iterate through every asset UUID, and set its postID to the post it belongs to
        int i = 0;
        for(String assetUUID : allAssetUUIDs)
        {
            Asset asset = mapper.load(Asset.class, assetUUID);
            asset.setPostId(currentPost.getPostId());
            asset.setAssetLocation(assetLocations.get(i));
            mapper.save(asset);
            i++;
        }

        //Store the list of asset UUIDs in a Set, and pass it on to the post that was entered into the DB earlier.
        //The post will now include a String set which includes every Asset UUID that is associated with it
        HashSet<String> assetList = new HashSet<>();
        assetList.addAll(allAssetUUIDs);
        currentPost.setAssetList(assetList);
        mapper.save(currentPost);
    }
}
