package com.example.myapplication.Utills;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.SystemClock;
import android.widget.RelativeLayout;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

public class SaveImageFile {
    private static final String DCIM = "/DCIM/";
    private static final String CAPTURE = "/capture";
    private Context context;
    private String fullPath;

    public SaveImageFile(Context context) {
        this.context = context;
    }

    public String getBatchDirectoryName() {
        String app_folder_path = "";
        app_folder_path = Environment.getExternalStorageDirectory().toString() + DCIM + getAppName() + CAPTURE;
        File dir = new File(app_folder_path);
        if (!dir.exists() && !dir.mkdirs()) {

        }

        return app_folder_path;
    }

    public void SaveImage(Bitmap finalBitmap, RelativeLayout rootLayout) {
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + DCIM + getAppName());
        if (!myDir.exists()) {
            myDir.mkdir();//directory is created;
        }
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        final String fname = "Image-" + n + ".jpg";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            fullPath = root + "/DCIM/" + getAppName() + "/" + fname;
            Snackbar.make(rootLayout, "Saved Image in " + fullPath, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        } catch (Exception e) {
            Snackbar.make(rootLayout, "Can't Save Image" + fullPath, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }

    }

    public String getAppName() {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = packageManager.getApplicationInfo(context.getApplicationInfo().packageName, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return (String) (applicationInfo != null ? packageManager.getApplicationLabel(applicationInfo) : "Unknown");
    }

    public static boolean saveBitmap(Bitmap bitmap, String path) {
        boolean ret = false;
        final File rootDir = new File(path);
        if (!rootDir.exists()) {
            if (!rootDir.mkdirs()) {

            }
        }
        String filename = path + SystemClock.uptimeMillis() + ".png";
        try {
            final FileOutputStream out = new FileOutputStream(filename);
            bitmap.compress(Bitmap.CompressFormat.PNG, 99, out);
            out.flush();
            out.close();
            ret = true;
        } catch (final Exception e) {
        }
        return ret;
    }
}
