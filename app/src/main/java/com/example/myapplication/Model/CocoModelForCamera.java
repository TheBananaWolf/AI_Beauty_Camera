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

public class CocoModelForCamera {

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
    public CocoModelForCamera(Activity activity) {
        this.activity = activity;
        imageProcess =new ImageProcess();
    }
    public void setOpenCVFunctionName(String s){
        this.OpenCVFunctionName=s;
    }
    public void setBackgroud(Bitmap b){
        this.backgroud=b;
    }
    /** Memory-map the model file in Assets. */
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
                        (int)(255 * RANDOM.nextFloat()),
                        (int)(255 * RANDOM.nextFloat()),
                        (int)(255 * RANDOM.nextFloat()));
            }
        }
    }
    @SuppressLint("LongLogTag")
    public Bitmap segment(Bitmap bitmap) {

        if (modelFile == null) {
            Logger.warn("tf model is NOT initialized.");
            return null;
        }
        if (bitmap == null) {
            return null;
        }
        Log.v("picture","1");
        Bitmap OpenCV=imageProcess.BigEye(bitmap);
        //OpenCV=imageProcess.mopi(OpenCV);
        Log.v("picture","2");
        //get user input image dim for last few step to re-create the same dim image
        int tempw=bitmap.getWidth();
        int temph=bitmap.getHeight();
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
        //set the different color for each different layer
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                //mSegmentBits[x][y] = 0;

                for (int c = 0; c < NUM_CLASSES; c++) {
                    val = mOutputs.getFloat((y * w * NUM_CLASSES + x * NUM_CLASSES + c) * BYTES_PER_POINT);
                    if (c == 0 || val > maxVal) {
                        maxVal = val;
                        mSegmentBits[x][y] = c;
                    }
                }

                maskBitmap.setPixel(x, y, mSegmentColors[mSegmentBits[x][y]]);
            }
        }
        /*
        Based on the mask to segment the image to get the human part
         */
        Bitmap maskBitmapModifySize = Bitmap.createScaledBitmap(maskBitmap, tempw, temph, false);
        Bitmap humanPortion=cropBitmapWithMask(OpenCV,maskBitmapModifySize);
        //OpenCV method to modify the the human part

//        if(OpenCVFunctionName.equals("Gray"))
//            OpenCV=imageProcess.Gray(humanPortion);
//        else if(OpenCVFunctionName.equals("Sketch"))
//            OpenCV=imageProcess.Sketch(humanPortion);
//        else if(OpenCVFunctionName.equals("Binarization"))
//            OpenCV=imageProcess.Binarization(humanPortion);


