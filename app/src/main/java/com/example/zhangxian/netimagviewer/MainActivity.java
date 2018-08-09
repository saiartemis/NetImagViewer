package com.example.zhangxian.netimagviewer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int LOAD_IMAGE = 1;
    private static final int LOAD_IMAGE_ERROR = 2;
    private ImageView iv;
    private ArrayList<String> paths;
    int count = 0;
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {

            switch (message.what)
            {
                case LOAD_IMAGE:
                Bitmap bitmap = (Bitmap) message.obj;
                iv.setImageBitmap(bitmap);
                Toast.makeText(MainActivity.this,"加载成功",Toast.LENGTH_SHORT).show();
                break;
                case LOAD_IMAGE_ERROR:
                    String text = (String) message.obj;
                    Toast.makeText(MainActivity.this,text,Toast.LENGTH_SHORT).show();
                    break;

            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv = findViewById(R.id.iv);
        loadAllPath();
    }

    /**
     * 将读取到的路径信息写入info.txt
     */
    private void loadAllPath() {

        new Thread() {
            @Override
            public void run() {
                try {
                    //网络获取访问必须在线程里
                    URL url = new URL("http://192.168.3.2:8080/Test/path.html");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    int code = connection.getResponseCode();
                    if (code == 200) {
                        //1、把从网络读取的内容存放到流中
                        InputStream is = connection.getInputStream();
                        //2、创建文件info.txt
                        File file = new File(getCacheDir(), "info.txt");
                        //3、创建输出流
                        FileOutputStream fos = new FileOutputStream(file);
                        //4、创建读取缓冲区
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, len);
                        }
                        is.close();
                        fos.close();
                    }
                    beginLoadImage();

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }.start();
    }

    public void pre(View view)
    {
        count = count -1;
        if(count<0)
        {
            Toast.makeText(MainActivity.this,"已经是最上一页",Toast.LENGTH_SHORT).show();
            count=0;
        }
        else
        {
            loadImgByPath(paths.get(count));
        }
    }

    public void next(View view)
    {
        count = count + 1;
        if(count>=paths.size())
        {
            Toast.makeText(MainActivity.this,"已经是最后一页",Toast.LENGTH_SHORT).show();
            count=paths.size()-1;
        }
        else
        {
            loadImgByPath(paths.get(count));
        }
    }

    private void beginLoadImage() {
        try
        {
            paths = new ArrayList();
            File file = new File(getCacheDir(),"info.txt");
            FileInputStream fis = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;
            while((line = br.readLine())!=null)
            {
                paths.add(line);
            }
            fis.close();
            loadImgByPath(paths.get(0));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }


    }

    private void loadImgByPath(final String path){
        new Thread()
        {
            @Override
            public void run() {
                File file = new File(getCacheDir(),path.replace("/","")+".png");
                if(file.exists()&&file.length()>0)
                {
                    System.out.println("通过缓存把图片读出来。。。");
                    Message msg = Message.obtain();
                    msg.what = LOAD_IMAGE;
                    msg.obj = BitmapFactory.decodeFile(file.getAbsolutePath());
                    handler.sendMessage(msg);
                }
                else
                {
                    System.out.println("通过访问网络把图片资源获取出来");
                    try
                    {
                        URL url = new URL(path);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("GET");
                        int code = connection.getResponseCode();
                        if(code == 200)
                        {
                            InputStream is = connection.getInputStream();
                            Bitmap bitmap = BitmapFactory.decodeStream(is);
                            FileOutputStream fos = new FileOutputStream(file);
                            bitmap.compress(Bitmap.CompressFormat.PNG,100,fos);
                            fos.close();
                            is.close();
                            Message msg = Message.obtain();
                            msg.what = LOAD_IMAGE;
                            msg.obj = BitmapFactory.decodeFile(file.getAbsolutePath());
                            handler.sendMessage(msg);
                        }
                        else
                        {
                            Message msg = Message.obtain();
                            msg.what = LOAD_IMAGE_ERROR;
                            msg.obj = "获取图片失败"+code;
                            handler.sendMessage(msg);
                        }
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                        Message msg = Message.obtain();
                        msg.what = LOAD_IMAGE_ERROR;
                        msg.obj = "获取图片失败";
                        handler.sendMessage(msg);
                    }
                }
            }
        }.start();

    }

}
