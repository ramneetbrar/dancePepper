package com.ramneet.dancepepper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.ChatBuilder;
import com.aldebaran.qi.sdk.builder.ListenBuilder;
import com.aldebaran.qi.sdk.builder.PhraseSetBuilder;
import com.aldebaran.qi.sdk.builder.QiChatbotBuilder;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.builder.TopicBuilder;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.object.conversation.Chat;
import com.aldebaran.qi.sdk.object.conversation.Listen;
import com.aldebaran.qi.sdk.object.conversation.ListenResult;
import com.aldebaran.qi.sdk.object.conversation.PhraseSet;
import com.aldebaran.qi.sdk.object.conversation.QiChatbot;
import com.aldebaran.qi.sdk.object.conversation.Say;
import com.aldebaran.qi.sdk.object.conversation.Topic;
import com.aldebaran.qi.sdk.util.PhraseSetUtil;

public class MainActivity extends RobotActivity implements RobotLifecycleCallbacks {
    private static final String TAG = "MainActivity";
    private Chat chat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        // Create a topic
        Topic topic = TopicBuilder.with(qiContext)
                                .withResource(R.raw.greetings)
                                .build();

        // Create a QiChatbot
        QiChatbot qiChatbot = QiChatbotBuilder.with(qiContext)
                                            .withTopic(topic) // use the previously created topic
                                            .build();

        // Create a Chat action
        chat = ChatBuilder.with(qiContext)
                        .withChatbot(qiChatbot)
                        .build();
        chat.addOnStartedListener(() -> Log.i(TAG, "Discussion started."));

        // Run the Chat action asynchronously.
        Future<Void> chatFuture = chat.async().run();
        chatFuture.thenConsume(future -> {
            if (future.hasError()) {
                Log.e(TAG, "Discussion finished with error.", future.getError());
            }
        });

        // Create a new say action.
        Say say = SayBuilder.with(qiContext)
                .withText("Want to dance?")
                .build();
        say.run();


        // Create the PhraseSet for yes and no.
        PhraseSet phraseSetYes = PhraseSetBuilder.with(qiContext)
                .withTexts("yes", "yeah", "OK", "alright", "let's do this") // Add the phrases Pepper will listen to.
                .build();

        PhraseSet phraseSetNo = PhraseSetBuilder.with(qiContext)
                .withTexts("no", "nah", "Sorry", "I can't")
                .build();

        // Create a new listen action.
        Listen listen = ListenBuilder.with(qiContext)
                .withPhraseSets(phraseSetYes, phraseSetNo) // Set the PhraseSets to listen to.
                .build();

        // Run the listen action and get the result.
        ListenResult listenResult = listen.run();
        Log.i(TAG, "Heard phrase: " + listenResult.getHeardPhrase().getText());
        // Identify the matched phrase set.
        PhraseSet matchedPhraseSet = listenResult.getMatchedPhraseSet();
        if (PhraseSetUtil.equals(matchedPhraseSet, phraseSetYes)) {
            Log.i(TAG, "Heard phrase set: yes");

            say = SayBuilder.with(qiContext)
                    .withText("Okay, show me your moves.")
                    .build();
            say.run();
            MediaPlayer mediaPlayer = MediaPlayer.create(qiContext, R.raw.ladyfingers);
            mediaPlayer.start();

            //maybe prompt to user to press record tablet or find way to launch strait into recording
            recordVideo();

        } else if (PhraseSetUtil.equals(matchedPhraseSet, phraseSetNo)) {
            Log.i(TAG, "Heard phrase set: no");
            say = SayBuilder.with(qiContext)
                    .withText("Your loss.")
                    .build();
            say.run();
        }
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

    private void recordVideo() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        startActivityForResult(intent, VIDEO_RECORD_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VIDEO_RECORD_CODE)
            if (resultCode == RESULT_OK) {
                videoPath = data.getData();
                Log.i("VIDEO_RECORD_TAG", "Video is recording and saved at " + videoPath);
            } else if (resultCode == RESULT_CANCELED) {
                Log.i("VIDEO_RECORD_TAG", "Recording canceled");
            } else {
                Log.i("VIDEO_RECORD_TAG", "Recording has error");

            }
    }
}
