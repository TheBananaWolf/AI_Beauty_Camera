package com.example.myapplication.Activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Model.SRGanModel;
import com.example.myapplication.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class MainActivityForSuperResolution extends AppCompatActivity {
    //sytle_transfer_anime.tflite or gan_generator.tflite or SP.tflite
    private static final String SRGAN_MODEL_FILE = "gan_generator.tflite";
    private static final String TAG = "FANG";
    private static final Random random = new Random();
    final String SR_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "SuperResolution" + File.separator;
    private Button testButton;
    private Button selectButton;
    private Button saveButton;
    private ImageView imageViewSrc;
    private ImageView imageViewDest;
    private ProgressBar srProgressBar;
    private Activity activity;
    private SRGanModel srGanModel;
    private Bitmap mergeBitmap;
    private final String[] testImages = {"0829x4-crop.png", "0851x4-crop.png", "0869x4-crop.png", "1.png", "2.png"};
    private Handler handler;
    private HandlerThread handlerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("size", String.valueOf(testImages.length));
        setContentView(R.layout.activity_main_super);

        testButton = findViewById(R.id.test_button);
        selectButton = findViewById(R.id.select_button);
        imageViewSrc = findViewById(R.id.imageview_src);
        imageViewDest = findViewById(R.id.imageview_dest);
        srProgressBar = findViewById(R.id.sr_progress_bar);
        saveButton = findViewById(R.id.save_button);
        activity = this;

        // 申请读写权限
        if (!hasPermission()) {
            requestPermission();
        }

        srGanModel = new SRGanModel(activity);
        srGanModel.loadModel(SRGAN_MODEL_FILE);  //导入模型
        srGanModel.addSRProgressCallback(new SRGanModel.SRProgressCallback() {
            @Override
            public void callback(int progress) {
                srProgressBar.setProgress(progress, true);
            }
        });

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    resetView();
                    AssetManager assetManager = getAssets();
                    InputStream inputStream = assetManager.open(testImages[random.nextInt(testImages.length)]);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                    srGanInference(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetView();
                Intent intent = new Intent(Intent.ACTION_PICK, null);
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(intent, 0x1);
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mergeBitmap != null) {
                    String text = "Save failed!";
                    if (saveBitmap(mergeBitmap)) {
                        text = "Save success!";
                    }
                    Toast toast = Toast.makeText(
                            getApplicationContext(), text, Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(
                                MainActivityForSuperResolution.this,
                                "Write external storage permission is required for this demo",
                                Toast.LENGTH_LONG)
                        .show();
            }
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    private void srGanInference(Bitmap bitmap) {

        runInBackground(new Runnable() {
            @Override
            public void run() {
                mergeBitmap = srGanModel.inference(bitmap);
                Log.e(TAG, "imageView width:" + bitmap.getWidth() + " height:" + bitmap.getHeight() +
                        " mergeBitmap width:" + mergeBitmap.getWidth() + " height:" + mergeBitmap.getHeight());
                // 显示图片
                runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                if (bitmap != null) {
                                    imageViewSrc.setImageBitmap(bitmap);
                                    imageViewDest.setImageBitmap(mergeBitmap);
                                }
                            }
                        });
            }
        });
    }

    //创建一个重置View函数
    private void resetView() {
        Bitmap mBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
        imageViewSrc.setImageBitmap(mBitmap);
        imageViewDest.setImageBitmap(mBitmap);
        srProgressBar.setProgress(0, true);

    }

    private synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // TODO Auto-generated method stub
        if (data == null) {
            return;
        }

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
            srGanInference(bitmap);
        } catch (Exception e) {
            Log.d("MainActivity", "[*]" + e);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean saveBitmap(Bitmap bitmap) {
        boolean ret = false;
        final File rootDir = new File(SR_ROOT);
        if (!rootDir.exists()) {
            if (!rootDir.mkdirs()) {
                Log.e(TAG, "Make dir failed");
            }
        }


        String filename = SR_ROOT + SystemClock.uptimeMillis() + ".png";
        try {
            final FileOutputStream out = new FileOutputStream(filename);
            bitmap.compress(Bitmap.CompressFormat.PNG, 99, out);
            out.flush();
            out.close();
            ret = true;
        } catch (final Exception e) {
            Log.e(TAG, "Exception!");
        }
        return ret;
    }

}