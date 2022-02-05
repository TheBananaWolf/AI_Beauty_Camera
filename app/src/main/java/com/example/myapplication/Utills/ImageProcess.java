package com.example.myapplication.Utills;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;
import com.nostra13.universalimageloader.utils.L;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

public class ImageProcess {
    private PointF leftEyePos;
    private PointF rightEyePos;
    private PointF center;
    private PointF leftCheck;
    private List<PointF> rightcheck;
    private List<PointF> faceCont;
    static {
        System.loadLibrary("opencv_java3");
    }
    private volatile Semaphore mSemaphore = new Semaphore(0);

    private int colordodge(int A, int B) {
        return Math.min(A + (A * B) / (255 - B + 1), 255);
    }

    //灰度化方法
    public Bitmap Gray(Bitmap photo) {
        Mat RGBMat = new Mat();
        Bitmap grayBitmap = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.RGB_565);
        Utils.bitmapToMat(photo, RGBMat);//convert original bitmap to Mat, R G B.
        Imgproc.cvtColor(RGBMat, RGBMat, Imgproc.COLOR_RGB2GRAY);//rgbMat to gray grayMat
        Utils.matToBitmap(RGBMat, grayBitmap);
        return grayBitmap;
    }

    //素描滤镜
    public Bitmap Sketch(Bitmap photo) {
        Mat SM = new Mat();
        Mat SM1 = new Mat();
        Bitmap sumiaoMap = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.ARGB_8888);
        Bitmap SMB = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.ARGB_8888);
        Bitmap SMB1 = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.ARGB_8888);
        Utils.bitmapToMat(photo, SM);
        //灰度化
        Imgproc.cvtColor(SM, SM, Imgproc.COLOR_RGB2GRAY);
        //颜色取反
        Core.bitwise_not(SM, SM1);
        //高斯模糊
        Imgproc.GaussianBlur(SM1, SM1, new Size(13, 13), 0, 0);
        Utils.matToBitmap(SM, SMB);
        Utils.matToBitmap(SM1, SMB1);
        for (int i = 0; i < SMB.getWidth(); i++) {
            for (int j = 0; j < SMB.getHeight(); j++) {
                int A = SMB.getPixel(i, j);
                int B = SMB1.getPixel(i, j);
                int CR = colordodge(Color.red(A), Color.red(B));
                int CG = colordodge(Color.green(A), Color.red(B));
                int CB = colordodge(Color.blue(A), Color.blue(B));
                sumiaoMap.setPixel(i, j, Color.rgb(CR, CG, CB));
            }
        }
        return sumiaoMap;
    }

    //二值化滤镜
    public Bitmap Binarization(Bitmap photo) {
        Mat mat = new Mat();
        Bitmap thes = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.ARGB_8888);
        Utils.bitmapToMat(photo, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);
        //Imgproc.GaussianBlur(mat,mat,new Size(13,13),0,0);
        //Imgproc.Canny(mat,mat,70,210);
        Core.bitwise_not(mat, mat);
        Imgproc.threshold(mat, mat, 100, 255, Imgproc.THRESH_BINARY_INV);
        Utils.matToBitmap(mat, thes);
        return thes;
    }

    //莫皮
    public Bitmap mopi(Bitmap photo) {
        Mat mat = new Mat();
        Mat det = new Mat();
        Bitmap thes = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.ARGB_8888);
        Utils.bitmapToMat(photo, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2RGB);
        Imgproc.medianBlur(mat, mat, 3);
        Imgproc.bilateralFilter(mat, det, 40, 50, 30);
        Mat kx = new Mat(3, 3, CvType.CV_32FC1);
        float[] robert_x = new float[]{0, -1, 0, -1, 5, -1, 0, -1, 0};
        kx.put(0, 0, robert_x);
        Imgproc.filter2D(det, det, -1, kx);
        Utils.matToBitmap(det, thes);
        return thes;
    }

    //轮廓
    public Bitmap Contour(Bitmap photo) {
        Mat mat = new Mat();
        Mat Cmat = new Mat();
        Mat Bmat = new Mat();
        Bitmap cartton = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.ARGB_8888);
        Utils.bitmapToMat(photo, mat);
        Imgproc.Canny(mat, Cmat, 50, 100);
        Core.bitwise_not(Cmat, Cmat);
        Utils.matToBitmap(Cmat, cartton);
        return cartton;
    }

    //怀旧色滤镜
    public Bitmap Nostalgic(Bitmap photo) {
        Bitmap huaijiu = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.ARGB_8888);
        for (int i = 0; i < photo.getWidth(); i++) {
            for (int j = 0; j < photo.getHeight(); j++) {
                int A = photo.getPixel(i, j);
                int AR = (int) (0.393 * Color.red(A) + 0.769 * Color.green(A) + 0.189 * Color.blue(A));
                int AG = (int) (0.349 * Color.red(A) + 0.686 * Color.green(A) + 0.168 * Color.blue(A));
                int AB = (int) (0.272 * Color.red(A) + 0.534 * Color.green(A) + 0.131 * Color.blue(A));
                AR = AR > 255 ? 255 : AR;
                AG = AG > 255 ? 255 : AG;
                AB = AB > 255 ? 255 : AB;
                huaijiu.setPixel(i, j, Color.rgb(AR, AG, AB));
            }
        }
        return huaijiu;
    }

    //连环画滤镜
    public Bitmap Comic_strip(Bitmap photo) {
        Bitmap lianhuanhua = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.ARGB_8888);
        for (int i = 0; i < photo.getWidth(); i++) {
            for (int j = 0; j < photo.getHeight(); j++) {
                int A = photo.getPixel(i, j);
                int AR = Math.abs(Color.red(A) - Color.blue(A) + Color.green(A) + Color.green(A)) * Color.red(A) / 256;
                int AG = Math.abs(Color.red(A) - Color.green(A) + Color.blue(A) + Color.blue(A)) * Color.red(A) / 256;
                int AB = Math.abs(Color.red(A) - Color.blue(A) + Color.blue(A) + Color.blue(A)) * Color.green(A) / 256;
                AR = AR > 255 ? 255 : AR;
                AG = AG > 255 ? 255 : AG;
                AB = AB > 255 ? 255 : AB;
                lianhuanhua.setPixel(i, j, Color.rgb(AR, AG, AB));
            }
        }
        return lianhuanhua;
    }
    //熔铸滤镜
    public Bitmap Cast(Bitmap photo) {
        Bitmap rongzhu = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.ARGB_8888);
        for (int i = 0; i < photo.getWidth(); i++) {
            for (int j = 0; j < photo.getHeight(); j++) {
                int A = photo.getPixel(i, j);
                int AR = Color.red(A) * 128 / (Color.blue(A) + Color.green(A) + 1);
                int AG = Color.green(A) * 128 / (Color.blue(A) + Color.red(A) + 1);
                int AB = Color.blue(A) * 128 / (Color.red(A) + Color.green(A) + 1);
                AR = AR > 255 ? 255 : AR;
                AG = AG > 255 ? 255 : AG;
                AB = AB > 255 ? 255 : AB;
                rongzhu.setPixel(i, j, Color.rgb(AR, AG, AB));
            }
        }
        return rongzhu;
    }

    //冰冻滤镜
    public Bitmap Iced(Bitmap photo) {
        Bitmap bingdong = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.ARGB_8888);
        for (int i = 0; i < photo.getWidth(); i++) {
            for (int j = 0; j < photo.getHeight(); j++) {
                int A = photo.getPixel(i, j);
                int AR = (Color.red(A) - Color.blue(A) - Color.green(A)) * 3 / 2;
                int AG = (Color.green(A) - Color.blue(A) - Color.red(A)) * 3 / 2;
                int AB = (Color.blue(A) - Color.red(A) - Color.green(A)) * 3 / 2;
                AR = AR > 255 ? 255 : AR;
                AG = AG > 255 ? 255 : AG;
                AB = AB > 255 ? 255 : AB;
                bingdong.setPixel(i, j, Color.rgb(AR, AG, AB));
            }
        }
        return bingdong;
    }

    //浮雕滤镜
    public Bitmap Relief(Bitmap photo) {
        Bitmap bingdong = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.ARGB_8888);
        for (int i = 1; i < photo.getWidth() - 1; i++) {
            for (int j = 1; j < photo.getHeight() - 1; j++) {
                int A = photo.getPixel(i - 1, j - 1);
                int B = photo.getPixel(i + 1, j + 1);
                int AR = Color.red(B) - Color.red(A) + 128;
                int AG = Color.green(B) - Color.green(A) + 128;
                int AB = Color.blue(B) - Color.blue(A) + 128;
                AR = AR > 255 ? 255 : AR;
                AG = AG > 255 ? 255 : AG;
                AB = AB > 255 ? 255 : AB;
                bingdong.setPixel(i, j, Color.rgb(AR, AG, AB));
            }
        }
        return bingdong;
    }

    //扩散滤镜
    public Bitmap Diffuse(Bitmap photo) {
        Bitmap kuosan = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.ARGB_8888);
        int random, randomMAX = 10;
        for (int i = 0; i < photo.getWidth() - randomMAX; i++) {
            for (int j = 0; j < photo.getHeight() - randomMAX; j++) {
                random = (int) Math.random() * randomMAX;
                int AR = Color.red(photo.getPixel(i + random, j + random));
                int AG = Color.green(photo.getPixel(i + random, j + random));
                int AB = Color.blue(photo.getPixel(i + random, j + random));
                kuosan.setPixel(i, j, Color.rgb(AR, AG, AB));
            }
        }
        return kuosan;
    }

     //big eye
    public Bitmap BigEye(Bitmap photo){
        Bitmap kuosan = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.ARGB_8888);
        mSemaphore.release();
        Bitmap finalPhoto = photo;
        Runnable r1 = new Runnable() {
            public void run() {
                try {
                   mSemaphore.acquire();
                    Log.v("Facemesh","pre");
                    Facemesh(finalPhoto);
                    Log.v("Facemesh","mid");

                    Log.v("Facemesh","aft");

                }
                catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };
        Thread t1 = new Thread(r1);
        t1.start();

        try {
            t1.join();
            Log.v("leftEyePos","pre");
            mSemaphore.acquire();
            Log.v("leftEyePos","mid");

            if(rightEyePos==null||leftEyePos==null){
                Log.v("cannot get point","123456");
                photo=Diffuse(photo);

            }

            else {

               // photo=smallhead(photo, center, 100, 4);
                photo = magnifyEye(photo, rightEyePos, 40, 5);
                photo = magnifyEye(photo, leftEyePos, 40, 5);

                Log.v("magnifyEye", "after");

            }
            mSemaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return photo;
    }
    public void Facemesh(Bitmap bitmap) {
        Log.v("stage1","face point");

        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();


        InputImage image = InputImage.fromBitmap(bitmap, 0);

        FaceDetector detector = FaceDetection.getClient(options);

        Task<List<Face>> result = null;
        result = detector.process(image);

        try {
            Tasks.await(result);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        List<Face>temp=result.getResult();
        for (Face face : temp) {
            // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
            // nose available):
            FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
            FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
            FaceLandmark nose = face.getLandmark(FaceLandmark.NOSE_BASE);
            if (leftEye != null) {

                leftEyePos = leftEye.getPosition();
            }
            if (rightEye != null) {
                rightEyePos = rightEye.getPosition();
            }

            if( nose != null) {
                center = nose.getPosition();
                Log.v("nose", String.valueOf(nose.getPosition()));
            }

//            faceCont =face.getContour(FaceContour.FACE).getPoints();
//            Log.v("faceCont size ", String.valueOf(faceCont.size()));

        }
        mSemaphore.release();

    }
    /**
     *  眼睛放大算法
     * @param bitmap      原来的bitmap
     * @param centerPoint 放大中心点
     * @param radius      放大半径
     * @param sizeLevel    放大力度  [0,4]
     * @return 放大眼睛后的图片
     */
     public  Bitmap magnifyEye(Bitmap bitmap, PointF centerPoint, int radius, float sizeLevel) {

        Bitmap dstBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        int left = centerPoint.x - radius < 0 ? 0 : (int) (centerPoint.x - radius);
        int top = centerPoint.y - radius < 0 ? 0 : (int) (centerPoint.y - radius);
        int right = centerPoint.x + radius > bitmap.getWidth() ? bitmap.getWidth() - 1 : (int) (centerPoint.x + radius);
        int bottom = centerPoint.y + radius > bitmap.getHeight() ? bitmap.getHeight() - 1 : (int) (centerPoint.y + radius);
        int powRadius = radius * radius;

        int offsetX, offsetY, powDistance, powOffsetX, powOffsetY;

        int disX, disY;

        //当为负数时，为缩小
        float strength = (5 + sizeLevel * 2) / 10;

        for (int i = top; i <= bottom; i++) {
            offsetY = (int) (i - centerPoint.y);
            for (int j = left; j <= right; j++) {
                offsetX = (int) (j - centerPoint.x);
                powOffsetX = offsetX * offsetX;
                powOffsetY = offsetY * offsetY;
                powDistance = powOffsetX + powOffsetY;

                if (powDistance <= powRadius) {
                    double distance = Math.sqrt(powDistance);
                    double sinA = offsetX / distance;
                    double cosA = offsetY / distance;

                    double scaleFactor = distance / radius - 1;
                    scaleFactor = (1 - scaleFactor * scaleFactor * (distance / radius) * strength);

                    distance = distance * scaleFactor;
                    disY = (int) (distance * cosA + centerPoint.y + 0.5);
                    disY = checkY(disY, bitmap);
                    disX = (int) (distance * sinA + centerPoint.x + 0.5);
                    disX = checkX(disX, bitmap);
                    //中心点不做处理
                    if (!(j == centerPoint.x && i == centerPoint.y)) {
                        dstBitmap.setPixel(j, i, bitmap.getPixel(disX, disY));
                    }
                }
            }
        }
//        transfer = ue;

        return dstBitmap;
    }
    public  Bitmap smallhead(Bitmap bitmap, PointF centerPoint, int radius, float sizeLevel) {

        Bitmap dstBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        int left = centerPoint.x - radius < 0 ? 0 : (int) (centerPoint.x - radius);
        int top = centerPoint.y - radius < 0 ? 0 : (int) (centerPoint.y - radius);
        int right = centerPoint.x + radius > bitmap.getWidth() ? bitmap.getWidth() - 1 : (int) (centerPoint.x + radius);
        int bottom = centerPoint.y + radius > bitmap.getHeight() ? bitmap.getHeight() - 1 : (int) (centerPoint.y + radius);
        int powRadius = radius * radius;

        int offsetX, offsetY, powDistance, powOffsetX, powOffsetY;

        int disX, disY;

        //当为负数时，为缩小
        float strength = -(5 + sizeLevel * 2) / 10;

        for (int i = top; i <= bottom; i++) {
            offsetY = (int) (i - centerPoint.y);
            for (int j = left; j <= right; j++) {
                offsetX = (int) (j - centerPoint.x);
                powOffsetX = offsetX * offsetX;
                powOffsetY = offsetY * offsetY;
                powDistance = powOffsetX + powOffsetY;

                if (powDistance <= powRadius) {
                    double distance = Math.sqrt(powDistance);
                    double sinA = offsetX / distance;
                    double cosA = offsetY / distance;

                    double scaleFactor = distance / radius - 1;
                    scaleFactor = (1 - scaleFactor * scaleFactor * (distance / radius) * strength);

                    distance = distance * scaleFactor;
                    disY = (int) (distance * cosA + centerPoint.y + 0.5);
                    disY = checkY(disY, bitmap);
                    disX = (int) (distance * sinA + centerPoint.x + 0.5);
                    disX = checkX(disX, bitmap);
                    //中心点不做处理
                    if (!(j == centerPoint.x && i == centerPoint.y)) {
                        dstBitmap.setPixel(j, i, bitmap.getPixel(disX, disY));
                    }
                }
            }
        }
//        transfer = true;

        return dstBitmap;
    }
    private static int checkY(int disY, Bitmap bitmap) {
        if (disY < 0) {
            disY = 0;
        } else if (disY >= bitmap.getHeight()) {
            disY = bitmap.getHeight() - 1;
        }
        return disY;
    }

    private static int checkX(int disX, Bitmap bitmap) {
        if (disX < 0) {
            disX = 0;
        } else if (disX >= bitmap.getWidth()) {
            disX = bitmap.getWidth() - 1;
        }
        return disX;
    }
    /**
     * 瘦脸算法
     *
     * @param bitmap      原来的bitmap
     * @return 之后的图片
     */
    private static final int WIDTH = 200;
    private static final int HEIGHT = 200;
    public static Bitmap smallFaceMesh(Bitmap bitmap, List<PointF> faceCont, PointF centerPoint, int level) {
        //交点坐标的个数
        int COUNT = (WIDTH + 1) * (HEIGHT + 1);
        //用于保存COUNT的坐标
        float[] verts = new float[COUNT * 2];
        float[] org = new float[COUNT * 2];
        float bmWidth = bitmap.getWidth();
        float bmHeight = bitmap.getHeight();

        int index = 0;
        for (int i = 0; i < HEIGHT + 1; i++) {
            float fy = bmHeight * i / HEIGHT;
            for (int j = 0; j < WIDTH + 1; j++) {
                float fx = bmWidth * j / WIDTH;
                //X轴坐标 放在偶数位
                org[index*2]=verts[index * 2] = fx;
                //Y轴坐标 放在奇数位
                org[index*2+1]=verts[index * 2 + 1] = fy;
                index += 1;
            }
        }
        int r = 180 + 15 * level;

        warp(verts,faceCont.get(21).x,faceCont.get(21).y,centerPoint.x,centerPoint.y,r);
        for(int i=0;i<COUNT*2;i++){
            if(org[i]!=verts[i]){
                Log.v("org different verts", String.valueOf(i));
            }
        }
//        warp(verts,faceCont.get(27).x,faceCont.get(27).y,centerPoint.x,centerPoint.y,r);
//
//        warp(verts,faceCont.get(15).x,faceCont.get(15).y,centerPoint.x,centerPoint.y,r);
//        warp(verts,faceCont.get(8).x,faceCont.get(8).y,centerPoint.x,centerPoint.y,r);


        Bitmap resultBitmap = Bitmap.createBitmap(bitmap.getWidth(),bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resultBitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);       //testing parameters
        paint.setFilterBitmap(true);    //testing parameters

        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawBitmapMesh(bitmap,WIDTH, HEIGHT,verts,0,null,0,paint);
        //canvas.drawBitmap(bitmap,0,0,null);
        Log.v("smallFaceMesh","done");
        return resultBitmap;
    }
    private static void warp(float verts[],float startX, float startY, float endX, float endY,int r) {
        //level [0,4]

        //int r = 200; default 200

        //计算拖动距离
        float ddPull = (endX - startX) * (endX - startX) + (endY - startY) * (endY - startY);
        float dPull = (float) Math.sqrt(ddPull);
        //Log.v("Pull_dis", String.valueOf(dPull));
        //dPull = screenWidth - dPull >= 0.0001f ? screenWidth - dPull : 0.0001f;
        if(dPull < 2 * r){
            dPull = 2 * r;
        }
        //Log.v("Pull_dis", String.valueOf(dPull));
        int powR = r * r;
        int index = 0;
        int offset = 1;
        for (int i = 0; i < HEIGHT + 1; i++) {
            for (int j = 0; j < WIDTH + 1; j++) {
                //边界区域不处理
                if(i < offset || i > HEIGHT - offset || j < offset || j > WIDTH - offset){
                    //Log.v("itom", String.valueOf(i)+" "+String.valueOf(j));

                    index = index + 1;
                    continue;
                }
                //计算每个坐标点与触摸点之间的距离
                float dx = verts[index * 2] - startX;
                float dy = verts[index * 2 + 1] - startY;
                float dd = dx * dx + dy * dy;
                //Log.v("determine", String.valueOf((dd < powR)));
                if (dd < powR) {
                    //变形系数，扭曲度
                    double e = (powR - dd) * (powR - dd) / ((powR - dd + dPull * dPull) * (powR - dd + dPull * dPull));
                    //Log.v("etom",String.valueOf(e));
                    double pullX = e * (endX - startX);
                    double pullY = e * (endY - startY);
                    verts[index * 2] = (float) (verts[index * 2] + pullX);
                    verts[index * 2 + 1] = (float) (verts[index * 2 + 1] + pullY);
                }
                index = index + 1;
            }
        }
    }



}
