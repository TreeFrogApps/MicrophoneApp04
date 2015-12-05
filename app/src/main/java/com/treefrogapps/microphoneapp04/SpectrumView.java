package com.treefrogapps.microphoneapp04;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;

public class SpectrumView extends View {

    float mWidth;
    float mHeight;

    static Paint mPaint = new Paint();

    // float array for graph layout
    float[] mGraphHorizontal;
    float[] mGraphVertical;

    // float array for spectral line layout
    float[] mDrawLines;


    // double array for frequency bins (512)
    double[] mFrequencyBins = new double[SoundRecorderFragment.FFTBuffer / 2];


    public SpectrumView(Context context) {
        super(context);


    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        mWidth = w;
        mHeight = h;

        mDrawLines = new float[mFrequencyBins.length * 4];

        mGraphHorizontal = new float[(int) (mHeight / 15) * 4]; // * 4 as each line has 4 co-ords
        mGraphVertical = new float[(int) (mWidth / 15) * 4]; // * 4 as each line has 4 co-ords

        Log.e("SCREEN DIMENS", "Width = " + String.valueOf(mWidth) + " Height " + String.valueOf(mHeight));

        super.onSizeChanged(w, h, oldw, oldh);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();


        // -- DRAW GRAPH ----------------------------------------------------------------

        int yHor = 0;
        float yPlus = 0;

        for (int i = 0; i < (int) mHeight / 15; i++) {


            mGraphHorizontal[yHor] = 0;
            mGraphHorizontal[yHor + 1] = mHeight - yPlus;
            mGraphHorizontal[yHor + 2] = mWidth;
            mGraphHorizontal[yHor + 3] = mHeight - yPlus;

            yPlus += (int) mHeight / 15;
            yHor += 4;
        }

        mPaint.setColor(Color.argb(90, 220, 220, 220));
        mPaint.setStrokeWidth(0);
        canvas.drawLines(mGraphHorizontal, mPaint);


        int xVer = 0;
        float xPlus = 0;

        for (int i = 0; i < (int) mWidth / 15; i++) {

            mGraphVertical[xVer] = xPlus;
            mGraphVertical[xVer + 1] = mHeight;
            mGraphVertical[xVer + 2] = xPlus;
            mGraphVertical[xVer + 3] = 0;

            xPlus += (int) mWidth / 15;
            xVer += 4;
        }

        mPaint.setColor(Color.argb(90, 220, 220, 220));
        mPaint.setStrokeWidth(0);
        canvas.drawLines(mGraphVertical, mPaint);

        // ------------------------------------------------------------------------------

        // -- DRAW SPECTRUM -------------------------------------------------------------

        float x = 0;

        int x1 = 0;
        int y1 = (int) mHeight;

        for (int i = 0; i < SoundRecorderFragment.FFTBuffer / 2; i++) {

            mDrawLines[x1] = x;
            mDrawLines[x1 + 1] = y1;

            mDrawLines[x1 + 2] = x;
            // variable that changes height
            mDrawLines[x1 + 3] = y1 - (int) (mFrequencyBins[i] * 50); // * 50 = gain level

            // changes distance between lines
            x += mWidth / mFrequencyBins.length;

            x1 += 4;
        }

        mPaint.setColor(Color.argb(255, 0, 255, 0));
        mPaint.setStrokeWidth(mWidth / mFrequencyBins.length);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        canvas.drawLines(mDrawLines, mPaint);

        // ---------------------------------------------------------------------------------

        canvas.restore();
    }


    public void redraw(double[] frequencyBins) {

        this.mFrequencyBins = frequencyBins; // 512 bins

        invalidate();
    }


}
