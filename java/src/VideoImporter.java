/**
 * Created by Shakib on 6/25/2016.
 *
 * The purpose of this class is to retrieve videos and its post metadata stored on OSU's DALN website and upload them to SproutVideo.
 * Each record in the DALN has a unique identifier, so the method will take this ID as input, find the video(s)
 * posted in that record (if any) along with post metadata, store in the working directory, then upload to SproutVideo using its API.
 */

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;

public class VideoImporter
{
    public void importVideo(String postID)
    {
        /**Connect to URL of post**/
        String website = "http://daln.osu.edu/handle/2374.DALN/" + postID;
        Document doc = null;
        try {
            doc = Jsoup.connect(website).get();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /**There are two tables of information from the page that we need. The first table includes the title, author,
         * and date of the post. The second table includes the list of files that are contained within the post.
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

        for(int i = 1; i <= numOfFiles ; i++)
            fileNames.add(fileInfoTableRows.get(i).child(0).child(0).ownText());

        System.out.println("Title: " + titleData);
        System.out.println("Description: " + descriptionData);
        System.out.println("Author: " + authorData);
        System.out.println("Date: " + dateData);
        for(String file : fileNames)
                System.out.println("File: " + file);

        /**Download video to working directory**/
        URL url = null;
        FileSystem fs = FileSystems.getDefault();
        //Navigate to each video file and download
        for(String file : fileNames) {
            file = file.replace(" ", "%20"); //Necessary to replace spaces for link to work correctly
            if(file.contains(".mov") || file.contains(".mp4") || file.contains(".wav")) {
                //navigate to video link
                try {
                    url = new URL("http://daln.osu.edu/bitstream/handle/2374.DALN/"+postID+"/"+ file);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                file = file.replace("%20", " ");
                Path target = fs.getPath("downloads/"+file); //define location and name of video here

                //download video with specified target and name
                try (InputStream in = url.openStream()) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
