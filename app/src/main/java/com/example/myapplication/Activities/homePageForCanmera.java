package com.example.myapplication.Activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.Activities.HumanOpenCVStream;
import com.example.myapplication.Activities.MainActivityForFaceBeauty;
import com.example.myapplication.Activities.MainActivityAR;
import com.example.myapplication.Activities.MainActivityForObject;
import com.example.myapplication.Activities.MainActivityForSuperResolution;
import com.example.myapplication.R;
import com.example.myapplication.Utills.PermissionUtils;

public class homePageForCanmera extends AppCompatActivity {
    Button transfer;
    Spinner s;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        transfer = findViewById(R.id.transfer);
        String[] arraySpinner = new String[] {
                "Object Detection","AR Filter","Background Replacement"
        };
        s = findViewById(R.id.function);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, arraySpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);

        transfer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = s.getSelectedItem().toString();
                if(text.equals("Object Detection"))
                    startActivity(new Intent(getApplicationContext(), MainActivityForObject.class));
                if(text.equals("AR Filter"))
                    startActivity(new Intent(getApplicationContext(), MainActivityAR.class));
                if(text.equals("Background Replacement"))
                    startActivity(new Intent(getApplicationContext(), HumanOpenCVStream.class));
            }
        });
    }

}