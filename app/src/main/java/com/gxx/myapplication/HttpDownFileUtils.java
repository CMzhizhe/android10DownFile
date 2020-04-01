package com.gxx.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import androidx.annotation.RequiresApi;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import static android.os.Environment.DIRECTORY_DCIM;
import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.DIRECTORY_MOVIES;
import static android.os.Environment.DIRECTORY_MUSIC;
import static android.os.Environment.DIRECTORY_PICTURES;

/**
 * @date: 2019/5/23 0023
 * @author: gaoxiaoxiong
 * @description:图片，视频下载工具
 **/
public class HttpDownFileUtils {
    private String TAG = HttpDownFileUtils.class.getSimpleName();
    private static HttpDownFileUtils downFileUtils;
    public static final int LOADING = 0;//加载中
    public static final int SUCCESS=1;
    public static final int FAIL=-1;

    public static HttpDownFileUtils getInstance() {
        if (downFileUtils == null) {
            synchronized (HttpDownFileUtils.class) {
                if (downFileUtils == null) {
                    downFileUtils = new HttpDownFileUtils();
                }
            }
        }
        return downFileUtils;
    }

    /**
     * @date :2020/1/16 0016
     * @author : gaoxiaoxiong
     * @description:下载到沙盒内部，路径自己指定目录
     * @param downPathUrl 下载的文件路径，需要包含后缀
     * @param localPath 下载到本地具体目录
     * @param isNeedDeleteOldFile 是否要删除老文件
     **/
    public void downFileFromServiceToLocalDir(String downPathUrl,final String localPath,final boolean isNeedDeleteOldFile,final OnFileDownListener onFileDownListener) {
        Observable.just(downPathUrl).subscribeOn(Schedulers.newThread()).map(new Function<String, File>() {
            @Override
            public File apply(String downPath)  {
                File file = null;
                try {
                    URL url = new URL(downPath);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(30 * 1000);
                    InputStream is = conn.getInputStream();
                    long time = System.currentTimeMillis();
                    int code = conn.getResponseCode();
                    String prefix = downPath.substring(downPath.lastIndexOf(".") + 1);
                    String fileName = null;
                    if (code == HttpURLConnection.HTTP_OK) {
                        fileName = conn.getHeaderField("Content-Disposition");
                        // 通过Content-Disposition获取文件名，这点跟服务器有关，需要灵活变通
                        if (fileName == null || fileName.length() < 1) {
                            // 通过截取URL来获取文件名
                            URL downloadUrl = conn.getURL(); // 获得实际下载文件的URL
                            fileName = downloadUrl.getFile();
                            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                        } else {
                            fileName = URLDecoder.decode(fileName.substring(
                                    fileName.indexOf("filename=") + 9), "UTF-8");
                            // 有些文件名会被包含在""里面，所以要去掉，不然无法读取文件后缀
                            fileName = fileName.replaceAll("\"", "");
                        }
                    }

                    if (isEmpty(fileName)) {
                        fileName = time + "." + prefix;
                    }

                    file = new File(localPath, fileName);

                    if (isNeedDeleteOldFile && file.exists()) {
                        file.delete();
                    }

                    if (!file.getParentFile().exists()){
                        file.getParentFile().mkdirs();
                    }

                    if (!file.exists()){
                        file.createNewFile();
                    }


                    FileOutputStream fos = new FileOutputStream(file);
                    BufferedInputStream bis = new BufferedInputStream(is);
                    byte[] buffer = new byte[1024];
                    int len;
                    int total = 0;
                    int contentLeng = conn.getContentLength();
                    while ((len = bis.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                        total += len;
                        if (onFileDownListener != null) {
                            onFileDownListener.onFileDownStatus(0, null, (total * 100 / contentLeng), total, contentLeng);
                        }
                    }
                    fos.close();
                    bis.close();
                    is.close();
                }catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return file;
            }
        })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<File>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(File file) {
                        if (file != null && onFileDownListener != null) {
                            onFileDownListener.onFileDownStatus(1, file, 0, 0, 0);
                        } else {
                            onFileDownListener.onFileDownStatus(-1, null, 0, 0, 0);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    /**
     * @date: 2019/5/23 0023
     * @author: gaoxiaoxiong
     * @description: 沙盒内部目录，是我们自己的APP目录下面
     * @param downPathUrl 下载的文件路径，需要包含后缀
     * @param isNeedDeleteOldFile 是否删除老项目
     * @param inserType  DIRECTORY_PICTURES  DIRECTORY_MOVIES  DIRECTORY_MUSIC
     * @param isDownCacleDir 是否下载到缓存目录 true 下载到缓存目录  false 不是下载到缓存目录
     **/
    public void downFileFromServiceToLocalDir(String downPathUrl,final String inserType,final boolean isNeedDeleteOldFile,final boolean isDownCacleDir,final OnFileDownListener onFileDownListener) {
        Observable.just(downPathUrl).subscribeOn(Schedulers.newThread()).map(new Function<String, File>() {
            @Override
            public File apply(String downPath)  {
                File file = null;
                try {
                    URL url = new URL(downPath);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(30 * 1000);
                    InputStream is = conn.getInputStream();
                    long time = System.currentTimeMillis();
                    int code = conn.getResponseCode();
                    String prefix = downPath.substring(downPath.lastIndexOf(".") + 1);
                    String fileName = null;
                    if (code == HttpURLConnection.HTTP_OK) {
                        fileName = conn.getHeaderField("Content-Disposition");
                        // 通过Content-Disposition获取文件名，这点跟服务器有关，需要灵活变通
                        if (fileName == null || fileName.length() < 1) {
                            // 通过截取URL来获取文件名
                            URL downloadUrl = conn.getURL(); // 获得实际下载文件的URL
                            fileName = downloadUrl.getFile();
                            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                        } else {
                            fileName = URLDecoder.decode(fileName.substring(
                                    fileName.indexOf("filename=") + 9), "UTF-8");
                            // 有些文件名会被包含在""里面，所以要去掉，不然无法读取文件后缀
                            fileName = fileName.replaceAll("\"", "");
                        }
                    }

                    if (isEmpty(fileName)) {
                        fileName = time + "." + prefix;
                    }


                    if(!isDownCacleDir){
                        if (inserType.equals(DIRECTORY_PICTURES)) {//图片
                            file = new File(FileSDCardUtil.getInstance().getPublickDiskImagePicDir(), fileName);
                        } else if (inserType.equals(DIRECTORY_MOVIES)) {//视频
                            file = new File(FileSDCardUtil.getInstance().getPublickDiskMoviesDir(), fileName);
                        } else if (inserType.equals(DIRECTORY_MUSIC)) {//音乐
                            file = new File(FileSDCardUtil.getInstance().getPublickDiskMusicDir(), fileName);
                        }
                    }else {
                        if (inserType.equals(DIRECTORY_PICTURES)) {//图片
                            file = new File(FileSDCardUtil.getInstance().getPublickDiskImagePicCacheDir(), fileName);
                        } else if (inserType.equals(DIRECTORY_MOVIES)) {//视频
                            file = new File(FileSDCardUtil.getInstance().getPublickDiskMoviesCacheDir(), fileName);
                        } else if (inserType.equals(DIRECTORY_MUSIC)) {//音乐
                            file = new File(FileSDCardUtil.getInstance().getPublickDiskMusicCacheDir(), fileName);
                        }
                    }

                    if (isNeedDeleteOldFile && file.exists()) {
                        file.delete();
                    }

                    if (!file.getParentFile().exists()){
                        file.getParentFile().mkdirs();
                    }

                    if (!file.exists()){
                        file.createNewFile();
                    }


                    FileOutputStream fos = new FileOutputStream(file);
                    BufferedInputStream bis = new BufferedInputStream(is);
                    byte[] buffer = new byte[1024];
                    int len;
                    int total = 0;
                    int contentLeng = conn.getContentLength();
                    while ((len = bis.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                        total += len;
                        if (onFileDownListener != null) {
                            onFileDownListener.onFileDownStatus(0, null, (total * 100 / contentLeng), total, contentLeng);
                        }
                    }
                    fos.close();
                    bis.close();
                    is.close();
                }catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return file;
            }
        })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<File>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(File file) {
                        if (file != null && onFileDownListener != null) {
                            onFileDownListener.onFileDownStatus(1, file, 0, 0, 0);
                        } else {
                            onFileDownListener.onFileDownStatus(-1, null, 0, 0, 0);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }


    /**
     * 如果是要存放到沙盒外部目录，就需要使用此方法
     * @date: 创建时间:2019/12/11
     * @author: gaoxiaoxiong
     * @descripion: 保存图片，视频，音乐到公共地区，此操作需要在线程，不是我们自己的APP目录下面的
     * @param downPathUrl 下载文件的路径，需要包含后缀
     * @param inserType 存储类型，可选参数 DIRECTORY_PICTURES  ,DIRECTORY_MOVIES  ,DIRECTORY_MUSIC ，DIRECTORY_DOWNLOADS
     **/
    public void downFileFromServiceToPublicDir(String downPathUrl, Context context, String inserType, OnFileDownListener onFileDownListener) {
        if (inserType.equals(DIRECTORY_DOWNLOADS)){
            if (Build.VERSION.SDK_INT>=29){//android 10
                downUnKnowFileFromService(downPathUrl,context,inserType,onFileDownListener);//返回的是uri
            }else {
                downUnKnowFileFromService(downPathUrl,onFileDownListener);//返回的是file
            }
        }else {
            //下载到沙盒外部公共目录
            downMusicVideoPicFromService(downPathUrl,context,inserType,onFileDownListener);
        }
    }

    /**
     * @date :2020/3/17 0017
     * @author : gaoxiaoxiong
     * @description:下载文件到DIRECTORY_DOWNLOADS，适用于android<=9
     **/
    private void downUnKnowFileFromService(final String downPathUrl,final OnFileDownListener onFileDownListener){
        Observable.just(downPathUrl).subscribeOn(Schedulers.newThread()).map(new Function<String, File>() {
            @Override
            public File apply(String s) throws Exception {
                File file = null;
                URL url = new URL(downPathUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(30 * 1000);
                InputStream is = conn.getInputStream();
                long time = System.currentTimeMillis();
                int code = conn.getResponseCode();
                String prefix = downPathUrl.substring(downPathUrl.lastIndexOf(".") + 1);
                String fileName = null;
                if (code == HttpURLConnection.HTTP_OK) {
                    fileName = conn.getHeaderField("Content-Disposition");
                    // 通过Content-Disposition获取文件名，这点跟服务器有关，需要灵活变通
                    if (fileName == null || fileName.length() < 1) {
                        // 通过截取URL来获取文件名
                        URL downloadUrl = conn.getURL(); // 获得实际下载文件的URL
                        fileName = downloadUrl.getFile();
                        fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                    } else {
                        fileName = URLDecoder.decode(fileName.substring(
                                fileName.indexOf("filename=") + 9), "UTF-8");
                        // 有些文件名会被包含在""里面，所以要去掉，不然无法读取文件后缀
                        fileName = fileName.replaceAll("\"", "");
                    }
                }

                if (isEmpty(fileName)) {
                    fileName = time + "." + prefix;
                }
                file = new File(FileSDCardUtil.getInstance().getPublickDiskFileDirAndroid9(DIRECTORY_DOWNLOADS),fileName);
                if (!file.getParentFile().exists()){
                    file.getParentFile().mkdirs();
                }

                if (!file.exists()){
                    file.createNewFile();
                }
                FileOutputStream fos = new FileOutputStream(file);
                BufferedInputStream bis = new BufferedInputStream(is);
                byte[] buffer = new byte[1024];
                int len;
                int total = 0;
                int contentLeng = conn.getContentLength();
                while ((len = bis.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                    total += len;
                    if (onFileDownListener != null) {
                        onFileDownListener.onFileDownStatus(LOADING, null, (total * 100 / contentLeng), total, contentLeng);
                    }
                }
                return file;
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<File>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(File file) {
                if (file != null && onFileDownListener != null) {
                    onFileDownListener.onFileDownStatus(SUCCESS, file, 0, 0, 0);
                } else {
                    onFileDownListener.onFileDownStatus(FAIL, null, 0, 0, 0);
                }
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    /**
     * 如果是要存放到沙盒外部目录，就需要使用此方法
     * @date: 创建时间:2019/12/11
     * @author: gaoxiaoxiong
     * @descripion: 下载的文件到 DIRECTORY_DOWNLOADS，只有10以上才有 MediaStore.Downloads
     * @param downPathUrl 下载文件的路径，需要包含后缀
     * @param inserType 存储类型 DIRECTORY_DOWNLOADS
     **/
    private void downUnKnowFileFromService(final String downPathUrl,final Context context, String inserType,final OnFileDownListener onFileDownListener){
        if (inserType.equals(DIRECTORY_DOWNLOADS)){
            Observable.just(downPathUrl).subscribeOn(Schedulers.newThread()).map(new Function<String, Uri>() {
                @RequiresApi(api = Build.VERSION_CODES.Q)
                @Override
                public Uri apply(String s) throws Exception {
                    Uri uri = null;
                    try {
                        URL url = new URL(downPathUrl);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(30 * 1000);
                        InputStream is = conn.getInputStream();
                        long time = System.currentTimeMillis();
                        int code = conn.getResponseCode();
                        String prefix = downPathUrl.substring(downPathUrl.lastIndexOf(".") + 1);
                        String fileName = null;
                        if (code == HttpURLConnection.HTTP_OK) {
                            fileName = conn.getHeaderField("Content-Disposition");
                            // 通过Content-Disposition获取文件名，这点跟服务器有关，需要灵活变通
                            if (fileName == null || fileName.length() < 1) {
                                // 通过截取URL来获取文件名
                                URL downloadUrl = conn.getURL(); // 获得实际下载文件的URL
                                fileName = downloadUrl.getFile();
                                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                            } else {
                                fileName = URLDecoder.decode(fileName.substring(
                                        fileName.indexOf("filename=") + 9), "UTF-8");
                                // 有些文件名会被包含在""里面，所以要去掉，不然无法读取文件后缀
                                fileName = fileName.replaceAll("\"", "");
                            }
                        }

                        if (isEmpty(fileName)) {
                            fileName = time + "." + prefix;
                        }

                        ContentValues contentValues = new ContentValues();
                        contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                        contentValues.put(MediaStore.Downloads.MIME_TYPE,getMIMEType(fileName));
                        contentValues.put(MediaStore.Downloads.DATE_TAKEN, System.currentTimeMillis());
                        uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                        BufferedInputStream inputStream = new BufferedInputStream(is);
                        OutputStream os = context.getContentResolver().openOutputStream(uri);
                        if (os != null) {
                            byte[] buffer = new byte[1024];
                            int len;
                            int total = 0;
                            int contentLeng = conn.getContentLength();
                            while ((len = inputStream.read(buffer)) != -1) {
                                os.write(buffer, 0, len);
                                total += len;
                                if (onFileDownListener != null) {
                                    onFileDownListener.onFileDownStatus(LOADING, null, (total * 100 / contentLeng), total, contentLeng);
                                }
                            }
                        }
                        os.flush();
                        inputStream.close();
                        is.close();
                        os.close();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return uri;
                }
            })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Uri>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(Uri uri) {
                            if (uri != null && onFileDownListener != null) {
                                onFileDownListener.onFileDownStatus(SUCCESS, uri, 0, 0, 0);
                            } else {
                                onFileDownListener.onFileDownStatus(FAIL, null, 0, 0, 0);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }
    }


    /**
     * 如果是要存放到沙盒外部目录，就需要使用此方法
     * @date: 创建时间:2019/12/11
     * @author: gaoxiaoxiong
     * @descripion: 保存图片，视频，音乐到公共地区，此操作需要在线程，不是我们自己的APP目录下面的
     * @param downPathUrl 下载文件的路径，需要包含后缀
     * @param inserType 存储类型，可选参数 DIRECTORY_PICTURES  ,DIRECTORY_MOVIES  ,DIRECTORY_MUSIC
     **/
    private void downMusicVideoPicFromService(final String downPathUrl,final Context context,final String inserType,final OnFileDownListener onFileDownListener){
        Observable.just(downPathUrl).subscribeOn(Schedulers.newThread()).map(new Function<String, Uri>() {
            @Override
            public Uri apply(String s) throws Exception {
                Uri uri = null;
                try {
                    URL url = new URL(downPathUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(30 * 1000);
                    InputStream is = conn.getInputStream();
                    long time = System.currentTimeMillis();
                    int code = conn.getResponseCode();
                    String prefix = downPathUrl.substring(downPathUrl.lastIndexOf(".") + 1);
                    String fileName = null;
                    if (code == HttpURLConnection.HTTP_OK) {
                        fileName = conn.getHeaderField("Content-Disposition");
                        // 通过Content-Disposition获取文件名，这点跟服务器有关，需要灵活变通
                        if (fileName == null || fileName.length() < 1) {
                            // 通过截取URL来获取文件名
                            URL downloadUrl = conn.getURL(); // 获得实际下载文件的URL
                            fileName = downloadUrl.getFile();
                            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                        } else {
                            fileName = URLDecoder.decode(fileName.substring(
                                    fileName.indexOf("filename=") + 9), "UTF-8");
                            // 有些文件名会被包含在""里面，所以要去掉，不然无法读取文件后缀
                            fileName = fileName.replaceAll("\"", "");
                        }
                    }

                    if (isEmpty(fileName)) {
                        fileName = time + "." + prefix;
                    }

                    ContentValues contentValues = new ContentValues();
                    if (inserType.equals(DIRECTORY_PICTURES)) {
                        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                        contentValues.put(MediaStore.Images.Media.MIME_TYPE, getMIMEType(fileName));
                        contentValues.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
                        //只是往 MediaStore 里面插入一条新的记录，MediaStore 会返回给我们一个空的 Content Uri
                        //接下来问题就转化为往这个 Content Uri 里面写入
                        uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                    } else if (inserType.equals(DIRECTORY_MOVIES)) {
                        contentValues.put(MediaStore.Video.Media.MIME_TYPE, getMIMEType(fileName));
                        contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, fileName);
                        contentValues.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
                        //只是往 MediaStore 里面插入一条新的记录，MediaStore 会返回给我们一个空的 Content Uri
                        //接下来问题就转化为往这个 Content Uri 里面写入
                        uri = context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
                    } else if (inserType.equals(DIRECTORY_MUSIC)) {
                        contentValues.put(MediaStore.Audio.Media.MIME_TYPE, getMIMEType(fileName));
                        contentValues.put(MediaStore.Audio.Media.DISPLAY_NAME, fileName);
                        if (Build.VERSION.SDK_INT>=29){//android 10
                            contentValues.put(MediaStore.Audio.Media.DATE_TAKEN, System.currentTimeMillis());
                        }
                        //只是往 MediaStore 里面插入一条新的记录，MediaStore 会返回给我们一个空的 Content Uri
                        //接下来问题就转化为往这个 Content Uri 里面写入
                        uri = context.getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues);
                    }
                    BufferedInputStream inputStream = new BufferedInputStream(is);
                    OutputStream os = context.getContentResolver().openOutputStream(uri);
                    if (os != null) {
                        byte[] buffer = new byte[1024];
                        int len;
                        int total = 0;
                        int contentLeng = conn.getContentLength();
                        while ((len = inputStream.read(buffer)) != -1) {
                            os.write(buffer, 0, len);
                            total += len;
                            if (onFileDownListener != null) {
                                onFileDownListener.onFileDownStatus(LOADING, null, (total * 100 / contentLeng), total, contentLeng);
                            }
                        }
                    }

                    //oppo手机不会出现在照片里面，但是会出现在图集里面
                    if (inserType.equals(DIRECTORY_PICTURES)){//如果是图片
                        //扫描到相册
                        String[] filePathArray = FileSDCardUtil.getInstance().getPathFromContentUri(uri,context);
                        MediaScannerConnection.scanFile(context, new String[] {filePathArray[0]}, new String[]{"image/jpeg"}, new MediaScannerConnection.OnScanCompletedListener(){
                            @Override
                            public void onScanCompleted(String path, Uri uri) {
                                Log.e(TAG,"PATH:"+path);
                            }
                        } );
                    }
                    os.flush();
                    inputStream.close();
                    is.close();
                    os.close();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return uri;
            }
        })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Uri>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Uri uri) {
                        if (uri != null && onFileDownListener != null) {
                            onFileDownListener.onFileDownStatus(SUCCESS, uri, 0, 0, 0);
                        } else {
                            onFileDownListener.onFileDownStatus(FAIL, null, 0, 0, 0);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG,"错误信息:"+e.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    /**
     * @date :2020/3/17 0017
     * @author : gaoxiaoxiong
     * @description:根据文件后缀名获得对应的MIME类型
     * @param fileName 文件名，需要包含后缀.xml类似这样的
     **/
    public String getMIMEType(String fileName) {
        String type="*/*";
        //获取后缀名前的分隔符"."在fName中的位置。
        int dotIndex = fileName.lastIndexOf(".");
        if(dotIndex < 0){
            return type;
        }
        /* 获取文件的后缀名*/
        String end=fileName.substring(dotIndex,fileName.length()).toLowerCase();
        if(end=="")return type;
        //在MIME和文件类型的匹配表中找到对应的MIME类型。
        for(int i=0;i<getFileMiMeType().length;i++){ //MIME_MapTable??在这里你一定有疑问，这个MIME_MapTable是什么？
            if(end.equals(getFileMiMeType()[i][0]))
                type = getFileMiMeType()[i][1];
        }
        return type;
    }

    /**
     * @date :2020/3/17 0017
     * @author : gaoxiaoxiong
     * @description:获取文件的mimetype类型
     **/
    public String[][] getFileMiMeType() {
        String[][] MIME_MapTable = {
                //{后缀名，MIME类型}
                {".3gp", "video/3gpp"},
                {".apk", "application/vnd.android.package-archive"},
                {".asf", "video/x-ms-asf"},
                {".avi", "video/x-msvideo"},
                {".bin", "application/octet-stream"},
                {".bmp", "image/bmp"},
                {".c", "text/plain"},
                {".class", "application/octet-stream"},
                {".conf", "text/plain"},
                {".cpp", "text/plain"},
                {".doc", "application/msword"},
                {".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"},
                {".xls", "application/vnd.ms-excel"},
                {".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
                {".exe", "application/octet-stream"},
                {".gif", "image/gif"},
                {".gtar", "application/x-gtar"},
                {".gz", "application/x-gzip"},
                {".h", "text/plain"},
                {".htm", "text/html"},
                {".html", "text/html"},
                {".jar", "application/java-archive"},
                {".java", "text/plain"},
                {".jpeg", "image/jpeg"},
                {".jpg", "image/jpeg"},
                {".js", "application/x-javascript"},
                {".log", "text/plain"},
                {".m3u", "audio/x-mpegurl"},
                {".m4a", "audio/mp4a-latm"},
                {".m4b", "audio/mp4a-latm"},
                {".m4p", "audio/mp4a-latm"},
                {".m4u", "video/vnd.mpegurl"},
                {".m4v", "video/x-m4v"},
                {".mov", "video/quicktime"},
                {".mp2", "audio/x-mpeg"},
                {".mp3", "audio/x-mpeg"},
                {".mp4", "video/mp4"},
                {".mpc", "application/vnd.mpohun.certificate"},
                {".mpe", "video/mpeg"},
                {".mpeg", "video/mpeg"},
                {".mpg", "video/mpeg"},
                {".mpg4", "video/mp4"},
                {".mpga", "audio/mpeg"},
                {".msg", "application/vnd.ms-outlook"},
                {".ogg", "audio/ogg"},
                {".pdf", "application/pdf"},
                {".png", "image/png"},
                {".pps", "application/vnd.ms-powerpoint"},
                {".ppt", "application/vnd.ms-powerpoint"},
                {".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"},
                {".prop", "text/plain"},
                {".rc", "text/plain"},
                {".rmvb", "audio/x-pn-realaudio"},
                {".rtf", "application/rtf"},
                {".sh", "text/plain"},
                {".tar", "application/x-tar"},
                {".tgz", "application/x-compressed"},
                {".txt", "text/plain"},
                {".wav", "audio/x-wav"},
                {".wma", "audio/x-ms-wma"},
                {".wmv", "audio/x-ms-wmv"},
                {".wps", "application/vnd.ms-works"},
                {".xml", "text/plain"},
                {".z", "application/x-compress"},
                {".zip", "application/x-zip-compressed"},
                {"", "*/*"}
        };
        return MIME_MapTable;
    }


    /**
     * Return whether the string is null or 0-length.
     *
     * @param s The string.
     * @return {@code true}: yes<br> {@code false}: no
     * true表示为空  false表示不是空
     */
    public boolean isEmpty(final CharSequence s) {
        return s == null || s.length() == 0;
    }
}
