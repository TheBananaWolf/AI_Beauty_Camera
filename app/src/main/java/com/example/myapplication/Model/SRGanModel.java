package com.example.myapplication.Model;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import org.tensorflow.lite.nnapi.NnApiDelegate;

public class SRGanModel {
    private static final String TAG = "FANG";

    private Interpreter tfLite;  //Interpreter主要用于加载模型和执行推理(转发)操作, load the model and perform inference (forward) operations
    private TensorImage inputImageBuffer;  //TensorImage用于向模型传输输入数据
    private TensorBuffer outputProbabilityBuffer;  //TensorBuffer用于获取模型输出数据

    private Activity activity;
    private NnApiDelegate gpuDelegate;
    private int scale = 4;
    private int cropBitmapSize = 24;
    private final Paint boxPaint = new Paint();
    private SRProgressCallback callback;

    public SRGanModel(Activity activity) {
        this.activity = activity;
        initPaint();
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    /***
     * 初始化画笔，用来调试切分图片和合并图片的
     */
    private void initPaint() {
        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(2.0f);
        boxPaint.setStrokeCap(Paint.Cap.ROUND);
        boxPaint.setStrokeJoin(Paint.Join.ROUND);
        boxPaint.setStrokeMiter(100);
    }

    /***
     * 导入模型
     * @param modelfile
     * @return
     */
    public boolean loadModel(String modelfile) {
        boolean ret = false;
        try {
            // 获取在assets中的模型
            MappedByteBuffer modelFile = loadModelFile(activity.getAssets(), modelfile);
            // 设置tflite运行条件，使用4线程和GPU进行加速
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);
            gpuDelegate = new NnApiDelegate();
            options.addDelegate(gpuDelegate);
            // 实例化tflite
            tfLite = new Interpreter(modelFile, options);
            ret = true;
            Log.v("tfLite", String.valueOf(tfLite));
        } catch (IOException e) {
            Log.v("WANGGUANJIE","QQ");
            e.printStackTrace();
        }

        return ret;
    }

