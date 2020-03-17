package com.gxx.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;

import java.io.File;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        HttpDownFileUtils.getInstance().downFileFromServiceToPublicDir("xxxxx", this, DIRECTORY_DOWNLOADS, new OnFileDownListener() {
            @Override
            public void onFileDownStatus(int status, Object object, int proGress, long currentDownProGress, long totalProGress) {
                if (status == 1){
                    if (object instanceof File){
                        File file = (File) object;
                    }else if (object instanceof Uri){
                        Uri uri = (Uri) object;
                    }
                }
            }
        });
    }
}
