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
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;

public class MainActivity extends AppCompatActivity {

    VideoView videoView;
    private SQLiteDatabase database;
    Uri videoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoView  =(VideoView)findViewById(R.id.videoView);

        Button rButton = (Button)findViewById(R.id.record);
        rButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakeVideoIntent();
            }
        });
        SQLiteDatabase.CursorFactory factory = new SQLiteDatabase.CursorFactory(){
            @Override
            public Cursor newCursor(SQLiteDatabase sqLiteDatabase, SQLiteCursorDriver sqLiteCursorDriver,String s, SQLiteQuery sqLiteQuery){
                return null;
            }
        };

        SQLiteOpenHelper dbHelper = new DataBase(this,DataBase.DbName,factory,DataBase.VERSION);
        database = dbHelper.getWritableDatabase();


        Button bplay = (Button)findViewById(R.id.playVideos);
        bplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                videoView.setVideoURI(videoUri);
                videoView.start();

            }
        });
    }

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
        super.onActivityResult(requestCode,resultCode,intent);
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            videoUri = intent.getData();
   //         videoView.setVideoURI(videoUri);
            String uriSave=videoUri.toString();
            String insertQuery="INSERT INTO VideoData(uri) VALUES ('" + videoUri.toString() + "')";
            database.execSQL(insertQuery);
            TextView tv =(TextView)findViewById(R.id.uri);
            tv.setText(videoUri.toString());
        }
    }


    class DataBase extends SQLiteOpenHelper {

        public static final String DbName="VideoDB";

        public static final  String createTable="CREATE TABLE VideoData(\n " +
                " id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "uri TEXT,\n" +
                "isUploaded INTEGER DEFAULT 0\n" +
                ")";

        public static final int VERSION=1;
        public DataBase (Context context,String name, SQLiteDatabase.CursorFactory factory, int version){
            super(context,name,factory,version);
        }
        /*
        creates table to store uri and uploaded status.
         */
        @Override
        public void onCreate(SQLiteDatabase sdb){
            sdb.execSQL(createTable);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1){

        }
    }
}
