package com.gxx.myapplication;

import android.content.Intent;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;

import static android.os.Environment.DIRECTORY_PICTURES;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PermissonsUtil permissonsUtil = new PermissonsUtil();
        permissonsUtil.requestPermisson(this,permissonsUtil.getAppNedPermissonsArray(),null);
        this.findViewById(R.id.bt_to_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                down();
            }
        });
    }

    public void down(){
        //https://static.runoob.com/images/demo/demo2.jpg
        HttpDownFileUtils.getInstance().downFileFromServiceToPublicDir("https://static.runoob.com/images/demo/demo2.jpg", this, DIRECTORY_PICTURES, new OnFileDownListener() {
            @Override
            public void onFileDownStatus(int status, Object object, int proGress, long currentDownProGress, long totalProGress) {
                if (status == 1){
                    Toast.makeText(MainActivity.this,"下载成功",Toast.LENGTH_SHORT).show();
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
