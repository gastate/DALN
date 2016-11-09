import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.Versioned;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by Shakib on 6/25/2016.
 *
 * The purpose of this class is to retrieve files from a post and its post metadata stored on OSU's DALN website.
 * Each record in the DALN has a unique identifier, so the method will take this ID as input, find the file(s)
 * posted in that record (if any) along with post metadata, then store it in the working directory.
 */

public class PostImporter
{
    private static Logger log = Logger.getLogger(PostImporter.class.getName());
    private StatusMessages message;
    private String postID;
    private boolean verboseOutput;
    private HashMap<String,Object> postDetails;
    private DynamoDBClient client;

    //fields with one or more entry possible
    private ArrayList<String>
            contributorAuthor, contributorInterviewer,
            creatorGender, creatorRaceEthnicity, creatorClass, creatorYearOfBirth,
            coverageSpatial, coveragePeriod, coverageRegion, coverageStateProvince, coverageNationality,
            language, subject;

    //fields with only one entry allowed
    private String
            title, description, identifierUri,
            dateAccessioned, dateAvailable, dateCreated, dateIssued,
            rightsConsent, rightsRelease;

    private String[] singleEntryFields, multiEntryFields;
    private HashMap<String,String> singleEntryMap;
    private HashMap<String,ArrayList<String>> multiEntryMap;

    private ArrayList<String>
            fileNames, fileLinks, fileDescriptions;

    public PostImporter(){}

    public PostImporter(String postID, boolean verboseOutput) throws TransformerException, ParserConfigurationException, IOException {
        message = new StatusMessages();
        this.postID = postID;
        this.verboseOutput = verboseOutput;
        postDetails = new HashMap<>();
        client = new DynamoDBClient();

        title = "";
        description = "";
        identifierUri = "";
        dateAccessioned = "";
        dateAvailable = "";
        dateCreated = "";
        dateIssued = "";
        rightsConsent = "";
        rightsRelease = "";

        contributorAuthor = new ArrayList<>();
        contributorInterviewer = new ArrayList<>();
        creatorGender = new ArrayList<>();
        creatorRaceEthnicity = new ArrayList<>();
        creatorClass = new ArrayList<>();
        creatorYearOfBirth = new ArrayList<>();
        coverageSpatial = new ArrayList<>();
        coveragePeriod = new ArrayList<>();
        coverageRegion = new ArrayList<>();
        coverageStateProvince = new ArrayList<>();
        coverageNationality = new ArrayList<>();
        language = new ArrayList<>();
        subject = new ArrayList<>();

        fileNames = new ArrayList<>();
        fileLinks = new ArrayList<>();
        fileDescriptions = new ArrayList<>();

        doImport();
    }

    public void initializeFields()
    {
        singleEntryFields = new String[]
                {"title", "description",
                 "identifierUri", "dateAccessioned",
                 "dateAvailable", "dateCreated",
                 "dateIssued", "rightsConsent", "rightsRelease"};

        singleEntryMap = new HashMap<>();
        singleEntryMap.put("title", title);
        singleEntryMap.put("description",description);
        singleEntryMap.put("identifierUri",identifierUri);
        singleEntryMap.put("dateAccessioned",dateAccessioned);
        singleEntryMap.put("dateAvailable",dateAvailable);
        singleEntryMap.put("dateCreated",dateCreated);
        singleEntryMap.put("dateIssued",dateIssued);
        singleEntryMap.put("rightsConsent", rightsConsent);
        singleEntryMap.put("rightsRelease", rightsRelease);


        multiEntryFields = new String[]
                {"contributorAuthor", "contributorInterviewer",
                        "creatorGender", "creatorRaceEthnicity", "creatorClass", "creatorYearOfBirth",
                        "coverageSpatial", "coveragePeriod", "coverageRegion", "coverageStateProvince", "coverageNationality",
                        "language", "subject"};

        multiEntryMap = new HashMap<>();
        multiEntryMap.put("contributorAuthor", contributorAuthor);
        multiEntryMap.put("contributorInterviewer",contributorInterviewer);
        multiEntryMap.put("creatorGender",creatorGender);
        multiEntryMap.put("creatorRaceEthnicity",creatorRaceEthnicity);
        multiEntryMap.put("creatorClass",creatorClass);
        multiEntryMap.put("creatorYearOfBirth",creatorYearOfBirth);
        multiEntryMap.put("coverageSpatial",coverageSpatial);
        multiEntryMap.put("coveragePeriod", coveragePeriod);
        multiEntryMap.put("coverageRegion",coverageRegion);
        multiEntryMap.put("coverageStateProvince",coverageStateProvince);
        multiEntryMap.put("coverageNationality",coverageNationality);
        multiEntryMap.put("language",language);
        multiEntryMap.put("subject",subject);

    }

