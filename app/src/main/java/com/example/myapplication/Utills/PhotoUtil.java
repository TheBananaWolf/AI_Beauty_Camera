package com.example.myapplication.Utills;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;


public class PhotoUtil {

    // start camera
    public static String start_camera(Activity activity, int requestCode) {
        Uri imageUri;
        // save image in cache path
        File outputImage = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/lite_mobile/", System.currentTimeMillis() + ".jpg");
        try {
            if (outputImage.exists()) {
                outputImage.delete();
            }
            File out_path = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/lite_mobile/");
            if (!out_path.exists()) {
                out_path.mkdirs();
            }

            outputImage.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT >= 24) {
            // compatible with Android 7.0 or over
            imageUri = FileProvider.getUriForFile(activity,
                    "com.example.myapplication.provider", outputImage);
        } else {
            imageUri = Uri.fromFile(outputImage);
        }
        // set system camera Action
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        // set save photo path
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        // set photo quality, min is 0, max is 1
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
        activity.startActivityForResult(intent, requestCode);
        // return image absolute path
        return outputImage.getAbsolutePath();
    }

    // get picture in photo
    public static void use_photo(Activity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        activity.startActivityForResult(intent, requestCode);
    }

    // get photo from Uri
    public static String get_path_from_URI(Context context, Uri uri) {
        String result;
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            result = uri.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }


}