    /***
     * 据输入图像的宽度和高度重置模型的输入和输出形状
     * 设置模型输入和输出的shape和类型
     * @param bitmap 要进行sr的图片
     */
    private void setInputOutputDetails(Bitmap bitmap) {
        // 获取模型输入数据格式
        Log.e(TAG, "imageDataType:" + bitmap);
        if(tfLite==null){
            Log.v("wangguanjie","w");
        }
        DataType imageDataType = tfLite.getInputTensor(0).dataType();


        // 创建TensorImage，用于存放图像数据
        inputImageBuffer = new TensorImage(imageDataType);
        inputImageBuffer.load(bitmap);

        // 因为模型的输入shape是任意宽高的图片，即{-1,-1,-1,3}，但是在tflite java版中，我们需要指定输入数据的具体大小。
        // 所以在这里，我们要根据输入图片的宽高来设置模型的输入的shape
        int[] inputShape = {1, bitmap.getHeight(), bitmap.getWidth(), 3};
        tfLite.resizeInput(tfLite.getInputTensor(0).index(), inputShape);
//        Log.e(TAG, "inputShape:" + bitmap.getByteCount());
//        for (int i : inputShape) {
//            Log.e(TAG, i + "");
//        }

        // 获取模型输出数据格式
        DataType probabilityDataType = tfLite.getOutputTensor(0).dataType();
//        Log.e(TAG, "probabilityDataType:" + probabilityDataType.toString());

        // 同样的，要设置模型的输出shape，因为我们用的模型的功能是在原图的基础上，放大scale倍，所以这里要乘以scale
        int[] probabilityShape = {1, bitmap.getWidth() * scale, bitmap.getHeight() * scale, 3};//tfLite.getOutputTensor(0).shapeSignature();
//        Log.e(TAG, "probabilityShape:");
//        for (int i : probabilityShape) {
//            Log.e(TAG, i + "");
//        }

        // Creates the output tensor and its processor.
        outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);
    }

    /***
     * 推理函数，将图片数据输送给模型并且得到输出结果，然后将输出结果转为Bitmap格式
     * @param bitmap
     * @return
     */
    public Bitmap inference(Bitmap bitmap) {
        /////////////////////////////////////////////////////////
//        // 直接对图片进行SR运算，当输入图片比较大的时候，APP就会被系统强制退出了
//        // 根据原图的小块图片设置模型输入
//        setInputOutputDetails(bitmap);
//        // 执行模型的推理，得到小块图片sr后的高清图片
//        tfLite.run(inputImageBuffer.getBuffer(), outputProbabilityBuffer.getBuffer());
//        // 将高清图片数据从TensorBuffer转成float[]，以转成安卓常用的Bitmap类型
//        float[] results = outputProbabilityBuffer.getFloatArray();
//        // 将图片从float[]转成Bitmap
//        Bitmap b = floatArrayToBitmap(results, bitmap.getWidth() * scale, bitmap.getHeight() * scale);
//        ///////////////////////////////////////////////////////
//        // 验证拆分和合并图片的代码
//        ArrayList<SplitBitmap> splitedBitmaps = splitBitmap(bitmap);
//        ArrayList<SplitBitmap> mergeBitmaps = new ArrayList<SplitBitmap>();
//        for (SplitBitmap sb : splitedBitmaps) {
//            SplitBitmap srsb = new SplitBitmap();
//            srsb.column = sb.column;
//            srsb.row = sb.row;
//            srsb.bitmap = sb.bitmap;
//            mergeBitmaps.add(srsb);
//        }
//        // 最后，将列表中的小块图片合并成一张大的图片并返回
//        Bitmap mergeBitmap = mergeBitmap(mergeBitmaps);
        /////////////////////////////////////////////////////////
        // 对所有原图的小块图片进行sr运算，并把得到的小块高清图存到sredBitmaps列表中
        ArrayList<SplitBitmap> splitedBitmaps = splitBitmap(bitmap);  //将原图切分成众多小块图片, 返回位图列表
        ArrayList<SplitBitmap> mergeBitmaps = new ArrayList<SplitBitmap>();
        float progress = 0;
        float total = splitedBitmaps.size() + 10; // 因为后面还有合并操作，所以分母设置的稍微大一点点
        int curIndex = 0;
        // 对所有原图的小块图片进行sr运算，并把得到的小块高清图存到sredBitmaps列表中
        for (SplitBitmap sb : splitedBitmaps) {
            callback.callback(Math.round(progress));
            // 根据原图的小块图片设置模型输入
            setInputOutputDetails(sb.bitmap);  //sb.bitmap当前小块的位图
            // 执行模型的推理，得到小块图片sr后的高清图片
            tfLite.run(inputImageBuffer.getBuffer(), outputProbabilityBuffer.getBuffer());
            // 将高清图片数据从TensorBuffer转成float[]，以转成安卓常用的Bitmap类型
            float[] results = outputProbabilityBuffer.getFloatArray();
            // 将图片从float[]转成Bitmap
            Bitmap b = floatArrayToBitmap(results, sb.bitmap.getWidth() * scale, sb.bitmap.getHeight() * scale);
            SplitBitmap srsb = new SplitBitmap();
            srsb.column = sb.column;
            srsb.row = sb.row;
            srsb.bitmap = b;
            mergeBitmaps.add(srsb);
            progress = (curIndex++ / total) * 100;
        }
        // 最后，将列表中的小块高清图片合并成一张大的高清图片并返回
        Bitmap mergeBitmap = mergeBitmap(mergeBitmaps);
        callback.callback(100);
        return mergeBitmap;
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

    /***
     * 模型的输出结果是float型的数据，需要转成int型
     * @param data
     * @return
     */
    private int floatToInt(float data) {
        int tmp = Math.round(data);
        if (tmp < 0){
            tmp = 0;
        }else if (tmp > 255) {
            tmp = 255;
        }
//        Log.e(TAG, tmp + " " + data);
        return tmp;
    }

    /***
     * 模型的输出得到的是一个float数据，这个数组就是sr后的高清图片信息，我们要将它转成Bitmap格式才好在安卓上使用
     * @param data 图片数据
     * @param width 图片宽度
     * @param height 图片高度
     * @return 返回图片的位图
     */
    private Bitmap floatArrayToBitmap(float[] data, int width, int height) {
        int [] intdata = new int[width * height];
        // 因为我们用的Bitmap是ARGB的格式，而data是RGB的格式，所以要经过转换，A指的是透明度
        for (int i = 0; i < width * height; i++) {
            int R = floatToInt(data[3 * i]);
            int G = floatToInt(data[3 * i + 1]);
            int B = floatToInt(data[3 * i + 2]);

            intdata[i] = (0xff << 24) | (R << 16) | (G << 8) | (B << 0);

//            Log.e(TAG, intdata[i]+"");
        }
        //得到位图
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(intdata, 0, width, 0, 0, width, height);
        return bitmap;
    }

    /***
     * 这个类用来存放切分后的小块图片的信息
     */
    private class SplitBitmap{
        public int row; // 当前小块图片相对原图处于哪一行
        public int column; // 当前小块图片相对原图处于哪一列
        public Bitmap bitmap; // 当前小块图片的位图
    }

    /***
     * 将原图切分成众多小块图片，根据原图的宽高和cropBitmapSize来决定分成多少小块
     * @param bitmap 待拆分的位图
     * @return 返回切割后的小块位图列表
     */
    private ArrayList<SplitBitmap> splitBitmap(Bitmap bitmap) {
        // 获取原图的宽高
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        // 原图宽高除以cropBitmapSize，得到应该将图片的宽和高分成几部分
        float splitFW = (float)width / cropBitmapSize;
        float splitFH = (float)height / cropBitmapSize;
        int splitW = (int)(splitFW);
        int splitH = (int)(splitFH);
        // 用来存放切割以后的小块图片的信息
        ArrayList<SplitBitmap> splitedBitmaps = new ArrayList<SplitBitmap>();
        Log.e(TAG, "width:" + width + " height:" + height);
        Log.e(TAG, "splitW:" + splitW + " splitH:" + splitH);
        Log.e(TAG, "splitFW:" + splitFW + " splitFH:" + splitFH);

        //对图片进行切割
        if (splitFW < 1.2 && splitFH < 1.2) {
            // 直接计算整张图
            SplitBitmap sb = new SplitBitmap();
            sb.row = 0;
            sb.column = 0;
            sb.bitmap = bitmap;
            splitedBitmaps.add(sb);
        } else if (splitFW < 1.2 && splitFH > 1) {
            // 仅在高度上拆分
            for (int i = 0; i < splitH; i++) {
                SplitBitmap sb = new SplitBitmap();
                sb.row = i;
                sb.column = 0;
                if (i == splitH - 1) {
                    sb.bitmap = Bitmap.createBitmap(bitmap, 0, i * cropBitmapSize, cropBitmapSize, height - i * cropBitmapSize, null, false);
                }else {
                    sb.bitmap = Bitmap.createBitmap(bitmap, 0, i * cropBitmapSize, cropBitmapSize, cropBitmapSize, null, false);
                }
                splitedBitmaps.add(sb);
            }
        } else if (splitFW > 1 && splitFH < 1.2) {
            // 仅在宽度上拆分
            for (int i = 0; i < splitW; i++) {
                SplitBitmap sb = new SplitBitmap();
                sb.row = 0;
                sb.column = i;
                if (i == splitW - 1) {
                    sb.bitmap = Bitmap.createBitmap(bitmap, i * cropBitmapSize, 0, cropBitmapSize, width - i * cropBitmapSize, null, false);
                }else {
                    sb.bitmap = Bitmap.createBitmap(bitmap, i * cropBitmapSize, 0, cropBitmapSize, cropBitmapSize, null, false);
                }

                splitedBitmaps.add(sb);
            }
        } else {
            // 在高度和宽度上都拆分
            for (int i = 0; i < splitH; i++) {
                for (int j = 0; j < splitW; j++) {
                    int lastH = cropBitmapSize;
                    int lastW = cropBitmapSize;
                    // 最后一行的高度
                    if (i == splitH - 1) {
                        lastH = height - i * cropBitmapSize;
//                        Log.e(TAG, "lastH:" +lastH);
                    }
                    // 最后一列的宽度
                    if (j == splitW - 1) {
                        lastW = width - j * cropBitmapSize;
//                        Log.e(TAG, "lastW:" +lastW);
                    }
//                    Log.e(TAG, "lastH:" + lastH + " lastW:" + lastW +
//                            " bitmapH:" + bitmap.getHeight() + " bitmapW:" + bitmap.getWidth() +
//                            " i * cropBitmapSize:" + i * cropBitmapSize + " j * cropBitmapSize:" + j * cropBitmapSize +
//                            " i:" + i + " j:" + j
//                    );

                    SplitBitmap sb = new SplitBitmap();
                    // 记录当前小块图片所处的行列
                    sb.row = i;
                    sb.column = j;
                    // 获取当前小块的位图
                    sb.bitmap = Bitmap.createBitmap(bitmap, j * cropBitmapSize, i * cropBitmapSize, lastW, lastH, null, false);
                    splitedBitmaps.add(sb);
                }
            }
        }

        return splitedBitmaps;
    }

    /***
     * 合并小块位图列表为一个大的位图
     * @param splitedBitmaps 待合并的小块位图列表
     * @return 返回合并后的大的位图
     */
    private Bitmap mergeBitmap(ArrayList<SplitBitmap> splitedBitmaps) {
        int mergeBitmapWidth = 0;
        int mergeBitmapHeight = 0;
        // 遍历位图列表，根据行和列的信息，计算出合并后的位图的宽高
        for (SplitBitmap sb : splitedBitmaps) {
//            Log.e(TAG, "sb.column:" + sb.column + " sb.row:" + sb.row + " sb.bitmap.getHeight():" + sb.bitmap.getHeight() + " sb.bitmap.getWidth():" + sb.bitmap.getWidth());
            if (sb.row == 0) {
                mergeBitmapWidth += sb.bitmap.getWidth();
            }
            if (sb.column == 0) {
                mergeBitmapHeight += sb.bitmap.getHeight();
            }
        }

        Log.e(TAG, "splitedBitmaps: " + splitedBitmaps.size() + " mergeBitmapWidth:" + mergeBitmapWidth + " mergeBitmapHeight:" + mergeBitmapHeight);
        // 根据宽高创建合并后的空位图
        Bitmap mBitmap = Bitmap.createBitmap(mergeBitmapWidth, mergeBitmapHeight, Bitmap.Config.ARGB_8888);

        // 创建画布，我们将在画布上拼接新的大位图
        Canvas canvas = new Canvas(mBitmap);

        // 计算位图列表的长度
        int splitedBitmapsSize = splitedBitmaps.size();

        //lastRowSB记录上一行的第一列的数据，主要用来判断当前行是否最后一行，因为最后一行之前的所有行的高度都是一致的
        SplitBitmap lastColumn0SB = null;

        for (int i = 0; i < splitedBitmapsSize; i++) {
            // 获取当前小块信息
            SplitBitmap sb = splitedBitmaps.get(i);
            // 根据当前小块所处的行列和宽高计算小块应处于大位图中的位置
            int left = sb.column * sb.bitmap.getWidth();
            int top = sb.row * sb.bitmap.getHeight();
            int right = left + sb.bitmap.getWidth();
            int bottom = top + sb.bitmap.getHeight();

            // 最后一列
            // 根据计算下一个小块位图的列数是否为0判断当前小块是否是最后一列
            if (i != 0 && i < splitedBitmapsSize - 1 && splitedBitmaps.get(i + 1).column == 0) {
                // 因为最后一列的宽度不确定，所以，要根据上一小块的宽高来计算当前小块在大位图中的起始位置
                SplitBitmap lastBitmap = splitedBitmaps.get(i - 1);
                left = sb.column * lastBitmap.bitmap.getWidth();
                top = sb.row * lastBitmap.bitmap.getHeight();
                right = left + sb.bitmap.getWidth();
                bottom = top + sb.bitmap.getHeight();
            }

            //最后一行
            // 根据对比上一行中的高度来计算当前行是否最后一行，因为最后一行前的所有行的高度都是一致的
            if (i != 0 && i < splitedBitmapsSize && lastColumn0SB != null && splitedBitmaps.get(i).bitmap.getHeight() != lastColumn0SB.bitmap.getHeight()) {
//                Log.e(TAG, "---------------");
                // 如果最后一行的高度和之前行的高度不一致，那么就要根据上一行中的高度来重新计算当前行的起始位置
                SplitBitmap lastColumnBitmap = lastColumn0SB;
                left = sb.column * lastColumnBitmap.bitmap.getWidth();
                top = sb.row * lastColumnBitmap.bitmap.getHeight();
                right = left + sb.bitmap.getWidth();
                bottom = top + sb.bitmap.getHeight();
            } else if (sb.column == 0) {
                // 记录上一行的第一个列的小块信息
                lastColumn0SB = sb;
            }

            // 这个是当前小块的信息
            Rect srcRect = new Rect(0, 0, sb.bitmap.getWidth(), sb.bitmap.getHeight());
            // 这个是当前小块应该在大图中的位置信息
            Rect destRect = new Rect(left, top, right, bottom);
            // 将当前小块画到大图中
            canvas.drawBitmap(sb.bitmap, srcRect, destRect, null);
            // 这个是为了调试而画的框
//            canvas.drawRect(destRect, boxPaint);
//            Log.e(TAG,"I:" + i + " col:" + sb.column + " row:" + sb.row + " width:" + sb.bitmap.getWidth() + " height:" + sb.bitmap.getHeight());
        }

        return mBitmap;
    }

    public void addSRProgressCallback(final SRProgressCallback callback) {
        this.callback = callback;
    }

    public interface SRProgressCallback {
        public void callback(int progress);
    }
}
