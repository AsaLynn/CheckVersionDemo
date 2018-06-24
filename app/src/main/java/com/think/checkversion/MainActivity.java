package com.think.checkversion;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;
import org.xutils.common.Callback;
import org.xutils.http.HttpMethod;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private String TAG = this.getClass().getSimpleName();
    private String appname;
    private String serverVersion;
    private String lastForce;
    private String updateurl;
    private String upgradeinfo;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //请求接口,检测新版本
        checkNewVersion();
    }

    private void checkNewVersion() {
        String url = "http://169.254.38.24/version.json";
        x.http().get(new RequestParams(url), new Callback.CommonCallback<String>() {
            @Override
            public void onSuccess(String result) {
                Log.i(TAG, "onSuccess: ***-" + result);
                //解析json
                parseJson(result);
            }

            @Override
            public void onError(Throwable ex, boolean isOnCallback) {

            }

            @Override
            public void onCancelled(CancelledException cex) {

            }

            @Override
            public void onFinished() {

            }
        });
    }

    private void parseJson(String result) {
        try {
            JSONObject object = new JSONObject(result);
            appname = object.getString("appname");
            serverVersion = object.getString("serverVersion");
            lastForce = object.getString("lastForce");
            updateurl = object.getString("updateurl");
            upgradeinfo = object.getString("upgradeinfo");
        } catch (Exception e) {
            e.printStackTrace();
        }

        //判断是否弹框升级

        try {
            int intServion = Integer.parseInt(serverVersion);
            int currentVersion= getVersioCode();
            //当应用版本号低于服务器的版本号,则提示升级.
            if ( currentVersion < intServion){

                //如果lastForce == 1强制升级,否则正常升级
                if ("1".equals(lastForce)){
                    //强制升级
                    showForceUpdateDialog();
                }else {
                    //选择性升级
                    //弹出对话框.
                    showUpdateDialog();
                }
            }
            Log.i(TAG, "***currentVersion: "+currentVersion);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showForceUpdateDialog() {
        new AlertDialog
                .Builder(this)
                .setTitle("强制版本升级!" + appname)
                .setIcon(R.mipmap.ic_launcher)
                .setMessage(upgradeinfo)
                .setCancelable(false)
                .setNegativeButton("退出", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.this.finish();//关闭页面.
                    }
                }).setPositiveButton("更新", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
//                Toast.makeText(MainActivity.this,"升级!",Toast.LENGTH_SHORT).show();
                //下载apk进行升级新版本.
                downLoadApk();

                //创建通知.
                createNotification(MainActivity.this);
            }
        })
                .create()
                .show();
    }

    private void showUpdateDialog() {
        new AlertDialog
                .Builder(this)
                .setTitle("版本升级!" + appname)
                .setIcon(R.mipmap.ic_launcher)
                .setMessage(upgradeinfo)
                .setCancelable(false)
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, "取消升级!", Toast.LENGTH_SHORT).show();
                    }
                }).setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
//                Toast.makeText(MainActivity.this,"升级!",Toast.LENGTH_SHORT).show();
                //下载apk进行升级新版本.
                downLoadApk();

                //创建通知.
                createNotification(MainActivity.this);
            }
        })
                .create()
                .show();
    }

    private void downLoadApk() {
        //创建请求参数.
        RequestParams requestParams = new RequestParams(updateurl);
        //设置下载完成文件保存的位置
        requestParams.setSaveFilePath(downLoadPath + appname);
        //发送请求.
        //参数1,请求方式.参数2,提交参数.参数3:结果回调.
        x.http().request(HttpMethod.GET, requestParams, new Callback.ProgressCallback<File>() {
            @Override
            public void onSuccess(File result) {

            }

            @Override
            public void onError(Throwable ex, boolean isOnCallback) {

            }

            @Override
            public void onCancelled(CancelledException cex) {

            }

            @Override
            public void onFinished() {

            }

            @Override
            public void onWaiting() {

            }

            @Override
            public void onStarted() {

            }

            //total:文件总大小,current,当前进度.isDownloading,是否正在下载.
            @Override
            public void onLoading(long total, long current, boolean isDownloading) {
                notifyNotification(current, total);
                Log.i(TAG, "***onLoading: "+current);
                if (total == current) {//若下载完成
                    Log.i(TAG, "***onLoading: current" + current + "/total" + current);
                    Toast.makeText(MainActivity.this, "下载完成!", Toast.LENGTH_SHORT).show();
                    mBuilder.setContentText("下载完成");
                    mNotifyManager.notify(10086, mBuilder.build());


                    //安装apk文件
                    installApk(MainActivity.this,new File(downLoadPath,appname));
                }
            }
        });
    }

    //下载后文件保存的目录.
    private String downLoadPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/downloads/";
    //文件名字.


    private int getVersioCode() throws Exception {
        // 获取packagemanager的实例
        PackageManager packageManager = getPackageManager();
        // getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo = packageManager.getPackageInfo(getPackageName(), 0);
        return packInfo.versionCode;
    }

    /**
     * 创建通知栏进度条
     *
     */
    private void createNotification(Context context) {
        mNotifyManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher_round);
        mBuilder.setContentTitle("版本更新");
        mBuilder.setContentText("正在下载...");
        mBuilder.setProgress(0, 0, false);
        Notification notification = mBuilder.build();
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        //参数1:通知id,参数:通知.
        mNotifyManager.notify(10086, notification);
    }


    /**
     * 更新通知栏进度条
     *
     */
    private void notifyNotification(long percent, long length) {
        mBuilder.setProgress((int) length, (int) percent, false);
        mNotifyManager.notify(10086, mBuilder.build());
    }

    /**
     * 安装apk
     *
     * @param context 上下文
     * @param file    APK文件
     */
    private void installApk(Context context, File file) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

}
