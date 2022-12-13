package com.example.myapplication.Activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.Adapter.AdapterForRecycle;
import com.example.myapplication.Adapter.RecyclerTouchListener;
import com.example.myapplication.Model.CocoModel;
import com.example.myapplication.Model.SRGanModel;
import com.example.myapplication.R;
import com.example.myapplication.Utills.ListForRecyclerViewForImage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class imageProcessing extends AppCompatActivity {
    private static final String SRGAN_MODEL_FILE = "gan_generator.tflite";
    private static final String MODEL_FILE = "lite-model_deeplabv3_1_metadata_2.tflite";
    private static Bitmap image;
    final String SR_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "ImageProcess" + File.separator;
    private final List<ListForRecyclerViewForImage> listForRecyclerViewForImageList = new ArrayList<>();
    private final String TAG = "com.example.myapplication : imageProcessing";
    public SmallFaceActivity SmallFaceActivity;
    private Activity activity;
    private SRGanModel srGanModel;
    private CocoModel CocoModel;
    private ImageView imageViewSrc;
    private ImageView imageViewDest;
    private Handler handler;
    private String imagePath;
    private Bitmap merged;
    private HandlerThread handlerThread;
    private ProgressBar progressBar;
    private Button background;
    private Button save;
    private Button choose;
    private Uri path;
    private RecyclerView recyclerView;
    private RecyclerTouchListener recyclerTouchListener;

    public static Bitmap getSRC() {
        return image;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listview_for_opencv_picture);
        if (!hasPermission()) {
            requestPermission();
        }
        imageViewSrc = findViewById(R.id.imageview_src);
        imageViewDest = findViewById(R.id.imageview_dest);
        imageViewSrc.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageViewDest.setScaleType(ImageView.ScaleType.FIT_CENTER);
        progressBar = findViewById(R.id.progressBar);
        background = findViewById(R.id.ChooseBackground);
        choose = findViewById(R.id.ChooseImage);
        background = findViewById(R.id.ChooseBackground);
        save = findViewById(R.id.Save);
        recyclerView = findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(layoutManager);
    }

    @Override
    protected void onStart() {
        super.onStart();
        activity = this;
        srGanModel = new SRGanModel(activity);
        CocoModel = new CocoModel(activity);
        SmallFaceActivity = new SmallFaceActivity();
        srGanModel.loadModel(SRGAN_MODEL_FILE);
        try {
            CocoModel.initialize(MODEL_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        srGanModel.addSRProgressCallback(new SRGanModel.SRProgressCallback() {
            @Override
            public void callback(int progress) {
                progressBar.setProgress(progress, true);
            }
        });
        CocoModel.addProgressCallback(new CocoModel.ProgressCallback() {
            @Override
            public void callback(int progress) {
                progressBar.setProgress(progress, true);
            }
        });

        initRecyclerView();
        AdapterForRecycle adapter = new AdapterForRecycle(listForRecyclerViewForImageList);
        recyclerView.setAdapter(adapter);
        initRecyclerTouchListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        choose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, 1);
            }
        });

        background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, 2);
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (merged != null) {
                    String text = "Save failed!";
                    if (com.example.myapplication.Utills.SaveImageFile.saveBitmap(merged, SR_ROOT)) {
                        text = "Save success!" + " the saving path is " + SR_ROOT;
                    }
                    Toast toast = Toast.makeText(
                            getApplicationContext(), text, Toast.LENGTH_SHORT);
                    toast.show();
                    merged.recycle();
                }
            }
        });

        recyclerView.addOnItemTouchListener(recyclerTouchListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        image.recycle();
        merged.recycle();
        handlerThread.quitSafely();
        handler.removeCallbacksAndMessages(null);
    }

    private synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    private void initRecyclerView() {
        for (int i = 0; i < 1; i++) {
            ListForRecyclerViewForImage SuperResolution = new ListForRecyclerViewForImage("SuperResolution", R.drawable.superresultion);
            listForRecyclerViewForImageList.add(SuperResolution);
            // the back behaviour work not as designed
/*            ListForRecyclerViewForImage FaceBeauty = new ListForRecyclerViewForImage("FaceBeauty", R.drawable.facebeauty);
            listForRecyclerViewForImageList.add(FaceBeauty);
            ListForRecyclerViewForImage Pinch_face = new ListForRecyclerViewForImage("Pinch_face", R.drawable.neilian);
            listForRecyclerViewForImageList.add(Pinch_face);*/
            ListForRecyclerViewForImage Grey = new ListForRecyclerViewForImage("Gray", R.drawable.grey);
            listForRecyclerViewForImageList.add(Grey);
            ListForRecyclerViewForImage Detect = new ListForRecyclerViewForImage("Sketch", R.drawable.detect);
            listForRecyclerViewForImageList.add(Detect);
            ListForRecyclerViewForImage Binarization = new ListForRecyclerViewForImage("Binarization", R.drawable.binarization);
            listForRecyclerViewForImageList.add(Binarization);
            ListForRecyclerViewForImage Contour = new ListForRecyclerViewForImage("Contour", R.drawable.countor);
            listForRecyclerViewForImageList.add(Contour);
            ListForRecyclerViewForImage Nostalgic = new ListForRecyclerViewForImage("Nostalgic", R.drawable.nostalgic);
            listForRecyclerViewForImageList.add(Nostalgic);
            ListForRecyclerViewForImage Comic_strip = new ListForRecyclerViewForImage("Comic_strip", R.drawable.comic_strip);
            listForRecyclerViewForImageList.add(Comic_strip);
            ListForRecyclerViewForImage Cutout = new ListForRecyclerViewForImage("Cutout", R.drawable.diffuse);
            listForRecyclerViewForImageList.add(Cutout);
            ListForRecyclerViewForImage Cast = new ListForRecyclerViewForImage("Cast", R.drawable.cast);
            listForRecyclerViewForImageList.add(Cast);
            ListForRecyclerViewForImage Iced = new ListForRecyclerViewForImage("Iced", R.drawable.iced);
            listForRecyclerViewForImageList.add(Iced);
            ListForRecyclerViewForImage Relief = new ListForRecyclerViewForImage("Relief", R.drawable.relief);
            listForRecyclerViewForImageList.add(Relief);
            ListForRecyclerViewForImage BigEye = new ListForRecyclerViewForImage("BigEye", R.drawable.bigeye);
            listForRecyclerViewForImageList.add(BigEye);
            ListForRecyclerViewForImage Stiker = new ListForRecyclerViewForImage("Stiker", R.drawable.superresultion);
            listForRecyclerViewForImageList.add(Stiker);

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                //Display an error
                return;
            }
            path = data.getData();
            image = rotateImage1(data);
            if (merged != null) {
                imageViewDest.setImageDrawable(null);
                imageViewDest.setVisibility(View.GONE);
            }
            progressBar.setProgress(0);
            imageViewSrc.setImageBitmap(image);
            if (imageViewSrc.getVisibility() == View.GONE) {
                imageViewSrc.setVisibility(View.VISIBLE);
            }

        }
        if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                //Display an error
                return;
            }

            InputStream backgroudPic = null;
            ContextWrapper context = this;
            try {
                backgroudPic = context.getContentResolver().openInputStream(data.getData());

            } catch (IOException e) {
                e.printStackTrace();
            }
            Bitmap backgroundIMage = BitmapFactory.decodeStream(backgroudPic);
            Bitmap mBitmap = null;
            if (image != null) {
                mBitmap = Bitmap.createScaledBitmap(backgroundIMage, image.getWidth(), image.getHeight(), false);
                CocoModel.setBackgroud(mBitmap);
            } else
                Toast.makeText(
                                imageProcessing.this,
                                "CHOOSE INPUT IMAGE FIRST!!",
                                Toast.LENGTH_LONG)
                        .show();

        }
    }

    @SuppressLint("LongLogTag")
    public Bitmap rotateImage1(Intent data) {
        //create new matrix
        Bitmap result = null;
        Uri selectedImage = data.getData();
        Log.v("Uri for image", String.valueOf(selectedImage));
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Log.v("array size", String.valueOf(filePathColumn.length));
        Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
        Log.v("cursor size", cursor.getCount() + " " + cursor.getColumnName(0));
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

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void initRecyclerTouchListener() {
        recyclerTouchListener = new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                ListForRecyclerViewForImage listForRecyclerViewForImage = listForRecyclerViewForImageList.get(position);
                if (progressBar.getProgress() == 100) {
                    progressBar.setProgress(0);
                }

                if (image == null) {
                    Toast.makeText(imageProcessing.this, "Choose the input image first !!!!!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (listForRecyclerViewForImage.getName().equals("SuperResolution")) {
                    runInBackground(new Runnable() {
                        @Override
                        public void run() {
                            setProcessImage(0);
                        }
                    });
                } else if (listForRecyclerViewForImage.getName().equals("Gray")) {
                    runInBackground(new Runnable() {
                        @Override
                        public void run() {
                            CocoModel.setOpenCVFunctionName("Gray");
                            setProcessImage(1);
                        }
                    });
                } else if (listForRecyclerViewForImage.getName().equals("Sketch")) {
                    runInBackground(new Runnable() {
                        @Override
                        public void run() {
                            CocoModel.setOpenCVFunctionName("Sketch");
                            setProcessImage(1);
                        }
                    });
                } else if (listForRecyclerViewForImage.getName().equals("Binarization")) {
                    runInBackground(new Runnable() {
                        @Override
                        public void run() {
                            CocoModel.setOpenCVFunctionName("Binarization");
                            setProcessImage(1);
                        }
                    });
                } else if (listForRecyclerViewForImage.getName().equals("Cutout")) {
                    runInBackground(new Runnable() {
                        @Override
                        public void run() {
                            CocoModel.setOpenCVFunctionName("Diffuse");
                            setProcessImage(1);
                        }
                    });
                } else if (listForRecyclerViewForImage.getName().equals("Contour")) {
                    runInBackground(new Runnable() {
                        @Override
                        public void run() {
                            CocoModel.setOpenCVFunctionName("Contour");
                            setProcessImage(1);
                        }
                    });
                } else if (listForRecyclerViewForImage.getName().equals("Nostalgic")) {
                    runInBackground(new Runnable() {
                        @Override
                        public void run() {
                            CocoModel.setOpenCVFunctionName("Nostalgic");
                            setProcessImage(1);
                        }
                    });
                } else if (listForRecyclerViewForImage.getName().equals("Comic_strip")) {
                    runInBackground(new Runnable() {
                        @Override
                        public void run() {
                            CocoModel.setOpenCVFunctionName("Comic_strip");
                            setProcessImage(1);
                        }
                    });
                } else if (listForRecyclerViewForImage.getName().equals("FaceBeauty")) {
                    Intent temp = new Intent(imageProcessing.this, DesignActivity.class);
                    temp.putExtra("imageUri", imagePath);
                    temp.putExtra("opt", "");
                    startActivity(temp);
                } else if (listForRecyclerViewForImage.getName().equals("Diffuse")) {
                    runInBackground(new Runnable() {
                        @Override
                        public void run() {
                            CocoModel.setOpenCVFunctionName("Diffuse");
                            setProcessImage(1);
                        }
                    });
                } else if (listForRecyclerViewForImage.getName().equals("Cast")) {
                    runInBackground(new Runnable() {
                        @Override
                        public void run() {
                            CocoModel.setOpenCVFunctionName("Cast");
                            setProcessImage(1);
                        }
                    });
                } else if (listForRecyclerViewForImage.getName().equals("Iced")) {
                    runInBackground(new Runnable() {
                        @Override
                        public void run() {
                            CocoModel.setOpenCVFunctionName("Iced");
                            setProcessImage(1);
                        }
                    });
                } else if (listForRecyclerViewForImage.getName().equals("Relief")) {
                    runInBackground(new Runnable() {
                        @Override
                        public void run() {
                            CocoModel.setOpenCVFunctionName("Relief");
                            setProcessImage(1);
                        }
                    });
                } else if (listForRecyclerViewForImage.getName().equals("Pinch_face")) {
                    Intent temp = new Intent(imageProcessing.this, SmallFaceActivity.class);
                    temp.putExtra("imageUri", imagePath);
                    startActivity(temp);
                } else if (listForRecyclerViewForImage.getName().equals("BigEye")) {
                    runInBackground(new Runnable() {
                        @Override
                        public void run() {
                            CocoModel.setOpenCVFunctionName("BigEye");
                            setProcessImage(1);
                        }
                    });
                } else if (listForRecyclerViewForImage.getName().equals("Stiker")) {
                    Intent temp = new Intent(imageProcessing.this, MainActivityForSticker.class);
                    startActivity(temp);
                }
            }

            @Override
            public void onLongClick(View view, int position) {
                Toast.makeText(imageProcessing.this, "Chose the desired function", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void requestPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(
                            imageProcessing.this,
                            "Write external storage permission is required for this demo",
                            Toast.LENGTH_LONG)
                    .show();
        }
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
    }

    public void setProcessImage(int JID) {
        if (JID == 0) {
            merged = srGanModel.inference(image);
        } else {
            merged = CocoModel.segment(image);
        }
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (merged != null) {
                            imageViewSrc.setImageDrawable(null);
                            imageViewSrc.setVisibility(View.GONE);
                            Toast.makeText(imageProcessing.this, "Image Replaced by the Processed Image", Toast.LENGTH_LONG).show();
                            if (imageViewDest.getVisibility() == View.GONE) {
                                imageViewDest.setVisibility(View.VISIBLE);
                            }
                            imageViewDest.setImageBitmap(merged);
                        }
                    }
                });
    }
}
