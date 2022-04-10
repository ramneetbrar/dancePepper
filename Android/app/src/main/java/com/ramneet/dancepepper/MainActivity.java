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
import com.aldebaran.qi.sdk.builder.TopicBuilder;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.object.actuation.Animate;
import com.aldebaran.qi.sdk.object.actuation.Animation;
import com.aldebaran.qi.sdk.object.conversation.BaseQiChatExecutor;
import com.aldebaran.qi.sdk.object.conversation.Chat;
import com.aldebaran.qi.sdk.object.conversation.Chatbot;
import com.aldebaran.qi.sdk.object.conversation.QiChatExecutor;
import com.aldebaran.qi.sdk.object.conversation.QiChatVariable;
import com.aldebaran.qi.sdk.object.conversation.QiChatbot;
import com.aldebaran.qi.sdk.object.conversation.Topic;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
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
                //String pathStr = Environment.getExternalStorageDirectory().getPath();
                File[] videoFiles = fileHandler.retrieveFilesFromDevice();
                if(videoFiles.length == 0){
                    Log.e("RetrieveVideo", "No videos could be found on the device.");
                } else {
                    File mostRecentVideo = videoFiles[videoFiles.length - 1];
                    //path = mostRecentVideo.getPath();
//                    fileHandler.uploadFile(mostRecentVideo, mediaType);

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
                //String path = "/mnt/sdcard/Movies/VID_20220406_193443.mp4";
               // String path = "/storage/emulated/0/DCIM/Camera/VID_20220406_195135.mp4";
               // Log.i("UploadFile", "External storage directory:" + dir);
                //String path = videoPath.toString();
                //uploadFile(path);
            } else if (resultCode == RESULT_CANCELED) {
                Log.i("VIDEO_RECORD_TAG", "Recording canceled");
            } else {
                Log.i("VIDEO_RECORD_TAG", "Recording has error");

            }
    }

    public int uploadFile(String sourceFileUri) {
        Log.e("uploadFile", "Full string:" + sourceFileUri);
        int lastSlashIndex = sourceFileUri.lastIndexOf("/");
        //Log.i("uploadFile", "Last index:" + lastSlashIndex);
        String uploadFileName = sourceFileUri.substring(lastSlashIndex + 1);
        String uploadFilePath = sourceFileUri.substring(0, lastSlashIndex);
        Log.i("uploadFile", "Filename:" + uploadFileName);
        Log.i("uploadFile", "Filename:" + uploadFilePath);

        int serverResponseCode = 0;
        String upLoadServerUri = "http://207.23.170.43:5000/";
        String fileName = sourceFileUri;

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(sourceFileUri);

        Log.e("uploadFile", "File name: " + sourceFile.getName());
        Log.e("uploadFile", "File String:" + sourceFile);
        Log.e("uploadFile", "File status:" + sourceFile.isFile());
        if (!sourceFile.isFile()) {

            //dialog.dismiss();

            Log.e("uploadFile", "Source File does not exist:"
                    + uploadFilePath + "" + uploadFileName);

            runOnUiThread(new Runnable() {
                public void run() {
                    //messageText.setText("Source File not exist :"
                           // + uploadFilePath + "" + uploadFileName);
                }
            });

            return 0;

        } else {
            try {

                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(upLoadServerUri);

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", fileName);
                System.out.println("Built request...");
                dos = new DataOutputStream(conn.getOutputStream());
                System.out.println("Created output stream...");

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\"" + fileName + "\"" + lineEnd);
                dos.writeBytes(lineEnd);
                System.out.println("Wrote bytes...");

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();
                System.out.println("Creating buffer...");
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {

                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);

                if (serverResponseCode == 200) {

                    runOnUiThread(new Runnable() {
                        public void run() {

                            String msg = "File Upload Completed.\n\n See uploaded file here : \n\n"
                                    + " http://www.androidexample.com/media/uploads/"
                                    + uploadFileName;

                            //messageText.setText(msg);
                            Toast.makeText(MainActivity.this, "File Upload Complete.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {

                //dialog.dismiss();
                ex.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        //messageText.setText("MalformedURLException Exception : check script url.");
                        Toast.makeText(MainActivity.this, "MalformedURLException",
                                Toast.LENGTH_SHORT).show();
                    }
                });

                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {

                //dialog.dismiss();
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        //messageText.setText("Got Exception : see logcat ");
                        Toast.makeText(MainActivity.this, "Got Exception : see logcat ",
                                Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e("Upload file to server Exception", "Exception : "
                        + e.getMessage(), e);
            }
            //dialog.dismiss();
            return serverResponseCode;

        } // End else block
    }
}
