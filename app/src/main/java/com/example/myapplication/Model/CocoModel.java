package com.example.myapplication.Model;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Log;

import com.dailystudio.app.utils.ArrayUtils;
import com.dailystudio.app.utils.BitmapUtils;
import com.dailystudio.development.Logger;
import com.example.myapplication.Utills.ImageProcess;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.nnapi.NnApiDelegate;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Random;

public class CocoModel {

    private Bitmap backgroud;
    private Activity activity;
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;
    private final static int INPUT_SIZE = 257;
    private final static int NUM_CLASSES = 21;
    private final static int COLOR_CHANNELS = 3;
    private final static int BYTES_PER_POINT = 4;
    private ByteBuffer mImageData;
    private ByteBuffer mOutputs;
    private MappedByteBuffer modelFile;
    private int[][] mSegmentBits;
    private int[] mSegmentColors;
    private ImageProcess imageProcess;
    private final static Random RANDOM = new Random(System.currentTimeMillis());
    private String OpenCVFunctionName;
    private CocoModel.ProgressCallback callback;


    public CocoModel(Activity activity) {
        this.activity = activity;
        imageProcess = new ImageProcess();
    }

    public void setOpenCVFunctionName(String s) {
        this.OpenCVFunctionName = s;
    }

    public void setBackgroud(Bitmap b) {
        this.backgroud = b;
    }

    /**
     * Memory-map the model file in Assets.
     */
    private MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public void initialize(String modelfile) throws IOException {
        //load model file
        modelFile = loadModelFile(activity.getAssets(), modelfile);
        //set the input and output buffer to store input and output
        mImageData = ByteBuffer.allocateDirect(
                INPUT_SIZE * INPUT_SIZE * COLOR_CHANNELS * BYTES_PER_POINT);
        mImageData.order(ByteOrder.nativeOrder());
        mOutputs = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * NUM_CLASSES * BYTES_PER_POINT);
        mOutputs.order(ByteOrder.nativeOrder());

