package com.xiaoming.minio;

import android.app.ProgressDialog;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import io.minio.MinioClient;

public class MainActivity extends AppCompatActivity {

    private final static String IP = "http://xxx.xxx.xxx.xxx:10010"; // 你的minio服务器地址
    private final static String ACCESSKEY = "xxxx"; // 你的minio服务器账号
    private final static String SECRETKEY = "xxxxxxxxxx"; // 你的minio服务器账号秘钥
    private final static String BUCKET_NAME = "padlogs"; // 你的minio服务器BUCKET
    private final static String FILE_NAME = "img_banner_01.png"; // 你要存在minio服务器上的文件名
    private static String FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "minio" + File.separator;

    private ProgressDialog dialog;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1: // 图片上传minio成功
                    Toast.makeText(MainActivity.this, "图片上minio成功", Toast.LENGTH_SHORT).show();
                    break;
                case 2: // 图片上传minio失败
                    Toast.makeText(MainActivity.this, "图片上minio失败", Toast.LENGTH_SHORT).show();
                    break;
            }
            if (dialog != null)
                dialog.dismiss();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 申请权限
        String[] perms = new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
        ActivityCompat.requestPermissions(this, perms, 0xFF);
    }

    public void OnClick(View v) {
        dialog = ProgressDialog.show(this, "请稍后", "我正在上Minio,不要心急!");
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 给本地文件夹写入图片
                File dir = new File(FILE_PATH);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                FILE_PATH += "test.png";
                File file = new File(FILE_PATH);
                InputStream is = null;
                OutputStream os = null;
                try {
                    is = getAssets().open("img_banner_01.png");
                    os = new FileOutputStream(file);
                    int ch = 0;
                    while ((ch = is.read()) != -1) {
                        os.write(ch);
                    }
                    os.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        os.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // 上传图片到minio服务器
                try {
                    MinioClient minioClient = new MinioClient(IP, ACCESSKEY, SECRETKEY);
                    boolean isExist = minioClient.bucketExists(BUCKET_NAME);
                    if (isExist) {
                        System.out.println("--Bucket already exists.");
                    } else {
                        minioClient.makeBucket(BUCKET_NAME);
                    }
                    minioClient.putObject(BUCKET_NAME, FILE_NAME, FILE_PATH);
                    System.out.print("--" + FILE_PATH + "上传成功");
                    mHandler.sendEmptyMessage(1);
                } catch (Exception e) {
                    e.printStackTrace();
                    mHandler.sendEmptyMessage(2);
                }
            }
        }).start();
    }

}
