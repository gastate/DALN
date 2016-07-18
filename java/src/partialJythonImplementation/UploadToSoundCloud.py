import soundcloud
import os
import UploadToSoundCloudInterface
import requests.packages.urllib3
import certifi
requests.packages.urllib3.disable_warnings()

client = soundcloud.Client(client_id='34f3a0291f39c81b9fe952a0a0508ed6',
                                   client_secret='ea8e7de9b471b03de72f0276c2b87993',
                                   username='rahmed8@gsu.edu',
                                   password='shakibsoundcloud')
print client.get('/me').username

class UploadToSoundCloud(UploadToSoundCloudInterface):
    def upload(self, postDetails):
        print("SC upload function")

        fileName = postDetails["Current File"]
        dalnID = postDetails["DalnId"]
        assetID = postDetails["Current Asset ID"]

        print(dalnID)
        print(fileName)
        print(assetID)

        path = os.path.abspath('downloads/'+dalnID+'/'+fileName)
        print(path)
     # upload audio file
        track = client.post('/tracks',track={
            'title': 'This is my sound',
            'asset_data': open(path, 'rb')
            })

    def getSoundCloudURL(self):
        return "SC URL:"+track.permalink_url
