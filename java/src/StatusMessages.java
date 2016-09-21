
/**
 * Created by Shakib on 7/26/2016.
 */
public class StatusMessages
{
    public String ConnectingTo(String connection)
    {
        return "Connecting to " + connection + "...";
    }

    /**PostImporter progress messages**/
    public String DALNConnectionError()
    {
        return "Could not connect to the website, or the post you entered does not exist on the DALN website.";
    }

    public String DALNPostDoesNotExist()
    {
        return "The post you entered does not exist on the DALN website.";
    }

    public String DownloadingFiles()
    {

        return "Downloading metadata and files to directory";
    }

    public String FileLinkInvalid()
    {
        return "Cannot access the file. The link to the file may be invalid or no files exist.";
    }


    /**PostImporter completed/failure messages**/
    public String PostImportCompleteVerbose(String postID)
    {
        return "Post #"+postID+" successfully downloaded to working directory.";
    }

    public String PostImportBeginLog(String postID)
    {
        return "postid:"+postID+"|action:download|status:initiated";
    }
    public String PostImportCompleteLog(String postID)
    {
        return "postid:"+postID+"|action:download|status:completed";
    }

    public String PostImportErrorLog(String postID)
    {
        return "postid:"+postID+"|action:download|status:error";
    }

    /**main.FileUploader progress messages**/
    public String CannotFindPostToUpload()
    {
        return "This post hasn't been downloaded yet or this post does not exist.";
    }

    public String PostAlreadyExistsInDB()
    {
        return "This post ID already exists in the database.";
    }

    public String BeginPostUpload(String postID)
    {
        return "You are uploading post #" + postID + ".";
    }

    public String CreateS3Data()
    {
        return "Creating post folder in S3 and uploading metadata.";
    }

    public String UploadingToSproutVideo(String fileName, String assetID)
    {
        return "Uploading the video file " + fileName + " as " + assetID + " to SproutVideo...";
    }

    public String UploadingToSoundCloud(String fileName, String assetID)
    {
        return "Uploading the audio file " + fileName + " as " + assetID + " to SoundCloud...";
    }

    public String UploadingToS3(String fileName)
    {
        return "Uploading the file " + fileName + " to S3...";
    }


    public String UpdatingMetadata()
    {
        return "Updating metadata with database information.";
    }

    public String NoFileType(String fileName)
    {
        return "The file \"" + fileName + "\" does not have a file type specified. This post can't be uploaded. Please" +
                " rename the file in your working directory to include a valid extension as well as edit the file name in the" +
                " metadata text file that was generated.";
    }


    /**main.FileUploader completed/failure messages**/
    public String FileUploadAssetBeginLog(String assetID)
    {
        return "assetid:"+assetID+"|action:upload|status:initiated";
    }

    public String FileUploadAssetCompleteLog(String assetID)
    {
        return "assetid:"+assetID+"|action:upload|status:completed";
    }

    public String FileUploadAssetErrorLog(String assetID)
    {
        return "assetid:"+assetID+"|action:upload|status:error";
    }


    public String FileUploadPostCompleteVerbose(String postID)
    {
        return "Post #" + postID + " successfully uploaded and added to database.";
    }

    public String FileUploadPostBeginLog(String postID)
    {
        return "postid:"+postID+"|action:upload|status:initiated";
    }

    public String FileUploadPostCompleteLog(String postID)
    {
        return "postid:"+postID+"|action:upload|status:completed";
    }

    public String FileUploadPostErrorLog(String postID)
    {
        return "postid:"+postID+"|action:upload|status:error";
    }
}