    public String[] getSingleEntryFields() {
        return singleEntryFields;
    }

    public String[] getMultiEntryFields() {
        return multiEntryFields;
    }

    public void doImport() throws TransformerException, ParserConfigurationException {
        if(client.checkIfIDAlreadyExistsInDB(postID))
        {
            if(client.areAllFilesUploaded(postID))
            {
                if(verboseOutput) log.error(message.PostAlreadyExistsInDB()); else log.error(message.FileUploadPostErrorLog(postID));
                return;
            }else
            {
                log.info("This post exists but not all media is present. Re-downloading post.");
                client.deletePost(postID);
            }
        }

        if (!verboseOutput) log.info(message.PostImportBeginLog(postID));
        /**Connect to the URL of post so that we can parse the elements on the page to find the necessary information.**/
        String xmlFile = "http://daln.osu.edu/metadata/handle/2374.DALN/" + postID + "/mets.xml";
        Document doc = null;
        try {
            if (verboseOutput) log.info(message.ConnectingTo(xmlFile));
            doc = Jsoup.connect(xmlFile).get();
        } catch (IOException e) {
            if (verboseOutput) log.error(message.DALNConnectionError());
            else log.error(message.PostImportErrorLog(postID));
            //System.exit(1);
        }

        Element root = null;
        try {
            root = doc.child(0);
        }catch(NullPointerException e)
        {
            if (verboseOutput) log.error(message.DALNConnectionError());
            else log.error(message.PostImportErrorLog(postID));
        }
        if(root.getElementsByTag("h1").text().equals("Resource not found"))
        {
            if (verboseOutput) log.error(message.DALNPostDoesNotExist());
            else log.error(message.PostImportErrorLog(postID));
            return;
            //System.exit(1);
        }
        else
        {
            Element postInfoRoot = root.child(0);
            Element fileInfoRoot = root.child(1);

            //GETTING POST INFO//
            Element postDataRoot = postInfoRoot.child(0).child(0).child(0);
            Elements postData = postDataRoot.children();
            for (Element field : postData) {
                String element = field.attr("element");
                String qualifier = field.attr("qualifier");
                String fieldText = field.ownText();

                String combinedAttr = element + " " + qualifier;
                switch (combinedAttr) {
                    case "title ":
                        title = fieldText;
                        break;
                    case "description ":
                        description = fieldText;
                        break;
                    case "date created":
                        dateCreated = fieldText;
                        break;
                    case "date accessioned":
                        dateAccessioned = fieldText;
                        break;
                    case "date available":
                        dateAvailable = fieldText;
                        break;
                    case "date issued":
                        dateIssued = fieldText;
                        break;
                    case "identifier uri":
                        identifierUri = fieldText;
                        break;
                    case "rights consent":
                        rightsConsent = fieldText;
                        break;
                    case "rights release":
                        rightsRelease = fieldText;
                        break;
                    case "contributor author":
                        contributorAuthor.add(fieldText);
                        break;
                    case "contributor interviewer":
                        contributorInterviewer.add(fieldText);
                        break;
                    case "coverage spatial":
                        coverageSpatial.add(fieldText);
                        break;
                    case "coverage period":
                        coveragePeriod.add(fieldText);
                        break;
                    case "coverage region":
                        coverageRegion.add(fieldText);
                        break;
                    case "coverage stateprovince":
                        coverageStateProvince.add(fieldText);
                        break;
                    case "coverage nationality":
                        coverageNationality.add(fieldText);
                        break;
                    case "creator gender":
                        creatorGender.add(fieldText);
                        break;
                    case "creator raceethnicity":
                        creatorRaceEthnicity.add(fieldText);
                        break;
                    case "creator class":
                        creatorClass.add(fieldText);
                        break;
                    case "creator yearofbirth":
                        creatorYearOfBirth.add(fieldText);
                        break;
                    case "subject ":
                        subject.add(fieldText);
                        break;
                    case "language ":
                        language.add(fieldText);
                        break;
                }

            }

            //GETTING FILE INFO//
            Elements allFileData = fileInfoRoot.select("[USE=CONTENT]").first().children();
            for (Element file : allFileData) {
                Element mainFileInfo = file.select("[loctype=URL]").first();
                String fileName = mainFileInfo.attr("xlink:title");
                String fileLink = mainFileInfo.attr("xlink:href");
                String fileDescription = mainFileInfo.attr("xlink:label");

                fileNames.add(fileName);
                fileLinks.add(fileLink);
                fileDescriptions.add(fileDescription);
            }

            initializeFields();
            createMetadataTextFile();
            downloadFiles();
        }
    }

