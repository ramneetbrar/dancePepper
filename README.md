
# Dance Pepper

Dance pepper is a Human-Computer Interaction project. It was built to mimic human movements. 


##  Project Structure
```
├── Android
│   ├── app
│   │   ├── build.gradle
│   │   ├── proguard-rules.pro
│   │   └── src
│   │       ├── androidTest
│   │       │   └── ...
│   │       ├── main
│   │       │   ├── AndroidManifest.xml
│   │       │   ├── assets
│   │       │   │   └── robot
│   │       │   │       └── robotsdk.xml
│   │       │   ├── java
│   │       │   │   └── com
│   │       │   │       └── ramneet
│   │       │   │           └── dancepepper
│   │       │   │               ├── FileHandler.java
│   │       │   │               ├── FileUploadService.java
│   │       │   │               ├── MainActivity.java
│   │       │   │               ├── ServiceGenerator.java
│   │       │   │               └── animationExecutor.java
│   │       │   └── res
│   │       │       ├── ..
│   │       └── test
│   │           └── ...
│   ├── build.gradle
│   ├── gradle
│   │   └── wrapper
│   │       ├── gradle-wrapper.jar
│   │       └── gradle-wrapper.properties
│   ├── gradle.properties
│   ├── gradlew
│   ├── gradlew.bat
│   └── settings.gradle
├── Augmentation
│   ├── 3pg2gif.py
│   ├── aug_script_2.ipynb
│   ├── gif2mp4.py
│   ├── upload.py
│   └── vidaug
│       ├── __init__.py
│       └── augmentors
│           ├── __init__.py
│           ├── affine.py
│           ├── crop.py
│           ├── flip.py
│           ├── geometric.py
│           ├── group.py
│           ├── intensity.py
│           └── temporal.py
└── Server
    ├── Classifier
    │   ├── Model___Date_Time_2022_04_10__16_38_28___Loss_0.4024992287158966___Accuracy_0.9200000166893005.h5
    │   ├── Pipfile
    │   ├── classifier.ipynb
    │   ├── pepper_data
    │   │   ├── boogie
    │   │   │   ├── ...
    │   │   ├── clap
    │   │   │   ├── ...
    │   │   ├── kisses
    │   │   │   ├── ...
    │   │   ├── mind_blown
    │   │   │   ├── ...
    │   │   └── wave
    │   │       ├── ...
    │   ├── requirements.txt
    ├── Pipfile
    ├── app.py
    ├── static
    │   └── uploads
    └── templates
        └── upload.html
```

### Description

The Android folder contains our pepper app. To run pepper applications follow this [guide](https://developer.softbankrobotics.com/pepper-qisdk/getting-started
). Then navigate to the FileHandler.java which can be found by the following path Android/app/src/main/java/com/ramneet/dancepepper/FileHandler.java. Then navigate to line 9 and you should see the following:
```
    private static final String BASE_URL = "http://10.0.0.5:5000/";
```
Replace http://10.0.0.5:5000/ with ip address of the machine that is running the server.

The Server folder is our server it receives post requests of video files from our pepper app and uses the model to generate a prediction. The prediction is contained in the response to the post request. To run the server use the following command with Server folder as root. Note that your_ip is the machine running the server's ip address and needs to match the one you changed in the previous step:
```
 flask run -h your_ip
```
Videos sent to the server will be in the uploads' folder Server/static/uploads

The Classifier folder contains the Juypiter Notebook that creates the model. Move the pepper_data to the classifier folder as shown in the project structure

The Augmentation folder contains the script to augment the data. Run it by asking peter looool
## Self Evaluation

We originally wanted to make pepper animate any movement that the user could make, specifically dance moves. This turned out to be really hard, so we changed strategies and decided on making a set of 5 actions that pepper could recognize and reproduce. We wanted to create our own exciting Pepper animations, but we could not make our own due to troubles with the Pepper animation editor. The animation editor would not launch on our macs. So we adapted and chose from a list of predefined animations. We then wanted to use OpenPose on the Pepper tablet and found a similar library suited for mobile development called Tensorflow light that provides similar data we could use for our model. Unfortunately, Tensorflow light would only compile on our virtual machines and would not run on the Pepper tablet. We then opted to create a server that could receive a video file and use a model to classify the video. We now tried to use the actual OpenPose library, but we were not able to use it due to compilation errors. Moving on, we found an article describing video classification and used this to build our model. After making our model, we found it inaccurate and needed to improve it. The data was initially recorded on MacBook cameras, but it was too dissimilar to the videos recorded by the pepper tablet. Videos recorded on the Pepper tablet look upwards and have a narrow frame of view. So we then re-recorded the videos using the Pepper tablet, utilizing our app and server to download the videos. We then reduced the number of actions to 3: wave, mind blown, and blow kisses. This was to help speed up the training process and model accuracy.

Overcoming the many challenges involved in creating and using AI and robot libraries, we met our **new** goal of classifying three human actions and mimicking them back to the user. 
## Acknowledgements

 - [Server Implementatoin](https://roytuts.com/upload-and-play-video-using-flask/) we followed 
 - [Classifier Implementatoin](https://learnopencv.com/introduction-to-video-classification-and-human-activity-recognition/) we followed


## Dependencies

### Server
Python 3.9.5

tensorflow==2.4.1
numpy==1.18.5
opencv-contrib-python==4.1.2.30
sklearn==0.22.2.post1
matplotlib==3.2.2
urllib3==1.24.3
pafy==0.5.5
moviepy==0.2.3.5
flask==2.2.1
werkzeug==2.1.1
