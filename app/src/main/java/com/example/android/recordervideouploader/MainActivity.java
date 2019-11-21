package com.example.android.recordervideouploader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQuery;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final int SELECT_VIDEO = 3;

    VideoView videoView;
    private SQLiteDatabase database;
    Uri videoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoView = (VideoView) findViewById(R.id.videoView);

        Button rButton = (Button) findViewById(R.id.record);
        rButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakeVideoIntent();
            }
        });
        SQLiteDatabase.CursorFactory factory = new SQLiteDatabase.CursorFactory() {
            @Override
            public Cursor newCursor(SQLiteDatabase sqLiteDatabase, SQLiteCursorDriver sqLiteCursorDriver, String s, SQLiteQuery sqLiteQuery) {
                return null;
            }
        };

        SQLiteOpenHelper dbHelper = new DataBase(this, DataBase.DbName, factory, DataBase.VERSION);
        database = dbHelper.getWritableDatabase();


        Button bplay = (Button) findViewById(R.id.playVideos);
        bplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                videoView.setVideoURI(videoUri);
                videoView.start();

            }
        });

        Button uploadButton = (Button) findViewById(R.id.uploadRecording);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("video/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select a Video "), SELECT_VIDEO);
            }
        });


    }
    Uri selectedVideoUri=null;
    static final int REQUEST_VIDEO_CAPTURE = 1;

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }

    //
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            videoUri = intent.getData();
            //         videoView.setVideoURI(videoUri);
            String uriSave = videoUri.toString();
            String insertQuery = "INSERT INTO VideoData(uri) VALUES ('" + videoUri.toString() + "')";
            database.execSQL(insertQuery);
            TextView tv = (TextView) findViewById(R.id.uri);
            tv.setText(videoUri.toString());
            if (requestCode == SELECT_VIDEO && resultCode == RESULT_OK) {

                UploadAsyncTask ipt = new UploadAsyncTask();
                ipt.execute();
                 selectedVideoUri = intent.getData();


            }

        }
    }


    class DataBase extends SQLiteOpenHelper {

        public static final String DbName = "VideoDB";

        public static final String createTable = "CREATE TABLE VideoData(\n " +
                " id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "uri TEXT,\n" +
                "isUploaded INTEGER DEFAULT 0\n" +
                ")";

        public static final int VERSION = 1;

        public DataBase(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        /*
        creates table to store uri and uploaded status.
         */
        @Override
        public void onCreate(SQLiteDatabase sdb) {
            sdb.execSQL(createTable);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

        }
    }

    private class UploadAsyncTask extends AsyncTask<URL, Void, Integer> {

        @Override
        protected Integer doInBackground(URL... urls) {
            // Create URL object

            String path = selectedVideoUri.getPath();
            upLoad2Server(path);
            Integer response=0;
            // Perform HTTP request to the URL and receive a JSON response back
            String jsonResponse = "";

             response = upLoad2Server(path);


            return response;
        }
    }

    public static int upLoad2Server(String sourceFileUri) {
        String upLoadServerUri = "your remote server link";
        // String [] string = sourceFileUri;
        String fileName = sourceFileUri;

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        DataInputStream inStream = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        String responseFromServer = "";
        int serverResponseCode=00;
        File sourceFile = new File(sourceFileUri);
        if (!sourceFile.isFile()) {
            Log.e("Huzza", "Source File Does not exist");
            return 0;
        }
        try { // open a URL connection to the Servlet
            FileInputStream fileInputStream = new FileInputStream(sourceFile);
            URL url = new URL(upLoadServerUri);
            conn = (HttpURLConnection) url.openConnection(); // Open a HTTP  connection to  the URL
            conn.setDoInput(true); // Allow Inputs
            conn.setDoOutput(true); // Allow Outputs
            conn.setUseCaches(false); // Don't use a Cached Copy
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("ENCTYPE", "multipart/form-data");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            conn.setRequestProperty("uploaded_file", fileName);
            dos = new DataOutputStream(conn.getOutputStream());

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\"" + fileName + "\"" + lineEnd);
            dos.writeBytes(lineEnd);

            bytesAvailable = fileInputStream.available(); // create a buffer of  maximum size
            Log.i("Huzza", "Initial .available : " + bytesAvailable);

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

            Log.i("Upload file to server", "HTTP Response is : " + serverResponseMessage + ": " + serverResponseCode);
            // close streams
            Log.i("Upload file to server", fileName + " File is written");
            fileInputStream.close();
            dos.flush();
            dos.close();
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
        } catch (Exception e) {
            e.printStackTrace();
        }
//this block will give the response of upload link
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn
                    .getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                Log.i("Huzza", "RES Message: " + line);
            }
            rd.close();
        } catch (IOException ioex) {
            Log.e("Huzza", "error: " + ioex.getMessage(), ioex);
        }
        return serverResponseCode;  // like 200 (Ok)


    }
}