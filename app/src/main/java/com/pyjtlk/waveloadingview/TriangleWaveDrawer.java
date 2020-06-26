package com.pyjtlk.waveloadingview;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;

import com.pyjtlk.waveloadview.WaveLoadingView;

public class TriangleWaveDrawer extends WaveLoadingView.AbsWaveDrawer {
    private Path mPath = new Path();

    @Override
    protected void onDrawWave(Canvas canvas, Paint paint, Rect elementRect, int imageSize) {
        mPath.reset();
        mPath.moveTo(elementRect.left,elementRect.bottom);
        mPath.lineTo(elementRect.right,elementRect.bottom);
        mPath.lineTo(elementRect.left + (elementRect.right - elementRect.left) / 2,elementRect.top);
        mPath.close();
        canvas.drawPath(mPath,paint);
    }
}
