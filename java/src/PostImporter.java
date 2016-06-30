import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;

/**
 * Created by Shakib on 6/25/2016.
 *
 * The purpose of this class is to retrieve files from a post and its post metadata stored on OSU's DALN website.
 * Each record in the DALN has a unique identifier, so the method will take this ID as input, find the file(s)
 * posted in that record (if any) along with post metadata, then store it in the working directory.
 */

public class PostImporter
{
    public void importPost(String postID)
    {
        /**Connect to URL of post**/
        String website = "http://daln.osu.edu/handle/2374.DALN/" + postID;
        Document doc = null;
        try {
            System.out.println("Connecting to " + website + "...");
            doc = Jsoup.connect(website).get();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /**There are two tables of information from the page that we need. The first table includes the title, author,
         * description, and date of the post. The second table includes the list of files that are contained within the post.
         */
        Elements pageTables = doc.select("table");

        /**Getting the post info**/
        Element postInfoBody = pageTables.get(0).child(0); //first table of the page
        Elements postInfoTableRows = postInfoBody.select("tr");

        String titleData = "None", descriptionData = "None", authorData = "None", dateData = "None";
        //Checks each row in the first table, determines its category, then extracts the contents of the category
        for(Element postInfoTableRow : postInfoTableRows)
        {
           switch(postInfoTableRow.child(0).child(0).ownText().trim())
           {
               case "Title:":
                   titleData = postInfoTableRow.child(1).ownText();
                   break;
               case "Description:":
                   descriptionData = postInfoTableRow.child(1).ownText();
                   break;
               case "Author:":
                   authorData = postInfoTableRow.child(1).ownText();
                   break;
               case "Date:":
                   dateData = postInfoTableRow.child(1).ownText();
                   break;
           }
        }

        /**Getting the file(s) info**/
        Element fileInfoBody = pageTables.get(1).child(0); //second table of the page

        Elements fileInfoTableRows = fileInfoBody.select("tr");
        int numOfFiles = fileInfoTableRows.size()-1; //Each row is a file, besides the first which is the table header
        ArrayList<String> fileNames = new ArrayList<String>();
        ArrayList<String> fileLinks = new ArrayList<String>();

        for(int i = 1; i <= numOfFiles ; i++) {
            fileNames.add(fileInfoTableRows.get(i).child(0).child(0).attr("title"));
            fileLinks.add(fileInfoTableRows.get(i).child(0).child(0).attr("abs:href"));
        }

        System.out.println("Downloading metadata and files to directory");
        /**Storing post metadata in a text file**/
        File newFolder = new File("downloads/"+ postID);
        newFolder.mkdir(); //create a new folder with the post ID

        //Write the file
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream("downloads/"+postID+ "/Post #"+postID + " Data.txt"), "utf-8")))
        {
            writer.write("Link: " + website
                        +"\r\nTitle: " + titleData
                        +"\r\nDescription: " + descriptionData
                        +"\r\nAuthor: " + authorData
                        +"\r\nDate: " + dateData
                        +"\r\nFiles: " + numOfFiles + "\r\n");

            for(String file : fileNames)
                writer.write("      " +file + "\r\n");

        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /**Download file(s) to working directory**/
        URL url = null;
        int i = 0;
        //Navigate to each file and download
        for(String link : fileLinks) {

            //navigate to link
            try {
                url = new URL(link);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            //download with specified target and name
            try {
                FileUtils.copyURLToFile(url, new File("downloads/"+postID +"/"+fileNames.get(i)));
            } catch (IOException e) {
                e.printStackTrace();
            }
            i++;

        }
        System.out.println("Post #"+postID+" downloaded to working directory.");
    }
}
