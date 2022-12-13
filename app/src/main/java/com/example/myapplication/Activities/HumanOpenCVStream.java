package com.example.myapplication.Activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.Surface;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.myapplication.Model.CocoModelForCamera;
import com.example.myapplication.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HumanOpenCVStream extends AppCompatActivity {
    private static final String MODEL_FILE = "lite-model_deeplabv3_1_metadata_2.tflite";
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final int IMAGE_REQUEST = 100;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final String[] PERMISSIONS_STORAGE = {
            "android.permission.CAMERA",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};
    private static final int lensFacing = CameraSelector.LENS_FACING_FRONT;

    static {
        System.loadLibrary("opencv_java3");
    }

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    CameraManager cManager;
    String cameraId;
    private CardView pickImageBtn, swipCamera;
    private PreviewView cameraPreviewView;
    private RelativeLayout rootLayout;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private Camera camera;
    private final String opt = "";
    private com.example.myapplication.Model.CocoModelForCamera CocoModelForCamera;
    private Bitmap background;
    private ImageView imageView3;
    private Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.human_stream_opencv_tensorflowlite);
        init();
        imageView3 = findViewById(R.id.imageView3);
        imageView3.setScaleType(ImageView.ScaleType.CENTER_CROP);
        CocoModelForCamera = new CocoModelForCamera(this);

        try {
            CocoModelForCamera.initialize(MODEL_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        AssetManager assetManager = getAssets();

        InputStream backgroud = null;
        try {
            backgroud = assetManager.open("b1.jpeg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        background = BitmapFactory.decodeStream(backgroud);
        startCamera();
    }

    private void init() {
        rootLayout = findViewById(R.id.main_relativeLayout);
        pickImageBtn = findViewById(R.id.main_btn_pickimage);
        swipCamera = findViewById(R.id.main_btn_swipcamera);
        cameraPreviewView = findViewById(R.id.main_camera_view_finder);
        activity = this;
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        cameraId = getFrontFacingCameraId(manager);

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
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        ImageCapture.Builder builder = new ImageCapture.Builder();


        imageAnalysis.setAnalyzer(ActivityCompat.getMainExecutor(this),
                new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(@NonNull ImageProxy image) {
                        Bitmap mBitmap = Bitmap.createScaledBitmap(background, (int) (1.5 * image.getWidth()), image.getHeight(), false);
                        CocoModelForCamera.setBackgroud(mBitmap);
                        @SuppressLint("UnsafeExperimentalUsageError")
                        Image img = image.getImage();
                        int rotation = 0;
                        try {
                            rotation = getRotationCompensation(cameraId, activity, true);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                        Image image1 = InputImage.fromMediaImage(img, rotation).getMediaImage();
                        Bitmap toBitmap = toBitmap(image1);
                        Bitmap mask = CocoModelForCamera.segment(toBitmap);
                        Bitmap tempResult = rotateImage(mask, 90);
                        imageView3.setImageBitmap(tempResult);
                        image.close();
                    }
                });
        final ImageCapture imageCapture = builder
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
                .build();
        preview.setSurfaceProvider(cameraPreviewView.createSurfaceProvider());
        camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageAnalysis, imageCapture);

    }

    /**
     * Get the angle by which an image must be rotated given the device's current
     * orientation.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private int getRotationCompensation(String cameraId, Activity activity, boolean isFrontFacing)
            throws CameraAccessException {
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int rotationCompensation = ORIENTATIONS.get(deviceRotation);

        // Get the device's sensor orientation.
        CameraManager cameraManager = (CameraManager) activity.getSystemService(CAMERA_SERVICE);
        int sensorOrientation = cameraManager
                .getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.SENSOR_ORIENTATION);

        if (isFrontFacing) {
            rotationCompensation = (sensorOrientation + rotationCompensation) % 360;
        } else { // back-facing
            rotationCompensation = (sensorOrientation - rotationCompensation + 360) % 360;
        }
        return rotationCompensation;
    }

    private String getFrontFacingCameraId(CameraManager cManager) {
        try {
            String cameraId;
            int cameraOrientation;
            CameraCharacteristics characteristics;
            for (int i = 0; i < cManager.getCameraIdList().length; i++) {
                cameraId = cManager.getCameraIdList()[i];
                characteristics = cManager.getCameraCharacteristics(cameraId);
                cameraOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cameraOrientation == CameraCharacteristics.LENS_FACING_FRONT) {
                    return cameraId;
                }

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Bitmap rotateImage(Bitmap bitmap, float degree) {
        //create new matrix
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);
        Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);


        return rotatedBitmap;
    }

    public Bitmap toBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();//Luminance
        ByteBuffer uBuffer = planes[1].getBuffer();//Chrominance
        ByteBuffer vBuffer = planes[2].getBuffer();//Chroma

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }


    public void displayNeverAskAgainDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(HumanOpenCVStream.this);
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
