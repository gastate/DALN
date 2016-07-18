import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.File;
import java.util.HashMap;

/**
 * Created by Shakib on 7/16/2016.
 */
public class UploadToS3
{
    private AmazonS3Client s3Client;
    private HashMap<String, Object> postDetails;
    private String dalnId, fileName;
    public UploadToS3(HashMap<String, Object> postDetails)
    {
        s3Client = new AmazonS3Client(new ProfileCredentialsProvider("daln"));

        this.postDetails = postDetails;
        dalnId = postDetails.get("DalnId").toString();
        fileName = postDetails.get("Current File").toString();

        uploadFile();
    }

    public void uploadFile()
    {
        try {
            System.out.print("Uploading " + fileName + " to S3...");

            //Specifying the upload location of our in S3 and set it to public read
            File file = new File("downloads/" + dalnId + "/" + fileName);
            s3Client.putObject(new PutObjectRequest("daln", "Posts/" + dalnId + "/" + fileName, file)
                    .withCannedAcl(CannedAccessControlList.PublicRead));


        } catch (AmazonServiceException ase) {
            System.out.println();
            System.out.println("Caught an AmazonServiceException, which " +
                    "means your request made it " +
                    "to Amazon S3, but was rejected with an error response" +
                    " for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which " +
                    "means the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    }

    public String getS3FileLocation()
    {
        return "https://s3-us-west-1.amazonaws.com/daln/Posts/" + dalnId + "/" + fileName;
    }
}