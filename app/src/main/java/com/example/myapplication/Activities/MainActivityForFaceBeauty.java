package com.example.myapplication.Activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.extensions.HdrImageCaptureExtender;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.myapplication.R;
import com.example.myapplication.Utills.SaveImageFile;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivityForFaceBeauty extends AppCompatActivity {
    private static final int IMAGE_REQUEST = 100;
    private static int lensFacing = CameraSelector.LENS_FACING_BACK;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private CardView captureBtn, pickImageBtn, swipCamera;
    private PreviewView cameraPreviewView;
    private RelativeLayout rootLayout;
    private Camera camera;
    private String opt = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_face);
        init();
        pickImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("image/*");
                startActivityForResult(Intent.createChooser(i, "Select Picture"), IMAGE_REQUEST);
            }
        });
        swipCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    lensFacing = CameraSelector.LENS_FACING_FRONT;
                } else {
                    lensFacing = CameraSelector.LENS_FACING_BACK;
                }
                startCamera();
            }
        });

    }

    private void init() {
        rootLayout = findViewById(R.id.main_relativeLayout);
        captureBtn = findViewById(R.id.main_btn_capture);
        pickImageBtn = findViewById(R.id.main_btn_pickimage);
        swipCamera = findViewById(R.id.main_btn_swipcamera);
        cameraPreviewView = findViewById(R.id.main_camera_view_finder);

    }

    private void startCamera() {

        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    cameraProvider.unbindAll();
                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    // No errors need to be handled for this Future.
                    // This should never be reached.
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }


    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .build();

        ImageCapture.Builder builder = new ImageCapture.Builder();

        //Vendor-Extensions (The CameraX extensions dependency in build.gradle)
        HdrImageCaptureExtender hdrImageCaptureExtender = HdrImageCaptureExtender.create(builder);

        // Query if extension is available (optional).
        if (hdrImageCaptureExtender.isExtensionAvailable(cameraSelector)) {
            // Enable the extension if available.
            hdrImageCaptureExtender.enableExtension(cameraSelector);
        }

        final ImageCapture imageCapture = builder
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
                .build();
        preview.setSurfaceProvider(cameraPreviewView.createSurfaceProvider());
        camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageAnalysis, imageCapture);


        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.v("yhl click:", "captureBtn");
                if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    opt = "captureBtn-opt-front";
                }
                if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    opt = "captureBtn-opt-back";
                }
                SaveImageFile imageFile = new SaveImageFile(MainActivityForFaceBeauty.this);

                SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
                File file = new File(getFilesDir(), mDateFormat.format(new Date()) + ".jpg");

                ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
                imageCapture.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(MainActivityForFaceBeauty.this, ImagePreviewActivity.class);
                                intent.putExtra("imageUrl", file.getPath());
                                intent.putExtra("opt", opt);
                                startActivity(intent);
                            }

                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException error) {
                        //Toast.makeText(MainActivity.this, "Photo capture failed: "+ error, Toast.LENGTH_SHORT).show();
                        Log.v("yhl MainActivity :", "Photo capture failed");
                        error.printStackTrace();
                    }
                });
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            try {

                if (data != null) {
                    Uri imageUri = data.getData();
                    Intent intent = new Intent(MainActivityForFaceBeauty.this, DesignActivity.class);
                    intent.putExtra("imageUri", imageUri.toString());
                    opt = "pickImageBtn";
                    Log.v("onActivityResult", imageUri.toString());
                    intent.putExtra("opt", opt);

                    startActivity(intent);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Snackbar.make(rootLayout, "You haven't picked Image", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }
    }


    public void displayNeverAskAgainDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivityForFaceBeauty.this);
        builder.setMessage("We need to capture & save Image for performing necessary task. Please permit the permission through "
                + "Settings screen.\n\nSelect Permissions -> Enable permission");
        builder.setCancelable(false);
        builder.setPositiveButton("Permit Manually", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Intent intent = new Intent();
                intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.show();
    }


}