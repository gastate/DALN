/**
 * Created by Shakib on 7/26/2016.
 */
public class StatusMessages
{
    public void ConnectingTo(String connection)
    {
        System.out.println("Connecting to " + connection + "...");
    }

    /**PostImporter progress messages**/
    public void DALNConnectionError()
    {
        System.out.println("Could not connect to the website, or the post you entered does not exist on the DALN website.");
    }

    public void DALNPostDoesNotExist()
    {
        System.out.println("The post you entered does not exist on the DALN website.");
    }

    public void DownloadingFiles()
    {

        System.out.println("Downloading metadata and files to directory");
    }

    public void FileLinkInvalid()
    {
        System.out.println("Cannot access the file. The link to the file may be invalid or no files exist.");
    }


    /**PostImporter completed/failure messages**/
    public void PostImportCompleteVerbose(String postID)
    {
        System.out.println("Post #"+postID+" successfully downloaded to working directory.");
    }

    public void PostImportBeginLog(String postID)
    {
        System.out.println("postid:"+postID+"|action:download|status:initiated");
    }
    public void PostImportCompleteLog(String postID)
    {
        System.out.println("postid:"+postID+"|action:download|status:completed");
    }

    public void PostImportErrorLog(String postID)
    {
        System.out.println("postid:"+postID+"|action:download|status:error");
    }

    /**FileUploader progress messages**/
    public void CannotFindPostToUpload()
    {
        System.out.println("This post hasn't been downloaded yet or this post does not exist.");
    }

    public void PostAlreadyExistsInDB()
    {
        System.out.println("This post ID already exists in the database.");
    }

    public void BeginPostUpload(String postID)
    {
        System.out.println("You are uploading post #" + postID + ".");
    }

    public void CreateS3Data()
    {
        System.out.println("Creating post folder in S3 and uploading metadata.");
    }

    public void UploadingToSproutVideo(String fileName, String assetID)
    {
        System.out.print("Uploading the video file " + fileName + " as " + assetID + " to SproutVideo...");
    }

    public void UploadingToSoundCloud(String fileName, String assetID)
    {
        System.out.print("Uploading the audio file " + fileName + " as " + assetID + " to SoundCloud...");
    }

    public void UploadingToS3(String fileName)
    {
        System.out.print("Uploading the file " + fileName + " to S3...");
    }


    public void UpdatingMetadata()
    {
        System.out.println("Updating metadata with database information.");
    }

    public void NoFileType(String fileName)
    {
        System.out.println("The file \"" + fileName + "\" does not have a file type specified. This post can't be uploaded. Please" +
                " rename the file in your working directory to include a valid extension as well as edit the file name in the" +
                " metadata text file that was generated.");
    }


    /**FileUploader completed/failure messages**/
    public void FileUploadAssetBeginLog(String assetID)
    {
        System.out.println("assetid:"+assetID+"|action:upload|status:initiated");
    }

    public void FileUploadAssetCompleteLog(String assetID)
    {
        System.out.println("assetid:"+assetID+"|action:upload|status:completed");
    }

    public void FileUploadAssetErrorLog(String assetID)
    {
        System.out.println("assetid:"+assetID+"|action:upload|status:error");
    }


    public void FileUploadPostCompleteVerbose(String postID)
    {
        System.out.println("Post #" + postID + " successfully uploaded and added to database.");
    }

    public void FileUploadPostBeginLog(String postID)
    {
        System.out.println("postid:"+postID+"|action:upload|status:initiated");
    }

    public void FileUploadPostCompleteLog(String postID)
    {
        System.out.println("postid:"+postID+"|action:upload|status:completed");
    }

    public void FileUploadPostErrorLog(String postID)
    {
        System.out.println("postid:"+postID+"|action:upload|status:error");
    }
}
