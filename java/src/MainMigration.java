/**
 * Created by lkittogsuedu on 1/17/17.
 */

import org.apache.http.ParseException;
import org.apache.log4j.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.nio.file.Paths;


public class MainMigration {
    private static final Logger LOGGER = Logger.getLogger(MainMigration.class.getName());
    private static Date today = new Date();
    private static String formattedDate = new SimpleDateFormat("yyyy-MM-dd_HHmmss").format(today);
    private static String currentDirectory = Paths.get(".").toAbsolutePath().normalize().toString();
    private static final String FAILED_POSTS = currentDirectory + "/" + formattedDate + "_REDOS.txt";


    public static void main(String[] args) {

        /*
        String filePath;
        String dalnId = null;
        boolean verboseOutput = true;

        BufferedReader br = null;
        FileReader fr = null;

        if (args.length == 0) {
            LOGGER.error("No file given for import");
            System.exit(1);
        }

        final String FILEPATH = args[0];


        try {

            fr = new FileReader(FILEPATH);
            br = new BufferedReader(fr);


            br = new BufferedReader(new FileReader(FILEPATH));
            String line;

            while ((line = br.readLine()) != null) {

                try {
                    System.out.println(line);
                    dalnId = line;
                    //dalnId = line.split(",")[0];

                    //throw new Exception("Testing file ");
                    new PostImporter(dalnId, verboseOutput);
                    new FileUploader(dalnId, verboseOutput);
                } catch (Exception e) {

                    e.printStackTrace();
                    //LOGGER.error("\n========================\n" + dalnId + " : " + e.getMessage() + "\n========================\n");
                    //logFailure(dalnId);
                }
            }

        } catch (IOException e) {
            LOGGER.error(e.getMessage());

        } finally {

            try {

                if (br != null)
                    br.close();

                if (fr != null)
                    fr.close();

            } catch (IOException ex) {

                MainMigration.LOGGER.error(ex.getMessage());

            }

        }

    }

    public static void logFailure(String id) {
        BufferedWriter bw = null;
        FileWriter fw = null;
        File file = new File(FAILED_POSTS);

        try {

            if (file.exists() && !file.isDirectory()) {
                fw = new FileWriter(FAILED_POSTS, true);
            } else {
                fw = new FileWriter(FAILED_POSTS);
            }

            bw = new BufferedWriter(fw);
            bw.write(id + "\r\n");

        } catch (IOException e) {

            LOGGER.error(e.getMessage());

        } finally {

            try {

                if (bw != null)
                    bw.close();

                if (fw != null)
                    fw.close();

            } catch (IOException ex) {

                LOGGER.error(ex.getMessage());

            }

        }
*/
    }


}



