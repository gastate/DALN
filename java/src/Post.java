import java.util.Set;
import java.util.UUID;

import com.amazonaws.services.dynamodbv2.datamodeling.*;

/**
 * Created by Shakib on 7/5/2016.
 *
 * This class is a template for a post, specifically a post that will be inserted in the Posts
 * table in DynamoDB. Special annotations are used to utilize the DynamoDB wrapper, which specify the
 * table name, the primary key, and the rest of the attributes in the table.
 */

@DynamoDBTable(tableName = "DALN-Posts")
public class Post
{
    private String postId;
    private String description;
    private String author;
    private String title;
    private String date;
    private Set<String> assetList;
    private String dalnId;

    @DynamoDBHashKey(attributeName = "PostId")
    @DynamoDBAutoGeneratedKey
    public String getPostId() {return postId;}
    public void setPostId(String postId) {this.postId = postId;}

    @DynamoDBAttribute(attributeName = "Description")
    public String getDescription() {return description;}
    public void setDescription(String description) {this.description = description;}

    @DynamoDBAttribute(attributeName = "Author")
    public String getAuthor() {return author;}
    public void setAuthor(String author) {this.author = author;}

    @DynamoDBAttribute(attributeName = "UploadDate")
    public String getDate() {return date;}
    public void setDate(String date) {this.date = date;}

    @DynamoDBAttribute(attributeName = "AssetList")
    public void setAssetList(Set<String> assetList) {this.assetList = assetList;}
    public Set<String> getAssetList() {return assetList;}

    @DynamoDBAttribute(attributeName = "Title")
    public String getTitle() {return title;}
    public void setTitle(String title) {this.title = title;}

    @DynamoDBAttribute(attributeName = "DalnId")
    public String getDalnId() {return dalnId;}
    public void setDalnId(String dalnId) {this.dalnId = dalnId;}
}