    public void createMetadataTextFile() throws ParserConfigurationException, TransformerException {


        /**Storing post metadata in an xml file. This file will be parsed in the main.FileUploader class to figure out
         * the information needed to uploaded each file.**/
        File downloadsFolder = new File("downloads");
        downloadsFolder.mkdir();
        File newFolder = new File("downloads/" + postID);
        newFolder.mkdir(); //create a new folder with the post ID

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        // root elements
        org.w3c.dom.Document doc = docBuilder.newDocument();
        org.w3c.dom.Element rootElement = doc.createElement("post");
        doc.appendChild(rootElement);

        //each of these elements can only have zero or one values
        for(String field : singleEntryFields)
        {
            org.w3c.dom.Element element = doc.createElement(field);
            element.appendChild(doc.createTextNode(singleEntryMap.get(field)));
            if(!(element.getTextContent().equals("") || element.equals(null)))
                rootElement.appendChild(element);
        }

        //each of these elements can have 0 or more values
        for(String field : multiEntryFields)
        {
            org.w3c.dom.Element element = doc.createElement(field);
            for(String entry : multiEntryMap.get(field)) {
                org.w3c.dom.Element value = doc.createElement("value");
                value.appendChild(doc.createTextNode(entry));
                element.appendChild(value);
            }
            if(element.hasChildNodes())
                rootElement.appendChild(element);
        }

        //creating the elements for the files
        org.w3c.dom.Element files = doc.createElement("files");
        int numOfFiles = fileNames.size();
        for(int i = 0; i < numOfFiles; i++)
        {
            org.w3c.dom.Element file = doc.createElement("file");

            org.w3c.dom.Element fileName = doc.createElement("fileName");
            fileName.appendChild(doc.createTextNode(fileNames.get(i)));
            file.appendChild(fileName);
            org.w3c.dom.Element fileLink = doc.createElement("fileLink");
            fileLink.appendChild(doc.createTextNode(fileLinks.get(i)));
            file.appendChild(fileLink);
            org.w3c.dom.Element fileDescription = doc.createElement("fileDescription");
            fileDescription.appendChild(doc.createTextNode(fileDescriptions.get(i)));
            if(!fileDescription.getTextContent().equals("")) file.appendChild(fileDescription);

            files.appendChild(file);
        }
        rootElement.appendChild(files);

        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File("downloads/" + postID + "/Post" + postID + ".xml"));

        transformer.transform(source, result);
    }

    public void downloadFiles()
    {
        if (verboseOutput) log.info(message.DownloadingFiles());

        /**Download file(s) to working directory. The files are stored on the computer locally before the upload.**/
        URL url = null;
        int i = 0;
        //Navigate to each file and download
        for (String link : fileLinks) {
            //navigate to link
            try {
                url = new URL("http://daln.osu.edu"+link);
            } catch (MalformedURLException e) {
                if (verboseOutput) log.error(message.FileLinkInvalid());
                else log.error(message.PostImportErrorLog(postID));
                //System.exit(1);
                //e.printStackTrace();
            }

            //download with specified target and name
            try {
                FileUtils.copyURLToFile(url, new File("downloads/" + postID + "/" + fileNames.get(i)));
            } catch (IOException e) {
                log.error("Problem downloading file.");
                e.printStackTrace();
            }
            i++;

        }
        if (verboseOutput) log.info(message.PostImportCompleteVerbose(postID));
        else log.info(message.PostImportCompleteLog(postID));
    }
}