//        else if(OpenCVFunctionName.equals("Contour"))
//            OpenCV=imageProcess.Contour(humanPortion);
//        else if(OpenCVFunctionName.equals("Nostalgic"))
//            OpenCV=imageProcess.Nostalgic(humanPortion);
//        else if(OpenCVFunctionName.equals("Comic_strip"))
//            OpenCV=imageProcess.Comic_strip(humanPortion);
//        else if(OpenCVFunctionName.equals("Diffuse"))
//            OpenCV=imageProcess.Diffuse(humanPortion);
//        else if(OpenCVFunctionName.equals("Cast"))
//            OpenCV=imageProcess.Cast(humanPortion);
//        else if(OpenCVFunctionName.equals("Iced"))
//            OpenCV=imageProcess.Iced(humanPortion);
//        else if(OpenCVFunctionName.equals("Relief"))
//            OpenCV=imageProcess.Relief(humanPortion);

        //putting the modified part to the background image
        Bitmap maskBitmapModifySize1 = Bitmap.createScaledBitmap(maskBitmap, tempw, temph, false);
        Bitmap humanPortion1=cropBitmapWithMask(humanPortion,maskBitmapModifySize1);

        Bitmap result=BackgroudReplace(humanPortion1,bitmap);
        return result;
    }



    /**
     *
     * @param original input original image bitmap
     * @param mask mask which be generated by calling the segment method
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
        canvas1.drawBitmap(backgroud, 0, 0, paint1);
        canvas1.drawBitmap(cropped,0,0,null);

        paint1.setXfermode(null);
        return FinalBackgroundWithHuman;
    }




//    private void setInputOutputDetails(Bitmap bitmap) {
//        // 获取模型输入数据格式
////        final int maxSize = 257;
////        int outWidth;
////        int outHeight;
////        int inWidth = bitmap.getWidth();
////        int inHeight = bitmap.getHeight();
////        if(inWidth > inHeight){
////            outWidth = maxSize;
////            outHeight = (inHeight * maxSize) / inWidth;
////        } else {
////            outHeight = maxSize;
////            outWidth = (inWidth * maxSize) / inHeight;
////        }
////        mImageData = ByteBuffer.allocateDirect(
////                1 * INPUT_SIZE * INPUT_SIZE * COLOR_CHANNELS * BYTES_PER_POINT);
////        mImageData.order(ByteOrder.nativeOrder());
////
////        mOutputs = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * NUM_CLASSES * BYTES_PER_POINT);
////        mOutputs.order(ByteOrder.nativeOrder());
//
//
//
//        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 513, 513, false);
//        DataType imageDataType = tfLite.getInputTensor(0).dataType();
//
//
//        inputImageBuffer = new TensorImage(imageDataType);
//        inputImageBuffer.load(resizedBitmap);
//
//        DataType probabilityDataType = tfLite.getOutputTensor(0).dataType();
//        int[] probabilityShape =tfLite.getOutputTensor(0).shapeSignature();
//
//
//        outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);
//    }

//    /**
//     * Converts ByteBuffer with segmentation mask to the Bitmap
//     *
//     * @param byteBuffer Output ByteBuffer from Interpreter.run
//     * @param imgSizeX Model output image width
//     * @param imgSizeY Model output image height
//     * @return Mono color Bitmap mask
//     */
//    private Bitmap convertByteBufferToBitmap(ByteBuffer byteBuffer, int imgSizeX, int imgSizeY){
//        byteBuffer.rewind();
//        byteBuffer.order(ByteOrder.nativeOrder());
//        Bitmap bitmap = Bitmap.createBitmap(imgSizeX , imgSizeY, Bitmap.Config.ARGB_4444);
//        int[] pixels = new int[imgSizeX * imgSizeY];
//        for (int i = 0; i < imgSizeX * imgSizeY; i++)
//            if (byteBuffer.getFloat()>0.5)
//                pixels[i]= Color.argb(100, 255, 105, 180);
//            else
//                pixels[i]=Color.argb(0, 0, 0, 0);
//
//        bitmap.setPixels(pixels, 0, imgSizeX, 0, 0, imgSizeX, imgSizeY);
//        return bitmap;
//    }
//    private Bitmap getOutputImage(ByteBuffer output){
//        output.rewind();
//
//        int outputWidth = 300;
//        int outputHeight = 168;
//        Bitmap bitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888);
//        int [] pixels = new int[outputWidth * outputHeight];
//        for (int i = 0; i < outputWidth * outputHeight; i++) {
//            //val a = 0xFF;
//            //float a = (float) 0xFF;
//
//            //val r: Float = output?.float!! * 255.0f;
//            //byte val = output.get();
//            float r = ((float) output.get()) * 255.0f;
//            //float r = ((float) output.get());
//
//            //val g: Float = output?.float!! * 255.0f;
//            float g = ((float) output.get()) * 255.0f;
//            //float g = ((float) output.get());
//
//            //val b: Float = output?.float!! * 255.0f;
//            float b = ((float) output.get()) * 255.0f;
//            //float b = ((float) output.get());
//
//
//            //pixels[i] = a shl 24 or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
//            pixels[i] = (((int) r) << 16) | (((int) g) << 8) | ((int) b);
//        }
//        bitmap.setPixels(pixels, 0, outputWidth, 0, 0, outputWidth, outputHeight);
//
//        return bitmap;
//    }
//    private int floatToInt(float data) {
//        int tmp = Math.round(data);
//        if (tmp < 0){
//            tmp = 0;
//        }else if (tmp > 255) {
//            tmp = 255;
//        }
////        Log.e(TAG, tmp + " " + data);
//        return tmp;
//    }
//    private Bitmap floatArrayToBitmap(float[] data, int width, int height) {
//        int [] intdata = new int[width * height];
//        // 因为我们用的Bitmap是ARGB的格式，而data是RGB的格式，所以要经过转换，A指的是透明度
//        for (int i = 0; i < width * height; i++) {
//            int R = floatToInt(data[3 * i]);
//            int G = floatToInt(data[3 * i + 1]);
//            int B = floatToInt(data[3 * i + 2]);
//
//            intdata[i] = (0xff << 24) | (R << 16) | (G << 8) | (B << 0);
//
//        }
//        //得到位图
//        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//        bitmap.setPixels(intdata, 0, width, 0, 0, width, height);
//        return bitmap;
//    }
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
        Logger.debug("[TF-LITE-MODEL] input tensors: [%d]",numOfInputs);

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
        Logger.debug("[TF-LITE-MODEL] output tensors: [%d]",numOfOutputs);

        for (int i = 0; i < numOfOutputs; i++) {
            Tensor t = interpreter.getOutputTensor(i);
            Logger.debug("[TF-LITE-MODEL] output tensor[%d[: shape[%s]",
                    i,
                    ArrayUtils.intArrayToString(t.shape()));
        }
    }
}
