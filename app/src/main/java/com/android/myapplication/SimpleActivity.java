package com.android.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.cache.BitmapMemCache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Random;
import java.util.Vector;

public class SimpleActivity extends AppCompatActivity {

    private static final String TAG = "SimpleActivity";
    private TextView tv;

    private BitmapMemCache mMemCache;

    private Vector<Picinfo> v;

    private Handler mHandler;

    private ImageView image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple);

        mHandler = new Handler();

        v = new Vector<>();
        mMemCache = BitmapMemCache.From(this);

        tv = (TextView) findViewById(R.id.cache_info);

        findViewById(R.id.dump).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv.setText(mMemCache.dump(true));
            }
        });

        image = (ImageView) findViewById(R.id.sample_image);

        try {
            File dir = Environment.getExternalStorageDirectory();
            File imageFile = new File(dir, "item_pic.jpg");
            image.setImageBitmap(BitmapFactory.decodeStream(new FileInputStream(imageFile)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        startTestCache();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMemCache.clear();

    }

    public void startTestCache() {

        new Thread(new Runnable() {
            @Override
            public void run() {

                Random random = new Random(System.currentTimeMillis());

                Picinfo picInfo;

                while (true) {

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (v.size() == 0) continue;

                    int index = Math.abs(random.nextInt() % v.size());

                    picInfo = v.get(index);

                    long start = System.currentTimeMillis();

                    final Bitmap bitmap = mMemCache.get(picInfo.url, picInfo.width, picInfo.height);

                    System.out.println(String.format("Getting Item spends %d milliseconds", System.currentTimeMillis() - start));

                    if (bitmap == null) {

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                image.setImageResource(R.mipmap.missing);
                            }
                        });

                    } else {

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                image.setImageBitmap(bitmap);
                            }
                        });

                    }

                }

            }
        }).start();

        new Thread(new Runnable() {
            File dir = Environment.getExternalStorageDirectory();

            @Override
            public void run() {
                int count = 50;
                while (count-- > 0) {
                    File imageFile;
                    if (count % 2 == 1)
                        imageFile = new File(dir, "item_pic.jpg");
                    else
                        imageFile = new File(dir, "item_pic_180.jpg");

                    if (imageFile.exists()) {
                        try {
                            Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(imageFile));
                            long time = System.currentTimeMillis();
                            String key = String.format("%d", time);

                            mMemCache.put(key, bitmap.getWidth(), bitmap.getHeight(), bitmap);

                            v.add(new Picinfo(key, bitmap.getWidth(), bitmap.getHeight()));

                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.d(TAG, "file does not exist");
                    }

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Producer thread stopped !!!!");
                    }

                }
            }
        }).start();
    }

    class Picinfo {
        String url;
        int width;
        int height;

        public Picinfo(String url, int width, int height) {
            this.url = url;
            this.width = width;
            this.height = height;
        }
    }

}