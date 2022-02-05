package com.example.myapplication.Activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import com.example.myapplication.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * author:DingDeGao
 * time:2019-08-23-16:16
 * function: default function
 */
public class SmallFaceActivity extends AppCompatActivity {

    private static final int PICK_PHOTO_FOR_AVATAR = 1;
    private ImageView img;
    private ImageView imgResult;
    private SmallFaceView smallFaceView;
    private View showPreview;
    private Button compare;
    private Bitmap inputImage;
    private boolean isShowCompare = false;
    private String imagePath;
    private Button save;
    final String SR_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "ImageProcess" + File.separator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_small_face);

        imgResult = findViewById(R.id.imgResult);
        img = findViewById(R.id.img);
        showPreview = findViewById(R.id.showPreview);
        smallFaceView = findViewById(R.id.smallFaceView);
        save = findViewById(R.id.save);

        compare = findViewById(R.id.compare);
        compare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isShowCompare = !isShowCompare;
                compare.setText(isShowCompare ? "返回" : "对比");
                loadImage();
            }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                com.example.myapplication.Utills.SaveImageFile.saveBitmap(smallFaceView.getBitmap(), SR_ROOT);
               Toast.makeText(SmallFaceActivity.this,"Image is stored in the "+SR_ROOT,Toast.LENGTH_SHORT).show();
            }


        });
        if(inputImage==null)
        Log.v("szdfasdfasdfasfasdf","dafasdfasdfasdfasd");

        Intent intent = getIntent();
        Log.e("tomtomtotm", String.valueOf((intent.getExtras()).getString("imageUri")));

        try {
        Uri imageUri = Uri.fromFile(new File((intent.getExtras()).getString("imageUri")));
        final InputStream stream;

        stream = getContentResolver().openInputStream(imageUri);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        inputImage = BitmapFactory.decodeStream(stream, null, options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        smallFaceView.setBitmap(inputImage);
        loadImage();
    }


    private void loadImage() {

        showPreview.setVisibility(isShowCompare ? View.VISIBLE : View.GONE);
        smallFaceView.setVisibility(!isShowCompare ? View.VISIBLE : View.GONE);

        if (isShowCompare) {
            img.setImageBitmap(inputImage);
            imgResult.setImageBitmap(smallFaceView.getBitmap());
        }


//        img.setImageBitmap(CommonShareBitmap.originBitmap);
//
//        String faceJson = FacePoint.getFaceJson(this,"face_point1.json");
//
//        Bitmap bitmap = SmallFaceUtils.smallFaceMesh(CommonShareBitmap.originBitmap,
//                FacePoint.getLeftFacePoint(faceJson),
//                FacePoint.getRightFacePoint(faceJson),
//                FacePoint.getCenterPoint(faceJson), 5);
//
//
//        imgResult.setImageBitmap(bitmap);

    }

    /*
    public void pickHumanImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(intent, PICK_PHOTO_FOR_AVATAR);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_PHOTO_FOR_AVATAR && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                //Display an error
                return;
            }

//                InputStream inputStream = null;
//                ContextWrapper context = this;
//                try {
//                    inputStream = context.getContentResolver().openInputStream(data.getData());
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }




        }
    }

     */


    @SuppressLint("LongLogTag")
    public Bitmap rotateImage(Intent data) {
        //create new matrix
        Bitmap result = null;
        Uri selectedImage = data.getData();
        Log.v("Uri for image", String.valueOf(selectedImage));
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Log.v("array size", String.valueOf(filePathColumn.length));
        Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
        Log.v("cursor size", String.valueOf(cursor.getCount()) + " " + String.valueOf(cursor.getColumnName(0)));
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        imagePath = cursor.getString(columnIndex);
        Log.v("image path: ", imagePath);
        cursor.close();

        try {


            ExifInterface exif = new ExifInterface(imagePath);

            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int rotationInDegrees = exifToDegrees(rotation);
            Log.v("Picture rotationInDegrees", String.valueOf(rotationInDegrees));
            Matrix matrix = new Matrix();
            if (rotation != 0) {
                matrix.preRotate(rotationInDegrees);
            }
            Bitmap temp = BitmapFactory.decodeFile(imagePath);
            Log.v("temp size", String.valueOf(temp.getHeight()));
            result = Bitmap.createBitmap(temp, 0, 0, temp.getWidth(), temp.getHeight(), matrix, false);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }
}
