package com.example.myapplication.Model;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.LinkedList;

/**
 * Created by chenkaijian on 17-10-19.
 */

public class StickerViewLayout extends FrameLayout {

    private final LinkedList<StickerView> mStickerViewList = new LinkedList<>();

    public StickerViewLayout(@NonNull Context context) {
        super(context);
    }

    public StickerViewLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void addView(final View child) {
        super.addView(child);
        if (child instanceof StickerView) {
            mStickerViewList.add((StickerView) child);
            ((StickerView) child).setOnSelectedListener(new StickerView.OnSelectedListener() {
                @Override
                public void onSelected() {
                    mStickerViewList.remove(child);
                    mStickerViewList.add((StickerView) child);
                }
            });

            ((StickerView) child).setOnRemovedListener(new StickerView.OnRemovedListener() {
                @Override
                public void onRemoved() {
                    mStickerViewList.remove(child);
                }
            });
        }
    }

    public LinkedList getStickerViewList() {
        return mStickerViewList;
    }
}