        //Store each position pixel for entry image space
        mSegmentBits = new int[INPUT_SIZE][INPUT_SIZE];
        //each different color corresponding to the different item such as "people", "car"
        mSegmentColors = new int[NUM_CLASSES];
        for (int i = 0; i < NUM_CLASSES; i++) {
            if (i == 0) {
                mSegmentColors[i] = Color.TRANSPARENT;
            } else {
                mSegmentColors[i] = Color.rgb(
                        (int) (255 * RANDOM.nextFloat()),
                        (int) (255 * RANDOM.nextFloat()),
                        (int) (255 * RANDOM.nextFloat()));
            }
        }
    }

    @SuppressLint("LongLogTag")
    public Bitmap segment(Bitmap bitmap) {
        Bitmap OpenCV = null;
        if (OpenCVFunctionName.equals("Gray"))
            OpenCV = imageProcess.Gray(bitmap);
        else if (OpenCVFunctionName.equals("Nostalgic"))
            OpenCV = imageProcess.Nostalgic(bitmap);
        else if (OpenCVFunctionName.equals("Comic_strip"))
            OpenCV = imageProcess.Comic_strip(bitmap);
        else if (OpenCVFunctionName.equals("Diffuse"))
            OpenCV = imageProcess.Diffuse(bitmap);
        else if (OpenCVFunctionName.equals("BigEye"))
            OpenCV = imageProcess.BigEye(bitmap);
        else if (OpenCVFunctionName.equals("Binarization")) {
            OpenCV = imageProcess.Binarization(bitmap);
            callback.callback(100);
            return OpenCV;
        } else if (OpenCVFunctionName.equals("Sketch")) {
            OpenCV = imageProcess.Sketch(bitmap);
            callback.callback(100);
            return OpenCV;
        } else if (OpenCVFunctionName.equals("Contour")) {
            OpenCV = imageProcess.Contour(bitmap);
            callback.callback(100);
            return OpenCV;
        } else if (OpenCVFunctionName.equals("Cast")) {
            OpenCV = imageProcess.Cast(bitmap);
            callback.callback(100);
            return OpenCV;
        } else if (OpenCVFunctionName.equals("Iced")) {
            OpenCV = imageProcess.Iced(bitmap);
            callback.callback(100);
            return OpenCV;
        } else if (OpenCVFunctionName.equals("Relief")) {
            OpenCV = imageProcess.Relief(bitmap);
            callback.callback(100);
            return OpenCV;
        }
        if (modelFile == null) {
            Logger.warn("tf model is NOT initialized.");
            return null;
        }
        if (bitmap == null) {
            return null;
        }
        //get user input image dim for last few step to re-create the same dim image
        int tempw = bitmap.getWidth();
        int temph = bitmap.getHeight();
        //
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(OpenCV, INPUT_SIZE, INPUT_SIZE, false);
        int w = resizedBitmap.getWidth();
        int h = resizedBitmap.getHeight();
        Logger.debug("bitmap: %d x %d,", w, h);

        if (w > INPUT_SIZE || h > INPUT_SIZE) {
            Logger.warn("invalid bitmap size: %d x %d [should be: %d x %d]",
                    w, h,
                    INPUT_SIZE, INPUT_SIZE);

            return null;
        }

        if (w < INPUT_SIZE || h < INPUT_SIZE) {
            bitmap = BitmapUtils.extendBitmap(
                    bitmap, INPUT_SIZE, INPUT_SIZE, Color.BLACK);

            w = bitmap.getWidth();
            h = bitmap.getHeight();
            Logger.debug("extend bitmap: %d x %d,", w, h);
        }
        //read/write the data from the position 0 for the corresponding bytebuffer
        mImageData.rewind();
        mOutputs.rewind();

        int[] mIntValues = new int[w * h];

        resizedBitmap.getPixels(mIntValues, 0, w, 0, 0, w, h);
        //set RGB to each position for input image
        int pixel = 0;
        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                if (pixel >= mIntValues.length) {
                    break;
                }

                final int val = mIntValues[pixel++];
                mImageData.putFloat((((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);//R
                mImageData.putFloat((((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);//G
                mImageData.putFloat(((val & 0xFF) - IMAGE_MEAN) / IMAGE_STD);//B
            }
        }

        //Config the TFL interpreter
        Interpreter.Options options = new Interpreter.Options();
        NnApiDelegate delegate = new NnApiDelegate();
        options.addDelegate(delegate);

        Interpreter interpreter = new Interpreter(modelFile, options);

        debugInputs(interpreter);
        debugOutputs(interpreter);

        //Count the process time for TFLite

        final long start = System.currentTimeMillis();

        Logger.debug("inference starts %d", start);
        Log.v("inference starts %d", String.valueOf(start));
        interpreter.run(mImageData, mOutputs);
        final long end = System.currentTimeMillis();
        Logger.debug("inference finishes at %d", end);
        Log.v("inference finishes %d", String.valueOf(end));
        Logger.debug("%d millis per core segment call.", (end - start));
        Log.v("tom", String.valueOf((end - start)));

        //create the new mask bitmap and the fill 0 to the mSegmentBits which represent base image is  transparent
        Bitmap maskBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        fillZeroes(mSegmentBits);

        float maxVal = 0;
        float val = 0;
        float progress = 0;
        float total = h * w * NUM_CLASSES + 10; // 因为后面还有合并操作，所以分母设置的稍微大一点点
        int curIndex = 0;
        //set the different color for each different layer
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                //mSegmentBits[x][y] = 0;

                for (int c = 0; c < NUM_CLASSES; c++) {
                    callback.callback(Math.round(progress));
                    val = mOutputs.getFloat((y * w * NUM_CLASSES + x * NUM_CLASSES + c) * BYTES_PER_POINT);
                    if (c == 0 || val > maxVal) {
                        maxVal = val;
                        mSegmentBits[x][y] = c;
                    }
                    progress = (curIndex++ / total) * 100;

                }

                maskBitmap.setPixel(x, y, mSegmentColors[mSegmentBits[x][y]]);
            }
        }
        /*
        Based on the mask to segment the image to get the human part
         */
        Bitmap maskBitmapModifySize = Bitmap.createScaledBitmap(maskBitmap, tempw, temph, false);
        Bitmap humanPortion = cropBitmapWithMask(OpenCV, maskBitmapModifySize);


        //putting the modified part to the background image
        Bitmap maskBitmapModifySize1 = Bitmap.createScaledBitmap(maskBitmap, tempw, temph, false);
        Bitmap humanPortion1 = cropBitmapWithMask(humanPortion, maskBitmapModifySize1);
        Bitmap result = BackgroudReplace(humanPortion1, bitmap);
        callback.callback(100);
        return result;
    }


    /**
     * @param original input original image bitmap
     * @param mask     mask which be generated by calling the segment method
     * @return the human portion from the original image based on the mask
     */
    private Bitmap cropBitmapWithMask(Bitmap original, Bitmap mask) {
        if (original == null
                || mask == null) {
            return null;
        }
        //the dim of the user input image
        final int w = original.getWidth();
        final int h = original.getHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }

        Bitmap cropped = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        cropped.setHasAlpha(true);

        Canvas canvas = new Canvas(cropped);

        Paint paintForMask = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint paintForOriginalImage = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintForMask.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        paintForOriginalImage.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));

        canvas.drawBitmap(original, 0, 0, paintForOriginalImage);
        canvas.drawBitmap(mask, 0, 0, paintForMask);

        paintForMask.setXfermode(null);
        paintForOriginalImage.setXfermode(null);

        return cropped;
    }

    private Bitmap BackgroudReplace(Bitmap cropped/*human part*/, Bitmap original) {
        //the dim of the user imput image
        final int w = original.getWidth();
        final int h = original.getHeight();
        //brand new and transparent image based on the original image
        Bitmap FinalBackgroundWithHuman = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Paint paint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint1.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        Canvas canvas1 = new Canvas(FinalBackgroundWithHuman);
        if (backgroud != null)
            canvas1.drawBitmap(backgroud, 0, 0, paint1);
        canvas1.drawBitmap(cropped, 0, 0, null);

        paint1.setXfermode(null);
        return FinalBackgroundWithHuman;
    }

    private void fillZeroes(int[][] array) {
        if (array == null) {
            return;
        }

        int r;
        for (r = 0; r < array.length; r++) {
            Arrays.fill(array[r], 0);
        }
    }

    private static void debugInputs(Interpreter interpreter) {
        if (interpreter == null) {
            return;
        }

        final int numOfInputs = interpreter.getInputTensorCount();
        Logger.debug("[TF-LITE-MODEL] input tensors: [%d]", numOfInputs);

        for (int i = 0; i < numOfInputs; i++) {
            Tensor t = interpreter.getInputTensor(i);
            Logger.debug("[TF-LITE-MODEL] input tensor[%d[: shape[%s]",
                    i,
                    ArrayUtils.intArrayToString(t.shape()));
        }
    }

    private static void debugOutputs(Interpreter interpreter) {
        if (interpreter == null) {
            return;
        }

        final int numOfOutputs = interpreter.getOutputTensorCount();
        Logger.debug("[TF-LITE-MODEL] output tensors: [%d]", numOfOutputs);

        for (int i = 0; i < numOfOutputs; i++) {
            Tensor t = interpreter.getOutputTensor(i);
            Logger.debug("[TF-LITE-MODEL] output tensor[%d[: shape[%s]",
                    i,
                    ArrayUtils.intArrayToString(t.shape()));
        }
    }

    public void addProgressCallback(final CocoModel.ProgressCallback callback) {
        this.callback = callback;
    }

    public interface ProgressCallback {
        public void callback(int progress);
    }
}
