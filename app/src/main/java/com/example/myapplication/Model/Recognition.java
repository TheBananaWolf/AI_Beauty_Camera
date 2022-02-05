package com.example.myapplication.Model;

import android.graphics.RectF;

public class Recognition {

    private final String id;
    private final String title;
    private final Float confidence;
    private RectF location;//bottom left right top

    public Recognition(final String id, final String title, final Float confidence, final RectF location){
        this.id = id;
        this.title = title;
        this.confidence = confidence;
        this.location = location;
    }

    public String getId(){

        return id;
    }
    public String getTitle(){

        return title;
    }
    public Float getConfidence(){

        return confidence;
    }
    public RectF getLocation(){

        return location;
    }

}
