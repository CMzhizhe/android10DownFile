package com.gxx.myapplication;

import android.Manifest;

import androidx.appcompat.app.AppCompatActivity;

import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * @author : gaoxiaoxiong
 * @date :2019/10/24 0024
 * @description:权限工具类
 **/
public class PermissonsUtil {
    //APP 需要的所有权限
    String[] AppNedPermissonsArray = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };


    public interface OnPermissonsListener {
        void onSuccess();

        void onFail();
    }

    /**
     * @date :2019/10/24 0024
     * @author : gaoxiaoxiong
     * @description:请求权限
     **/
    public Disposable requestPermisson(AppCompatActivity activity, String[] list,final OnPermissonsListener onPermissonsListener) {
        RxPermissions rxPermission = new RxPermissions(activity);
        return rxPermission.request(list).subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                if (aBoolean) {//申请的权限全部允许
                    if (onPermissonsListener != null) {
                        onPermissonsListener.onSuccess();
                    }
                } else { //只要有一个权限被拒绝，就会执行
                    if (onPermissonsListener != null) {
                        onPermissonsListener.onFail();
                    }
                }
            }
        });
    }

    /**
     * @date :2019/12/30 0030
     * @author : gaoxiaoxiong
     * @description:获取需要的权限
     **/
    public String[] getAppNedPermissonsArray() {
       // List<String> permissonList = new ArrayList<>();
        return AppNedPermissonsArray;
       /* if (MLUtils.getInstance().getAndroidVersion() >= 29) {//Android 10的版本
            for (int i = 0; i < AppNedPermissonsArray.length; i++) {
                if (!AppNedPermissonsArray[i].equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    permissonList.add(AppNedPermissonsArray[i]);
                }
            }
            String[] strArray=permissonList.toArray(new String[permissonList.size()]);
            return strArray;
        }else {
            return AppNedPermissonsArray;
        }*/
    }
}
