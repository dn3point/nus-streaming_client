package com.cs5248.two.streamingclient;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

public class CameraView extends TextureView {
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;

    public CameraView(Context context) {
        super(context);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Negative value is invalid");
        }
        mRatioHeight = height;
        mRatioWidth = width;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (mRatioWidth == 0 || mRatioHeight == 0) {
            setMeasuredDimension(width, height);
        } else if (width < height * mRatioWidth / mRatioHeight) {
            setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
        } else {
            setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
        }
    }
}
