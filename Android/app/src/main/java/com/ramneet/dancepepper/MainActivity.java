package com.ramneet.dancepepper;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.AnimateBuilder;
import com.aldebaran.qi.sdk.builder.AnimationBuilder;
import com.aldebaran.qi.sdk.builder.ChatBuilder;
import com.aldebaran.qi.sdk.builder.QiChatbotBuilder;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.builder.TopicBuilder;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.object.actuation.Animate;
import com.aldebaran.qi.sdk.object.actuation.Animation;
import com.aldebaran.qi.sdk.object.conversation.BaseQiChatExecutor;
import com.aldebaran.qi.sdk.object.conversation.Chat;
import com.aldebaran.qi.sdk.object.conversation.Chatbot;
import com.aldebaran.qi.sdk.object.conversation.Phrase;
import com.aldebaran.qi.sdk.object.conversation.QiChatExecutor;
import com.aldebaran.qi.sdk.object.conversation.QiChatVariable;
import com.aldebaran.qi.sdk.object.conversation.QiChatbot;
import com.aldebaran.qi.sdk.object.conversation.Say;
import com.aldebaran.qi.sdk.object.conversation.Topic;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;


public class MainActivity extends RobotActivity implements RobotLifecycleCallbacks {
    private static final String TAG = "MainActivity";
    private FileHandler fileHandler = new FileHandler();
    private Chat chat;
    private QiChatbot qiChatbot;
    private QiChatVariable variable;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private String gPredictionStr = "";
    private QiContext qiContextG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int permission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Register the RobotLifecycleCallbacks to this Activity.
        QiSDK.register(this, this);

        if (isCameraPresent()) {
            Log.i("VIDEO_RECORD_TAG", "Camera is detected");
            getCameraPermission();
        } else {
            Log.i("VIDEO_RECORD_TAG", "Camera is NOT detected");

        }
    }

    @Override
    protected void onDestroy() {
        // Unregister the RobotLifecycleCallbacks for this Activity.
        QiSDK.unregister(this, this);
        super.onDestroy();
    }

    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        // The robot focus is gained.
        qiContextG = qiContext;
        // Create a topic
        Topic topic = TopicBuilder.with(qiContext)
                .withResource(R.raw.greetings)
                .build();

        // Create a QiChatbot
        qiChatbot = QiChatbotBuilder.with(qiContext)
                .withTopic(topic) // use the previously created topic
                .build();

        Map<String, QiChatExecutor> executors = new HashMap<>();
//        executors.put("animateExecuter", new animationExecutor(qiContext, "animate"));
        executors.put("recordVideo", new recordVideoExecutor(qiContext));
        executors.put("discoExecuter", new animationExecutor(qiContext, "disco"));
        executors.put("guitarExecuter", new animationExecutor(qiContext, "guitar"));
        executors.put("drumrollExecuter", new animationExecutor(qiContext, "drumroll"));
        executors.put("danceExecuter", new animationExecutor(qiContext, "dance"));

        qiChatbot.setExecutors(executors);
        List<Chatbot> chatbots = new ArrayList<>();
        chatbots.add(qiChatbot);

        // Create a Chat action
        chat = ChatBuilder.with(qiContext)
                        .withChatbot(qiChatbot)
                        .build();
        chat.addOnStartedListener(() -> {
            Log.i(TAG, "Discussion started.");
            Log.i(TAG, "getSaying: " + chat.getSaying());
        });

        // Run the Chat action asynchronously.
        Future<Void> chatFuture = chat.async().run();
        chatFuture.thenConsume(future -> {
            if (future.hasError()) {
                Log.e(TAG, "Discussion finished with error.", future.getError());
            } else {
                Log.i(TAG, "Discussion finished successfully");
            }
        });
    }

    @Override
    public void onRobotFocusLost() {
        // The robot focus is lost.
        if (chat != null) {
            chat.removeAllOnStartedListeners();
        }
    }

    @Override
    public void onRobotFocusRefused(String reason) {
        // The robot focus is refused.
    }

    /*
    The camera activity code including getting permissions was take from this tutorial:
    https://www.youtube.com/watch?v=_igp9Apumvg
 */
    private static int CAMERA_PERMISSION_CODE = 100;
    private static int VIDEO_RECORD_CODE = 101;
    private Uri videoPath;
    private String videoPathString = "";

    public void recordVideoButtonPressed(View view) {
        recordVideo();
    }

    private boolean isCameraPresent() {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            return true;
        } else {
            return false;
        }
    }

    private void getCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
    }

    // TODO: Figure out how to pass context through intent stuff all the way to animate call (onActivityResult)
    private void recordVideo() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 4);
        startActivityForResult(intent, VIDEO_RECORD_CODE);
    }

    public class recordVideoExecutor extends BaseQiChatExecutor {
        private QiContext qiContext;
        protected recordVideoExecutor(QiContext context) {
            super(context);
            this.qiContext = context;
        }

        @Override
        public void runWith(List<String> params) {
            recordVideo();
            Log.i("RecordVideoExecutor", "back from record video.");
            animationExecutor anim = new animationExecutor(qiContext, "animate");
            anim.run(gPredictionStr);
            Log.i("RecordVideoExecutor", "after executing animation.");
        }

        @Override
        public void stop() {
            Log.i("RecordVideoExecutor", "QiChatExecutor stopped");
        }
    }

    interface PredictionCallback {
        void success(String prediction, String probability);
        void failure(Throwable t);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VIDEO_RECORD_CODE)
            if (resultCode == RESULT_OK) {
                videoPath = data.getData();
                String mediaType = getContentResolver().getType(videoPath);
                Log.i("VIDEO_RECORD_TAG", "Video is recording and saved at " + videoPath);
                File[] videoFiles = fileHandler.retrieveFilesFromDevice();
                if(videoFiles.length == 0){
                    Log.e("RetrieveVideo", "No videos could be found on the device.");
                } else {
                    File mostRecentVideo = videoFiles[videoFiles.length - 1];
//                    Tried making it say thinking
//                    Thread thinkingThread = new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            try  {
//                                //Your code goes here
//                                Log.i("Thinking", "before say");
//                                Phrase phrase = new Phrase("Let me think ...");
//                                Say say = SayBuilder.with(qiContextG)
//                                        .withPhrase(phrase)
//                                        .build();
//
//                                say.run();
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    });
//
//                    thinkingThread.start();
                    fileHandler.uploadFile(mostRecentVideo, mediaType, new PredictionCallback() {
                        @Override
                        public void success(String prediction, String probability) {
                            // Use result
                            Log.i("BackFromCamera", "About to animate prediction: " + prediction + " with probability: " + probability);
                            gPredictionStr = prediction;
                            Log.i("BackFromCamera", "updated animation");
                            animationExecutor anim = new animationExecutor(qiContextG, prediction);
                            anim.run(prediction);
                            Log.i("BackFromCamera", "after executing animation");
                        }

                        @Override
                        public void failure(Throwable t) {
                            // Display error
                            Log.i("BackFromCamera", "failed");
                        }

                    });
                }
            } else if (resultCode == RESULT_CANCELED) {
                Log.i("VIDEO_RECORD_TAG", "Recording canceled");
            } else {
                Log.i("VIDEO_RECORD_TAG", "Recording has error");

            }

    }
}
