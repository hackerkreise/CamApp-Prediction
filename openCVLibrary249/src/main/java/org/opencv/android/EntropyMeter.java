package org.opencv.android;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import org.opencv.core.Core;

import java.text.DecimalFormat;

/**
 * Created by Adam on 01.06.2016. Noch nicht fertig!!
 */
public class EntropyMeter {
    private static final String TAG               = "EntropyMeter";
    private static final int    STEP              = 20;
    private static final DecimalFormat ENTROPY_FORMAT = new DecimalFormat("0.00");

    private int                 mFramesCouner;
    private double              mFrequency;
    private long                mprevFrameTime;
    private String              mStrEntropy;
    Paint mPaint;
    boolean                     mIsInitialized = false;
    int                         mWidth = 0;
    int                         mHeight = 0;

    public void init() {
        mFramesCouner = 0;
        mFrequency = Core.getTickFrequency();
        mprevFrameTime = Core.getTickCount();
        mStrEntropy = "";

        mPaint = new Paint();
        mPaint.setColor(Color.RED);
        mPaint.setTextSize(20);
    }

    public void measure() {




        //mStrEntropy = entropy;

        /* if (!mIsInitialized) {
            init();
            mIsInitialized = true;
        } else {
            mFramesCouner++;
            if (mFramesCouner % STEP == 0) {
                long time = Core.getTickCount();
                double fps = STEP * mFrequency / (time - mprevFrameTime);
                mprevFrameTime = time;
                if (mWidth != 0 && mHeight != 0)
                    mStrEntropy = FPS_FORMAT.format(fps) + " Entropy@" + Integer.valueOf(mWidth) + "x" + Integer.valueOf(mHeight);
                else
                    mStrEntropy = FPS_FORMAT.format(fps) + " Entropy";
                Log.i(TAG, mStrEntropy);
            }
        }
        */



    }

    public void setResolution(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public void draw(Canvas canvas, float offsetx, float offsety) {
        Log.d(TAG, mStrEntropy);
        canvas.drawText(mStrEntropy, offsetx, offsety, mPaint);
    }
}
