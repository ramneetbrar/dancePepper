package com.ramneet.dancepepper;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FileHandler {

    public FileHandler() {
    }

    protected File[] retrieveFilesFromDevice(){
        Log.i("RetrieveVideo", "Attempting to locate video files on the device.");
        String apparentRootPath = Environment.getExternalStorageDirectory().getPath();
        Log.i("RetrieveVideo", "The root storage folder of the device is:");
        String apparentLocationOfFiles;
        File [] discoveredFiles;
        File rootDirectory = Environment.getExternalStorageDirectory();

        File[] foldersNamedDCIM = rootDirectory.listFiles(namedDCIM);
        Log.i("RetrieveVideo", "Discovered the following files matching name 'DCIM':");
        for (File file : foldersNamedDCIM){
            Log.i("RetrieveVideo", "Filename:" + file.getName());
        }

        if(containsOneFolder(foldersNamedDCIM)){
            File DCIMFolder = foldersNamedDCIM[0];
            File[] foldersNamedCamera = DCIMFolder.listFiles(namedCamera);
            Log.i("RetrieveVideo", "Discovered the following files matching name 'Camera':");
            for (File file : foldersNamedCamera){
                Log.i("RetrieveVideo", "Filename:" + file.getName());
            }
            if(containsOneFolder(foldersNamedCamera)){
                File cameraFolder = foldersNamedCamera[0];
                apparentLocationOfFiles = cameraFolder.getPath();
                discoveredFiles = cameraFolder.listFiles();
                Log.i("RetrieveVideo", "Discovered the following files:");
                for (File file : discoveredFiles){
                    Log.i("RetrieveVideo", "Filename:" + file.getName());
                }
                Log.i("RetrieveVideo", "The external storage directory was reported as: \"" + apparentLocationOfFiles + "\"");
                Log.i("RetrieveVideo", "\"" + apparentLocationOfFiles + "\" contained " + discoveredFiles.length + " files.");
                return discoveredFiles;
            } else {
                Log.e("RetrieveVideo", "No 'Camera' folder was able to be located under the root path \"" + apparentRootPath + "/DCIM/\"");
                return null;

            }
        } else {
            Log.e("RetrieveVideo", "No 'DCIM' folder was able to be located under the root path \"" + apparentRootPath + "\"");
            return null;
        }

    }


    protected String getFileNameFromPath(String path){
        String fileName = path;
        return fileName;
    }

    protected String getPathWithoutFileNameFromPath(String path){
        String basePath = path;
        return basePath;
    }

    //From tutorial here: https://futurestud.io/tutorials/retrofit-2-how-to-upload-files-to-server
    //And here: https://futurestud.io/tutorials/retrofit-2-creating-a-sustainable-android-client
    public void uploadFile(File file, String type){
        FileUploadService service = ServiceGenerator.createService(FileUploadService.class);
        RequestBody requestFile = RequestBody.create(MediaType.parse(type), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("Video File", file.getName(), requestFile);
        String descriptionStr = "A video from the tablet camera";
        RequestBody description = RequestBody.create(MultipartBody.FORM, descriptionStr);
        Call<ResponseBody> call = service.upload(description, body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.v("UploadFile", "File successfully uploaded");
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("UploadTile", t.getMessage());
            }
        });
    }

    private static boolean containsOneFolder(File[] folder){
        return folder.length == 1;
    }

    FilenameFilter namedDCIM = new FilenameFilter() {
        @Override
        public boolean accept(File file, String s) {
            return s.equals("DCIM");
        }
    };
    FilenameFilter namedCamera = new FilenameFilter() {
        @Override
        public boolean accept(File file, String s) {
            return s.equals("Camera");
        }
    };
}