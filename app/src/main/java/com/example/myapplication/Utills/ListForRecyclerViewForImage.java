package com.example.myapplication.Utills;

public class ListForRecyclerViewForImage {
    private String name;
    private int imageID;
    public ListForRecyclerViewForImage(String name, int imageID){
        this.name=name;
        this.imageID=imageID;
    }
    public String getName(){
        return name;
    }
    public int getImageID(){
        return imageID;
    }


}
