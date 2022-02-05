package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
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
