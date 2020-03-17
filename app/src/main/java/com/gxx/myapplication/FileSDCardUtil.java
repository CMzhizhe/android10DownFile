package com.gxx.myapplication;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import static android.os.Environment.DIRECTORY_MOVIES;
import static android.os.Environment.DIRECTORY_MUSIC;
import static android.os.Environment.DIRECTORY_PICTURES;

public class FileSDCardUtil {
    public static FileSDCardUtil fileSDCardUtil;

    public static FileSDCardUtil getInstance() {
        if (fileSDCardUtil == null) {
            synchronized (FileSDCardUtil.class) {
                if (fileSDCardUtil == null) {
                    fileSDCardUtil = new FileSDCardUtil();
                }
            }
        }
        return fileSDCardUtil;
    }

    /**
     * @date: 2019/5/22 0022
     * @author: gaoxiaoxiong
     * @description: 下载的图片保存的位置
     **/
    public String getPublickDiskImagePicDir() {
        return getPublickDiskFileDir(BaseApplication.getInstance(), DIRECTORY_PICTURES);
    }


    /**
     * @date: 2019/5/22 0022
     * @author: gaoxiaoxiong
     * @description: 电影保存的位置
     **/
    public String getPublickDiskMoviesDir() {
        return getPublickDiskFileDir(BaseApplication.getInstance(), DIRECTORY_MOVIES);
    }

    /**
     * @date :2019/12/16 0016
     * @author : gaoxiaoxiong
     * @description:音乐保存的位置
     **/
    public String getPublickDiskMusicDir() {
        return getPublickDiskFileDir(BaseApplication.getInstance(), DIRECTORY_MUSIC);
    }

    /**
     * @date 创建时间:2018/12/20
     * @author GaoXiaoXiong
     * @Description: 创建保存图片的缓存目录
     */
    public String getPublickDiskImagePicCacheDir() {
        return getPublickDiskCacheDir(BaseApplication.getInstance(), DIRECTORY_PICTURES);
    }

    /**
     * @date: 2019/6/21 0021
     * @author: gaoxiaoxiong
     * @description:获取音乐的缓存目录
     **/
    public String getPublickDiskMusicCacheDir() {
        return getPublickDiskCacheDir(BaseApplication.getInstance(), DIRECTORY_MUSIC);
    }

    /**
     * @date: 创建时间:2019/12/22
     * @author: gaoxiaoxiong
     * @descripion:获取视频的缓存目录
     **/
    public String getPublickDiskMoviesCacheDir() {
        return getPublickDiskCacheDir(BaseApplication.getInstance(), DIRECTORY_MOVIES);
    }



