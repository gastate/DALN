# The DALN and Application Introduction

The [DALN](http://daln.osu.edu/), a joint project with The Ohio State University and Georgia State University, is a public archive of literary narratives. It invites people of all kinds to submit a narrative about their personal experiences about learning how to read, write, or teach. Users are able submit their narratives in many different formats, such as videos, audio recordings, images, text files, and more. 

Currently, the website hosts all of these narratives. Each narrative, along with the file or files that each narrative contains, is considered a single post with a unique identifier. This application downloads a post to your local computer and examines the files contained within the post. It will then upload the files located on your computer and store the information in a database.

# Resources Utilized

This application utilizes three services to handle the different types of files. SproutVideo is used to upload video files, SoundCloud is used to upload audio files, and Amazon S3 is used to store all other files as well as metadata about the post. The application stores the information in Amazon DynamoDB, a NoSQL database. API keys must be provided in a config.properties file (template included in the program) for access to SproutVideo, SoundCloud, and AWS Services.

# How the Program Runs
The program takes in command-line parameters in the following format:

`java Tester postId [download|upload|full] [verbose|log]`

## Inputs
**postId:** The unique identifier for a post located on the DALN website. For example, for the page http://daln.osu.edu/handle/2374.DALN/885, 885 would be the post ID. A post ID must be given.
 
### Post Handling Options
One of the following three options may be given. If omitted, the default option is **full**.

**download:** The program will only download the specified post.

**upload:** The program will only upload the specified post.

**full:** The program will first download the specified post, then upload.

### Output Options
One of the following two options may be given. If omitted, the default option is **verbose**.

**verbose**: The program will output descriptive messages during the download and upload processes.

**log**: The program will output log-based messages, displaying the success/error of a post or asset download or upload.

This application utilizes log4j. A log of the program's output will be saved in a text file located in the directory. Every log contains a time stamp, a log message, and the class and line number which it came from.

# Program Maintenance
Maintenance for this program will mainly include the services that are used. The downloaded locations of the files (URLs) are inserted into the database for every file. However, a service may change the way a file (that they host) is pointed to. The file locations already listed in the database must then be changed, and the method for storing new file locations must be updated. Also, there is a possibility that space limitations may be met for the different services used, although unlikely. If this problem occurs, then that specific service plan must be upgraded or an alternative solution must be found.
