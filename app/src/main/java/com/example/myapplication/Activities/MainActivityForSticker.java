package com.example.myapplication.Activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Model.StickerView;
import com.example.myapplication.Model.StickerViewLayout;
import com.example.myapplication.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

public class MainActivityForSticker extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_FOR_PICTURE = 1;

    public ImageView img;
    private Context mContext;
    private TextView saveBtn;
    private StickerViewLayout mStickerLayout;
    private TextView decorateType;
    private Bitmap src;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = MainActivityForSticker.this;

        saveBtn = (TextView) findViewById(R.id.save);
        img = (ImageView) findViewById(R.id.src);
        src = imageProcessing.getSRC();
        img.setImageBitmap(src);
        // 贴图容器
        mStickerLayout = (StickerViewLayout) findViewById(R.id.sticker_layout);
        decorateType = (TextView) findViewById(R.id.type_decorate);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 跳转至素材界面
        decorateType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent();
                i.setClass(mContext, MaterialActivity.class);
                startActivityForResult(i, REQUEST_FOR_PICTURE);
                Log.v("wangguanjie", "te");
                overridePendingTransition(R.anim.push_up, 0);
            }
        });

        // 保存图片
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LinkedList<StickerView> materialList = mStickerLayout.getStickerViewList();
                Log.v(TAG, "count=" + materialList.size());
                if (materialList.size() == 0) {
                    Toast.makeText(mContext, "请先添加素材！", Toast.LENGTH_SHORT).show();
                    return;
                }
                Bitmap tempBmp = Bitmap.createBitmap(src);


                for (StickerView effectView : materialList) {
                    tempBmp = createBitmap(tempBmp,
                            BitmapFactory.decodeFile(effectView.getImgPath()),
                            effectView.getCenterPoint(), effectView.getDegree(), effectView.getScaleValue());
                    Log.v(TAG, "effectView.getScaleValue()=" + effectView.getScaleValue());
                }

                saveMyBitmap(tempBmp);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FOR_PICTURE && resultCode == RESULT_OK) {
            String imgPath = data.getStringExtra(MaterialActivity.MATERIAL_PATH);
            // 贴图容器中心点
            int centerX = (img.getLeft() + img.getRight()) / 2;
            int centerY = (img.getTop() + img.getBottom()) / 2;
            Log.v(TAG, "centerX=" + centerX + "  centerY=" + centerY);

            StickerView view = new StickerView(mContext, imgPath);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            view.setLayoutParams(params);
            mStickerLayout.addView(view);
        }
    }

    // 图片合成
    private Bitmap createBitmap(Bitmap src, Bitmap dst, float[] centerPoint, float degree, float scaleValue) {
        if (src == null) {
            return null;
        }
        int w = src.getWidth();
        int h = src.getHeight();
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        int width = metric.widthPixels;     // 屏幕宽度（像素）
        float scale = (float) w / (float) width;

        // 素材原始宽高(像素)
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();

        // 定义矩阵对象
        Matrix matrix = new Matrix();
        matrix.postScale(scaleValue * scale, scaleValue * scale);

        // 素材起始位置(左上角)
        float Ltx = (centerPoint[0] - img.getLeft() - dstWidth * scaleValue / 2) * scale;
        float Lty = (centerPoint[1] - img.getTop() - dstHeight * scaleValue / 2) * scale;

        Bitmap newb = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);//创建一个新的和SRC长度宽度一样的位图
        Canvas cv = new Canvas(newb);
        cv.drawBitmap(src, 0, 0, null);//在 0，0坐标开始画入src
        cv.save();
        cv.rotate(degree, centerPoint[0] * scale, centerPoint[1] * scale);
        Bitmap dstbmp = Bitmap.createBitmap(dst, 0, 0, dst.getWidth(), dst.getHeight(),
                matrix, true);
        cv.drawBitmap(dstbmp, Ltx, Lty, null);//在src画贴图
        cv.restore();//存储
        return newb;
    }

    // 保存图片到手机
    public void saveMyBitmap(Bitmap mBitmap) {
        //获取当前时间，进一步转化为字符串
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy" + "_" + "MM" + "_" + "dd" + "_" + "HH" + "_" + "mm" + "_" + "ss");
        String bmpName = format.format(date);

        File f = new File(this.getExternalFilesDir(null).getPath() + "/" + bmpName + ".jpg");
        if (f.exists()) {
            f.delete();
        }
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);

        mBitmap.recycle();
        try {
            fOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Toast.makeText(mContext, "已保存:" + f.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        Log.v("wangguanjie", f.getAbsolutePath());
    }

    public void setSrc(Bitmap src) {
        this.src = src;
    }
}