    /**
     * 作者：GaoXiaoXiong
     * 创建时间:2019/1/26
     * 注释描述:获取缓存目录
     *
     * @fileName 获取外部存储目录下缓存的 fileName的文件夹路径
     */
    public String getPublickDiskCacheDir(Context context, String fileName) {
        String cachePath = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {//此目录下的是外部存储下的私有的fileName目录
            cachePath = context.getExternalCacheDir().getPath() + "/" + fileName;  //SDCard/Android/data/你的应用包名/cache/fileName
        } else {
            cachePath = context.getCacheDir().getPath() + "/" + fileName;
        }
        File file = new File(cachePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath(); //SDCard/Android/data/你的应用包名/cache/fileName
    }

    /**
     * @date: 2019/8/2 0002
     * @author: gaoxiaoxiong
     * @description:获取外部存储目录下的 fileName的文件夹路径
     **/
    public String getPublickDiskFileDir(Context context, String fileName) {
        String cachePath = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {//此目录下的是外部存储下的私有的fileName目录
            cachePath = context.getExternalFilesDir(fileName).getAbsolutePath();  //mnt/sdcard/Android/data/com.my.app/files/fileName
        } else {
            cachePath = context.getFilesDir().getPath() + "/" + fileName;        //data/data/com.my.app/files
        }
        File file = new File(cachePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();  //mnt/sdcard/Android/data/com.my.app/files/fileName
    }


    /**
     * @date :2020/3/17 0017
     * @author : gaoxiaoxiong
     * @description:获取公共目录，注意，只适合android9.0以下的
     **/
    public String getPublickDiskFileDirAndroid9(String fileDir){
        String filePath = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            filePath = Environment.getExternalStoragePublicDirectory(fileDir).getPath();
        }
        File file = new File(filePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();
    }


    /**
     * @date: 创建时间:2020/1/13
     * @author: gaoxiaoxiong
     * @descripion:拷贝公共目录下的图片到沙盒cache目录
     **/
    public void copyPublicDirPic2LocalCacheDir(final Context context, List<Uri> uriList,final OnCopyPublicFile2PackageListener onCopyPublicFile2PackageListener) {
        String path = getPublickDiskImagePicCacheDir();
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        final String resultPath = path;
        Observable.just(uriList).subscribeOn(Schedulers.newThread()).map(new Function<List<Uri>, List<File>>() {
            @Override
            public List<File> apply(List<Uri> uriList) {
                List<File> newFilesList = new ArrayList<>();
                try {
                    for (int i = 0; i < uriList.size(); i++) {
                        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
                        Date date = new Date(System.currentTimeMillis());
                        String filename = format.format(date);
                        File destFile = new File(resultPath, filename + ".jpg");
                        if (!destFile.getParentFile().exists()) {
                            destFile.getParentFile().mkdirs();
                        }
                        if (!destFile.exists()) {
                            destFile.createNewFile();
                        }
                        ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uriList.get(i), "r");
                        FileInputStream inputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
                        FileOutputStream ostream = new FileOutputStream(destFile);
                        byte[] buffer = new byte[1024];
                        int byteCount = 0;
                        while ((byteCount = inputStream.read(buffer)) != -1) {  // 循环从输入流读取 buffer字节
                            ostream.write(buffer, 0, byteCount);        // 将读取的输入流写入到输出流
                        }
                        ostream.flush();
                        ostream.close();
                        inputStream.close();
                        newFilesList.add(destFile);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return newFilesList;
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<File>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(List<File> files) {
                        if (onCopyPublicFile2PackageListener != null) {
                            onCopyPublicFile2PackageListener.onCopyPublicFile2Package(files);
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
     * @param uri    需要拷贝的uri
     * @param toFile 输出的目标文件，
     * @date :2019/12/20 0020
     * @author : gaoxiaoxiong
     * @description:此方法将公共目录文件保存在沙盒目录下面,toFile最好是缓存目录下面的文件
     **/
    public void copyPublicDir2PackageDir(final Context context,final Uri uri,final File toFile,final OnCopyPublicFile2PackageListener onCopyPublicFile2PackageListener) {
        Observable.just(toFile).observeOn(Schedulers.newThread()).map(new Function<File, File>() {
            @Override
            public File apply(File file) {
                try {
                    if (toFile.exists()) {
                        toFile.delete();
                    }
                    if (!toFile.getParentFile().exists()) {
                        toFile.getParentFile().mkdirs();
                    }
                    if (!toFile.exists()) {
                        toFile.createNewFile();
                    }
                    ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r");
                    FileInputStream inputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
                    FileOutputStream ostream = new FileOutputStream(file);
                    byte[] buffer = new byte[1024];
                    int byteCount = 0;
                    while ((byteCount = inputStream.read(buffer)) != -1) {  // 循环从输入流读取 buffer字节
                        ostream.write(buffer, 0, byteCount);        // 将读取的输入流写入到输出流
                    }
                    ostream.flush();
                    ostream.close();
                    inputStream.close();
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
                        if (onCopyPublicFile2PackageListener != null) {
                            onCopyPublicFile2PackageListener.onCopyPublicFile2Package(file);
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
