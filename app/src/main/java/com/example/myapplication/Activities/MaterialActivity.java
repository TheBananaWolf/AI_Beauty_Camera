package com.example.myapplication.Activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import com.example.myapplication.Adapter.MaterialAdapter;
import com.example.myapplication.R;
import com.example.myapplication.Utills.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author chenkaijian
 * 贴图素材显示界面
 */
public class MaterialActivity extends Activity {

    public static final String MATERIAL_PATH = "material_path";

    private final int materialType = 1;// 贴图类别

    private SharedPreferences preferences = null;


    private TextView cancel;
    private GridView gv;

    private ArrayList<HashMap<String, String>> packageList;// 素材包名集合
    private MaterialAdapter adapter;
    private ArrayList<HashMap<String, String>> materialList;// 素材图片集合

    @SuppressLint("LongLogTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_material);

        preferences = getSharedPreferences("material_default_value", MODE_PRIVATE);


        // 取消按钮
        cancel = (TextView) findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // 第一次进入应用时解压默认素材包
        if (preferences.getBoolean("isFirst", true)) {
            // 解压素材包
            Utils.unZipMaterials(MaterialActivity.this, "wangguanjie", materialType);

            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isFirst", false);
            editor.apply();
        }
        Log.v("wangguanjie", String.valueOf(preferences.getBoolean("isFirst", true)));


        // 保存素材图片集合
        materialList = new ArrayList<HashMap<String, String>>();
        File dir = getExternalFilesDir(null);
        File file = new File(dir.getAbsolutePath() + "/" + Utils.getMaterialDescription(materialType) + "/" + "wangguanjie" + "/materials.xml");
        try {
            InputStream in = new FileInputStream(file);
            ArrayList<HashMap<String, String>> tempList = Utils.parseXML(in, "wangguanjie");
            for (int j = 0; j < tempList.size(); j++) {
                materialList.add(tempList.get(j));
                Log.v("wangguanjie count", String.valueOf(tempList.get(j)));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


        // 素材显示
        gv = (GridView) findViewById(R.id.gridView);
        gv.setSelector(new ColorDrawable(Color.TRANSPARENT));
        Log.v("wangguanjie materialList", String.valueOf(materialList.size()));
        adapter = new MaterialAdapter(this, materialList);
        gv.setAdapter(adapter);
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Intent intent = new Intent();
                intent.putExtra(MATERIAL_PATH, getExternalFilesDir(null).getAbsolutePath() + "/" + Utils.getMaterialDescription(materialType) + "/" + materialList.get(position).get("name"));
                setResult(RESULT_OK, intent);

                adapter.getItem(position);
                finish();
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        this.overridePendingTransition(0, R.anim.push_bottom);// 关闭窗体动画显示
    }

}
