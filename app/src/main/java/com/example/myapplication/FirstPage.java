package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Activities.homePageForCanmera;
import com.example.myapplication.Activities.imageProcessing;
import com.example.myapplication.Utills.PermissionUtils;


public class FirstPage extends AppCompatActivity {
    private Button image;
    private Button canmera;

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("wangguanjie", "onStart");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("wangguanjie", "onRestart");
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.d("wangguanjie", "onPause");
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.d("wangguanjie", "onResume");
    }
    @Override
    protected void onStop() {
        super.onStop();
        Log.d("wangguanjie", "onStop");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        Log.d("wangguanjie", "onSaveInstanceState");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d("wangguanjie", "onRestoreInstanceState");

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose);
        PermissionUtils.verifyStoragePermissions(this);
        image = findViewById(R.id.Image);
        canmera = findViewById(R.id.Vedio);
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), imageProcessing.class));
            }
        });
        canmera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), homePageForCanmera.class));
            }
        });

    }
}
