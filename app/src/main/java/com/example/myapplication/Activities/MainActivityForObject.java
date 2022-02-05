package com.example.myapplication.Activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Model.ObjectDetection;
import com.example.myapplication.R;
import com.example.myapplication.Utills.PhotoUtil;

public class MainActivityForObject extends AppCompatActivity {

    private static final String TAG = MainActivityForObject.class.getName();
    private static final int USE_PHOTO = 1001;
    private static final int START_CAMERA = 1002;
    private String camera_image_path;
    private ImageView show_image;
    private TextView result_text;
    private boolean load_result = false;
    ObjectDetection test = new ObjectDetection();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_for_object);
        init_view();
    }


    private void init_view() {
        show_image = (ImageView) findViewById(R.id.show_image);
        result_text = (TextView) findViewById(R.id.result_text);
        result_text.setMovementMethod(ScrollingMovementMethod.getInstance());
        Button start_photo = (Button) findViewById(R.id.start_camera);
        start_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                load_result = test.load_model(MainActivityForObject.this);
                if (!load_result) {
                    Toast.makeText(MainActivityForObject.this, "never load model", Toast.LENGTH_SHORT).show();
                    return;
                }
                camera_image_path = PhotoUtil.start_camera(MainActivityForObject.this, START_CAMERA); //获得一张图片的本地路径
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String image_path;
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case USE_PHOTO:
                    if (data == null) {
                        Log.w(TAG, "user photo data is null");
                        return;
                    }
                    Uri image_uri = data.getData();
                    image_path = PhotoUtil.get_path_from_URI(MainActivityForObject.this, image_uri);
                    System.out.println("----------------------------------------------------------------");
                    System.out.println(image_path);
                    System.out.println("----------------------------------------------------------------");
                    BitmapFactory.Options opt = new BitmapFactory.Options();
                    opt.inJustDecodeBounds = false;
                    Bitmap oriImage = BitmapFactory.decodeFile(image_path, opt);
                    Bitmap res = test.predict_image(oriImage);
                    show_image.setImageBitmap(res);

                    break;
                case START_CAMERA:

                    BitmapFactory.Options opt1 = new BitmapFactory.Options();
                    opt1.inJustDecodeBounds = false;
                    Bitmap oriImage2 = BitmapFactory.decodeFile(camera_image_path, opt1);
                    Bitmap res2 = test.predict_image(oriImage2);
                    show_image.setImageBitmap(res2);
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {

        test.close();//模型关闭与销毁 释放内存 避免内存泄露

        test = null;

        super.onDestroy();

    }

}
