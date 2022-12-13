package com.example.myapplication.Model;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Environment;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectDetection {

    private static final int NUM_DETECTIONS = 10; //只展示前10个检测出来的
    private static final float IMAGE_MEAN = 128.0f;//均值
    private static final float IMAGE_STD = 128.0f;//标准化
    private final String TAG = "ObjectDetection";
    private final int[] ddims = {1, 3, 300, 300};//根据自己的实际情况修改
    private final float[][][] outputLocations = new float[1][NUM_DETECTIONS][4];//默认保留的前10个检测到的物体的坐标;
    private final float[][] outputClasses = new float[1][NUM_DETECTIONS];//10个目标属于的类别;
    private final float[][] outputScores = new float[1][NUM_DETECTIONS];//10个目标的概率值;
    private final float[] numDetections = new float[1];//实际检测出来的物体的个数
    //目标检测模型的输出是多节点输出  根据tflite的runForMultipleInputsOutputs函数  声明并初始化 outputMap  用来接收输出数据
    private final Map<Integer, Object> outputMap = new HashMap() {
        {
            put(0, outputLocations);
            put(1, outputClasses);
            put(2, outputScores);
            put(3, numDetections);
        }
    };
    private Interpreter tflite;
    private List<String> labelList;
    private Recognition[] recognitions = null;

    //-------------------------------------------加载目标检测模型-----------------------------------------
    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd("detect.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public boolean load_model(Activity activity) {
        try {
            Interpreter.Options tfliteOptions = new Interpreter.Options();
            tfliteOptions.setNumThreads(4);
            tflite = new Interpreter(loadModelFile(activity), tfliteOptions);
            labelList = loadLabelList(activity);//加载标签  可以单独作为一个方法去调用
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    //加载标签
    private List<String> loadLabelList(Activity activity) throws IOException {
        List<String> labelList = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(activity.getAssets().open("labelmap.txt")));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }
//-------------------------------------------加载目标检测模型-----------------------------------------

    //-----------------------------------------输入数据预处理--------------------------------------------
    //与图像分类的预处理工作高度相似  把一张图片的三个通道每个通道的像素点数据存入ByteBuffer类型的 imgData
    private ByteBuffer getInputData(Bitmap bitmap) {
        ByteBuffer imgData = ByteBuffer.allocateDirect(ddims[0] * ddims[1] * ddims[2] * ddims[3] * 4);
        imgData.order(ByteOrder.nativeOrder());
        int[] intValues = new int[ddims[2] * ddims[3]];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < ddims[2]; ++i) {
            for (int j = 0; j < ddims[3]; ++j) {
                final int val = intValues[pixel++];
//                imgData.putFloat((((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
//                imgData.putFloat((((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
//                imgData.putFloat(((val & 0xFF) - IMAGE_MEAN) / IMAGE_STD);

                imgData.putFloat(((val >> 16) & 0xFF) * 2.0f / 255.0f - 1.0f);
                imgData.putFloat(((val >> 8) & 0xFF) * 2.0f / 255.0f - 1.0f);
                imgData.putFloat((val & 0xFF) * 2.0f / 255.0f - 1.0f);
            }
        }
        return imgData;

    }
//-------------------------------------------输入数据预处理----------------------------------------

    //-----------------------------------推理预测函数-------------------------------------------------------------
    public Bitmap predict_image(Bitmap oribitmap) {
        Bitmap resizedbitmap = Bitmap.createScaledBitmap(oribitmap, ddims[2], ddims[3], false);//resize
        ByteBuffer imgData = getInputData(resizedbitmap);//输入数据预处理
        Object[] inputArray = {imgData};//runForMultipleInputsOutput函数的输入是一个Object类型的数组
        tflite.runForMultipleInputsOutputs(inputArray, outputMap);//运行模型
        recognize();//后处理
        Bitmap rectBitmap = drawRect(resizedbitmap, oribitmap);//后处理
        return rectBitmap;
    }

    //-----------------------------------推理预测函数-------------------------------------------------------------
//-------------------------------------------模型释放与关闭------------------------------------------------------
    public void close() {

        tflite.close();

    }

    //-------------------------------------------模型释放与关闭------------------------------------------------------
//-----------------------------------输出数据后处理------------------------------------------------------------
    //此处后处理的目的是得到一张用有颜色的框框出检测到的物体的图片
    private Bitmap drawRect(Bitmap resizedbitmap, Bitmap oribitmap) {
        Bitmap boxImage = null;
        int oriImageH = oribitmap.getHeight();//原始图片的高度
        int oriImageW = oribitmap.getWidth();//原始图片的宽度
        Bitmap mutableBitmap = oribitmap.copy(Bitmap.Config.ARGB_8888, true);//复制原始图片
        Canvas canvas = new Canvas(mutableBitmap);//新建画布对象
        Paint paint = new Paint();//新建画笔对象
        paint.setTextSize((int) (200));//字体大小
        paint.setShadowLayer(1f, 0f, 1f, Color.DKGRAY);//阴影参数
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);//颜色设置
        //不填充
        paint.setStrokeWidth(10);  //空心线的宽度


        for (Recognition recognition : recognitions) {
            if (recognition.getConfidence() > 0.5) {//当物体所属概率值大于0.4时 才做进一步处理
                float scaleH = (float) oriImageH / resizedbitmap.getHeight();//计算高度的缩放比例
                float scaleW = (float) oriImageW / resizedbitmap.getWidth(); //计算宽度的缩放比例
                int ori_left = (int) (scaleW * recognition.getLocation().left);//原始图片上对应的真实的left坐标
                int ori_top = (int) (scaleH * recognition.getLocation().top);//原始图片上对应的真实的top坐标
                int ori_right = (int) (scaleW * recognition.getLocation().right);//原始图片上对应的真实的right坐标
                int ori_bottom = (int) (scaleH * recognition.getLocation().bottom);//原始图片上对应的真实的bottom坐标


                Double id = Double.valueOf(recognition.getId());//被检测物体的类别ID
                int ID = (int) Math.ceil(id);
                String label = recognition.getTitle();//被检测物体的标签值
                System.out.println(label);

                Rect bounds = new Rect();//构造矩形框对象
                paint.getTextBounds(label, 0, label.length(), bounds);
                canvas.drawText(ID + ":" + label, ori_left, ori_top - 50, paint);
                canvas.drawRect(ori_left, ori_top, ori_right, ori_bottom, paint);

                // 创建类别文件夹
                Log.v(TAG + "yhl create category:", recognition.getTitle());
                boolean result = createAlbum(recognition.getTitle(), oribitmap);
            }
        }
        boxImage = mutableBitmap;
        return boxImage; //返回写入文字和有颜色的检测矩形框的原始图片
    }

    //对输出结果中检测到的物体作进一步的处理 将outputMap中的数据取出来 变换处理 存入recognitions数组中
    private void recognize() {
        float n = ((float[]) outputMap.get(3))[0];//检测出来的物体个数
        //将所有的识别结果存入了Recognition类型的数组中
        recognitions = new Recognition[(int) n];//数组的长度也就是检测出的物体的个数
        for (int i = 0; i < n; ++i) {
            //RectF类  构造函数 left top right bottom  因此要将outputMap 中的坐标按照对应关系存入
            //outputLocations 中每个检测框的坐标顺序是  0:top  1:left  2:bootm   3:right
            RectF rectF = new RectF(ddims[2] * ((float[][][]) outputMap.get(0))[0][i][1], ddims[3] * ((float[][][]) outputMap.get(0))[0][i][0], ddims[2] * ((float[][][]) outputMap.get(0))[0][i][3], ddims[3] * ((float[][][]) outputMap.get(0))[0][i][2]);
            //Recognition 构造函数 四个参数分别 物体所属类别索引 类别标签 类别对应的概率  检测物体的左上角和右下角坐标值
            recognitions[i] = new Recognition("" + ((float[][]) outputMap.get(1))[0][i], labelList.get((int) ((float[][]) outputMap.get(1))[0][i]), ((float[][]) outputMap.get(2))[0][i], rectF);
        }
    }
//-----------------------------------输出数据后处理------------------------------------------------------------

    private boolean createAlbum(String n, Bitmap bitmap) {
        boolean ret = false;
//        String Album_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Album" + File.separator +n + File.separator;
        String Album_ROOT = Environment.getExternalStorageDirectory() + "/Album/" + n;
        File localFile = new File(Album_ROOT);
        if (!localFile.exists()) {
            Log.v(TAG + "yhl mkdir Album Success", localFile.toString());
            localFile.mkdir();

            if (!localFile.mkdirs()) {
                Log.v(TAG + "yhl Album Exists", Album_ROOT);
            }
        }

        File finalImageFile = new File(localFile, System.currentTimeMillis() + ".png");
        try {
            final FileOutputStream out = new FileOutputStream(finalImageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            ret = true;
        } catch (final Exception e) {
            Log.v(TAG + "yhl createAlbum:", "error!");
        }
        return ret;
    }

}
