package com.pyjtlk.waveloadview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;

import java.lang.ref.SoftReference;

import androidx.annotation.Nullable;

public class WaveLoadingView extends View {
    /**
     * 文本风格
     */
    private static final int IMAGE_TYPE_TEXT = 0;

    /**
     * 圆点风格
     */
    public static final int IMAGE_TYPE_CIRCLE = 1;

    /**
     * 长方形风格
     */
    public static final int IMAGE_TYPE_RECT = 2;

    /**
     * 正方形风格
     */
    public static final int IMAGE_TYPE_SQUARE = 3;

    /**
     * 噪声波风格
     */
    public static final int IMAGE_TYPE_NOISE = 4;

    /**
     * 自定义图像风格
     */
    public static final int IMAGE_TYPE_CUSTOM = 5;

    /**
     * 波形高度：低
     */
    public static final int WAVE_HEIGHT_SLIGHT = 1;

    /**
     * 波形高度：中等
     */
    public static final int WAVE_HEIGHT_NORMAL = 2;

    /**
     * 波形高度：高
     */
    public static final int WAVE_HEIGHT_BIG = 3;

    /**
     * 波形高度：巨大
     */
    public static final int WAVE_HEIGHT_LARGE = 4;

    /**
     * 波移动的默认时间，指每移动一步的时间，单位为毫秒
     */
    public static final int DEFAULT_DURATION_MS = 100;

    /**
     * 默认元素间距，单位为DP
     */
    public static final int DEFAULT_INTERVAL_DP = 5;

    /**
     * 默认元素的尺寸，单位为DP
     * 除了字符风格，元素的宽高是一样的
     */
    public static final int DEFAULT_IMAGE_SIZE_DP = 10;

    /**
     * 默认文本大小，单位为SP
     */
    public static final int DEFAULT_TEXT_SIZE_SP = 10;

    /**
     * 幻影效果透明度最大值
     * alpha范围：0~255
     */
    public static final int GHOST_ALPHA_MAX_DEFAULT = 255;

    /**
     * 幻影效果透明度最小值
     * alpha范围：0~255
     */
    public static final int GHOST_ALPHA_MIN_DEFAULT = 100;

    private static final int ANIM_MESSAGE = 1001;
    private String mText;
    private int mColor;
    private int mType;
    private int mWaveLength;
    private int mLength;
    private float mWaveOffset;
    private int mWaveHeight;
    private Drawable mCustomWaveDrawable;
    private int mDuration;
    private int mInterval;
    private int mImageSize;
    private int mTextWidth;
    private int mTextHeight;
    private int mRectRadius;
    private int mGhostAlphaMax = 255;
    private int mGhostAlphaMin = 100;
    private Paint mPaint;
    private int mWaveStart;
    private int mDisplayStart;
    private int mDisplayEnd;
    private Element mElements[];
    private WaveControler mWaveControler;
    private boolean running;
    private int mWaveAlpha[];
    private boolean ghostEffect;

    private class Element{
        int y;
        int x;
        int alpha = mGhostAlphaMin;
    }

    private static final class AnimHandler extends Handler{
        private SoftReference<WaveLoadingView> mView;

        public AnimHandler(WaveLoadingView view){
            mView = new SoftReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            WaveLoadingView view = mView.get();
            if(view != null){
                view.refresh();
            }
        }

        public void recycle(){
            mView.clear();
        }
    }

    private AnimHandler mAnimHandler = new AnimHandler(this);

    /**
     * 波控制类，可以用于控制波的移动方向和移动大小
     */
    public static interface WaveControler{
        /**
         * 波移动前会调用，可以用于控制波的移动方向和移动大小
         * @param currentPostion 波当前的位置，这个位置是指波最高点的位置
         * @param start 波可移动的左边界
         * @param end 波可移动的右边界
         * @return 波下一个移动位置,可以通过它决定波的移动方向和移动大小
         *         currentPostion大于返回值，波往左移动。currentPostion小于返回值，波往右移动
         */
        int onRefresh(int currentPostion,int start,int end);
    }

    public WaveLoadingView(Context context) {
        super(context);
    }

    public WaveLoadingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context,attrs);
    }

    public WaveLoadingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context,attrs);
    }

    public WaveLoadingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context,attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs){
        TypedArray typedArray = context.obtainStyledAttributes(attrs,R.styleable.WaveLoadingView);

        mText = typedArray.getString(R.styleable.WaveLoadingView_text);
        mColor = typedArray.getColor(R.styleable.WaveLoadingView_color, Color.BLUE);
        mType = typedArray.getInt(R.styleable.WaveLoadingView_imageWaveType,IMAGE_TYPE_CIRCLE);
        mWaveLength = typedArray.getInt(R.styleable.WaveLoadingView_waveLength,1);
        mWaveHeight = typedArray.getInt(R.styleable.WaveLoadingView_waveHeight,WAVE_HEIGHT_NORMAL);
        mCustomWaveDrawable = typedArray.getDrawable(R.styleable.WaveLoadingView_customImage);
        mDuration = typedArray.getInt(R.styleable.WaveLoadingView_duration,DEFAULT_DURATION_MS);
        mInterval = typedArray.getDimensionPixelSize(R.styleable.WaveLoadingView_interval,dp2px(DEFAULT_INTERVAL_DP));
        mImageSize = typedArray.getDimensionPixelSize(R.styleable.WaveLoadingView_imageSize,dp2px(DEFAULT_IMAGE_SIZE_DP));
        mTextWidth = typedArray.getDimensionPixelSize(R.styleable.WaveLoadingView_textSize,sp2px(DEFAULT_TEXT_SIZE_SP));
        mLength = typedArray.getInt(R.styleable.WaveLoadingView_length,mWaveLength + 2);
        mRectRadius = typedArray.getDimensionPixelSize(R.styleable.WaveLoadingView_rectRadius,0);
        ghostEffect = typedArray.getBoolean(R.styleable.WaveLoadingView_ghostEffect,false);
        mGhostAlphaMax = typedArray.getInt(R.styleable.WaveLoadingView_ghostAlphaMax,GHOST_ALPHA_MAX_DEFAULT);
        mGhostAlphaMin = typedArray.getInt(R.styleable.WaveLoadingView_ghostAlphaMin,GHOST_ALPHA_MIN_DEFAULT);

        typedArray.recycle();

        switch(mWaveHeight){
            case WAVE_HEIGHT_SLIGHT:
                mWaveOffset = 0.25f;
                break;

            case WAVE_HEIGHT_NORMAL:
                mWaveOffset = 0.5f;
                break;

            case WAVE_HEIGHT_BIG:
                mWaveOffset = 0.75f;
                break;

            case WAVE_HEIGHT_LARGE:
                mWaveOffset = 1f;
                break;
        }

        int type = mType;

        if(mText != null && (mText.length() - mWaveLength) >= 2){
            mLength = mText.length();
            mTextHeight = textWidthToHtight(mTextWidth);
            type = IMAGE_TYPE_TEXT;
        }

        if(mCustomWaveDrawable != null){
            type = IMAGE_TYPE_CUSTOM;
        }

        if(mRectRadius * 2 > mImageSize){
            mRectRadius = mImageSize / 2 - 1;
        }

        int elementsTotal = mLength + (mWaveLength - 1) * 2;
        mElements = new Element[elementsTotal];
        for(int i = 0;i < elementsTotal;i++){
            mElements[i] = new Element();
        }
        mDisplayStart = mWaveLength - 1;
        mDisplayEnd = mDisplayStart + mLength;
        mWaveStart = 0;

        mWaveAlpha= new int[mWaveLength];

        if(mGhostAlphaMax > GHOST_ALPHA_MAX_DEFAULT){
            mGhostAlphaMax = GHOST_ALPHA_MAX_DEFAULT;
        }

        if(mGhostAlphaMin < 10){
            mGhostAlphaMin = 10;
        }

        if(mGhostAlphaMax < mGhostAlphaMin){
            mGhostAlphaMax = GHOST_ALPHA_MAX_DEFAULT;
            mGhostAlphaMin = GHOST_ALPHA_MIN_DEFAULT;
        }

        int halfLen = mWaveLength % 2 == 0 ? mWaveLength / 2 + 1 : mWaveLength / 2;
        int alphaInterval = (mGhostAlphaMax - mGhostAlphaMin) / (halfLen + 1);

        for(int i = 0;i <= halfLen;i++) {
            mWaveAlpha[i] = mWaveAlpha[mWaveLength - i - 1] = mGhostAlphaMin + (i + 1) * alphaInterval;
        }

        mType = type;

        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        ViewTreeObserver observer = getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                start();
            }
        });
    }

    private int textWidthToHtight(int width){
        return (int) ((width +  0.00000007) / 0.7535);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //super.onMeasure(widthMeasureSpec,heightMeasureSpec);
        int elementWidth = 0;
        int elementHeight= 0;

        switch(mType){
            case IMAGE_TYPE_TEXT:
                elementWidth = mTextWidth;
                elementHeight = mTextHeight;
                break;

            case IMAGE_TYPE_CIRCLE:
            case IMAGE_TYPE_SQUARE:
            case IMAGE_TYPE_RECT:
            case IMAGE_TYPE_NOISE:
                elementWidth = elementHeight = mImageSize;
                break;

            case IMAGE_TYPE_CUSTOM:
                elementWidth = elementHeight = mImageSize;
                break;
        }

        onMeasureSelf(widthMeasureSpec,heightMeasureSpec,elementWidth,elementHeight);
    }

    protected void onMeasureSelf(int widthMeasureSpec, int heightMeasureSpec,int elementWidth,int elemwntHeight){
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        int wrapWidth = mLength * elementWidth +
                (mLength - 1) * mInterval +
                paddingLeft + paddingRight;

        int waveHeight = (mWaveLength % 2) != 0 ?
                (int) (elemwntHeight + (mWaveLength / 2 + 1) * elemwntHeight * mWaveOffset) :
                (int) (elemwntHeight + mWaveLength / 2 * elemwntHeight * mWaveOffset);

        int wrapHeight = waveHeight + paddingTop + paddingBottom;

        if(widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.AT_MOST){
            width = wrapWidth;
            height = wrapHeight;
        }else if(widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.AT_MOST){
            height = wrapHeight;
        }else if(widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.EXACTLY){
            width = wrapWidth;
        }

        setMeasuredDimension(width,height);
    }

    private int dp2px(int dp){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX,dp,getResources().getDisplayMetrics());
    }

    private int sp2px(int sp){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX,sp,getResources().getDisplayMetrics());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        prepareElementsX();
        prepareElementsY();

        switch(mType){
            case IMAGE_TYPE_TEXT:
                onDrawText(canvas);
                break;

            case IMAGE_TYPE_CIRCLE:
                onDrawCirclesWave(canvas);
                break;

            case IMAGE_TYPE_SQUARE:
                onDrawSquareWave(canvas);
                break;

            case IMAGE_TYPE_RECT:
                onDrawRectWave(canvas);
                break;

            case IMAGE_TYPE_NOISE:
                onDrawNoiseWave(canvas);
                break;

            case IMAGE_TYPE_CUSTOM:
                onDrawDrawable(canvas);
                break;
        }
    }

    protected void prepareElementsY(){
        int waveEleCount = 1;
        int waveHighestPos;
        int waveEndPos = (mWaveStart + mWaveLength - 1) % mElements.length;

        boolean isOdd = mWaveLength % 2 != 0;

        int elementHeight = 0;

        float elementHeightOffset;

        switch(mType){
            case IMAGE_TYPE_TEXT:
                elementHeight = mTextHeight;
                break;

            case IMAGE_TYPE_CIRCLE:
            case IMAGE_TYPE_SQUARE:
            case IMAGE_TYPE_RECT:
            case IMAGE_TYPE_NOISE:
                elementHeight = mImageSize;
                break;

            case IMAGE_TYPE_CUSTOM:
                elementHeight = mImageSize;
                break;
        }

        elementHeightOffset = elementHeight * mWaveOffset;

        int waveBottom = getHeight() - getPaddingBottom();

        waveHighestPos = (mWaveStart + mWaveLength / 2) % mElements.length;

        if(isOdd){
            //case1
            if(waveHighestPos >= mDisplayStart && waveHighestPos <= mDisplayEnd){
                //set wave
                for(int i = mWaveStart;i <= waveEndPos;i++){
                    //wave up
                    if(i < waveHighestPos){
                        mElements[i].y = (int) (waveBottom - elementHeight - waveEleCount * elementHeightOffset);
                        mElements[i].alpha = mWaveAlpha[waveEleCount - 1];
                        waveEleCount++;
                    }else if(i > waveHighestPos){ //wave down
                        waveEleCount--;
                        mElements[i].y = (int) (waveBottom - elementHeight - waveEleCount * elementHeightOffset);
                        mElements[i].alpha =  mWaveAlpha[waveEleCount - 1];
                    }else{
                        mElements[i].y = (int) (waveBottom - elementHeight - waveEleCount * elementHeightOffset);
                        mElements[i].alpha = mGhostAlphaMax;
                    }
                }

                //set even
                for(int i = mDisplayStart;i < mWaveStart;i++){
                    mElements[i].y = waveBottom - elementHeight;
                    mElements[i].alpha = mGhostAlphaMin;
                }

                for(int i = waveEndPos + 1;i <= mDisplayEnd;i++){
                    mElements[i].y = waveBottom - elementHeight;
                    mElements[i].alpha = mGhostAlphaMin;
                }
                return;
            }

            //case2
            if(waveHighestPos < mDisplayStart){
                //set wave
                for(int i = waveEndPos;i >= mDisplayStart;i--){
                    mElements[i].y = (int) (waveBottom - elementHeight - waveEleCount * elementHeightOffset);
                    mElements[i].alpha =  mWaveAlpha[waveEleCount - 1];
                    waveEleCount++;
                }

                //set even
                for(int i = waveEndPos + 1;i <= mDisplayEnd;i++){
                    mElements[i].y = waveBottom - elementHeight;
                    mElements[i].alpha = mGhostAlphaMin;
                }
                return;
            }

            //case3
            if(waveHighestPos > mDisplayEnd){
                //set even
                for(int i = mDisplayStart;i < mWaveStart;i++){
                    mElements[i].y = waveBottom - elementHeight;
                    mElements[i].alpha = mGhostAlphaMin;
                }

                //set wave
                for(int i = mWaveStart;i < mDisplayEnd;i++){
                    mElements[i].y = (int) (waveBottom - elementHeight - waveEleCount * elementHeightOffset);
                    mElements[i].alpha =  mWaveAlpha[waveEleCount - 1];
                    waveEleCount++;
                }
                return;
            }
        }

        waveHighestPos--;
        //is not odd
        //case1
        if(waveHighestPos > mDisplayStart && waveHighestPos <= mDisplayEnd){
            //set wave
            for(int i = mWaveStart;i <= waveEndPos;i++){
                //wave up
                if(i < waveHighestPos){
                    mElements[i].y = (int) (waveBottom - elementHeight - waveEleCount * elementHeightOffset);
                    mElements[i].alpha =  mWaveAlpha[waveEleCount - 1];
                    waveEleCount++;
                }else if(i > waveHighestPos){ //wave down
                    mElements[i].y = (int) (waveBottom - elementHeight - waveEleCount * elementHeightOffset);
                    mElements[i].alpha =  mWaveAlpha[waveEleCount - 1];
                    waveEleCount--;
                }else{
                    mElements[i].y = (int) (waveBottom - elementHeight - waveEleCount * elementHeightOffset);
                    mElements[i].alpha = mGhostAlphaMax;
                }
            }

            //set even
            for(int i = mDisplayStart;i < mWaveStart;i++){
                mElements[i].y = waveBottom - elementHeight;
                mElements[i].alpha = mGhostAlphaMin;
            }

            for(int i = waveEndPos + 1;i <= mDisplayEnd;i++){
                mElements[i].y = waveBottom - elementHeight;
                mElements[i].alpha = mGhostAlphaMin;
            }
            return;
        }

        //case2
        if(waveHighestPos <= mDisplayStart){
            //set wave
            for(int i = waveEndPos;i > waveHighestPos;i--){
                mElements[i].y = (int) (waveBottom - elementHeight - waveEleCount * elementHeightOffset);
                mElements[i].alpha =  mWaveAlpha[waveEleCount - 1];
                waveEleCount++;
            }

            //set even
            for(int i = waveEndPos + 1;i <= mDisplayEnd;i++){
                mElements[i].y = waveBottom - elementHeight;
                mElements[i].alpha = mGhostAlphaMin;
            }
            return;
        }

        //case3
        if(waveHighestPos > mDisplayEnd){
            //set even
            for(int i = mDisplayStart;i < mWaveStart;i++){
                mElements[i].y = waveBottom - elementHeight;
                mElements[i].alpha = mGhostAlphaMin;
            }

            //set wave
            for(int i = mWaveStart;i <= mDisplayEnd;i++){
                mElements[i].y = (int) (waveBottom - elementHeight - waveEleCount * elementHeightOffset);
                mElements[i].alpha =  mWaveAlpha[waveEleCount - 1];
                waveEleCount++;
            }
            return;
        }
    }

    protected void prepareElementsX(){
        int drawLeftStart = getPaddingLeft();

        int elementWidth = 0;

        switch(mType){
            case IMAGE_TYPE_TEXT:
                elementWidth = mTextWidth;
                break;

            case IMAGE_TYPE_CIRCLE:
            case IMAGE_TYPE_SQUARE:
            case IMAGE_TYPE_RECT:
            case IMAGE_TYPE_NOISE:
                elementWidth = mImageSize;
                break;

            case IMAGE_TYPE_CUSTOM:
                elementWidth = mImageSize;
                break;
        }

        for(int i = 0;i < mLength;i++){
            int x = drawLeftStart + (elementWidth + mInterval) * i;
            mElements[mDisplayStart + i].x = x;
        }
    }

    protected void onDrawText(Canvas canvas){
        mPaint.setColor(mColor);
        mPaint.setTextSize(mTextWidth);
        Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();

        for(int i = 0;i < mLength;i++){
            if(ghostEffect){
                mPaint.setAlpha(mElements[mDisplayStart + i].alpha);
            }

            canvas.drawText(mText.substring(i,i+1),
                    mElements[mDisplayStart + i].x,
                    mElements[mDisplayStart + i].y + mTextHeight - fontMetrics.bottom,
                    mPaint);
        }
    }

    protected void onDrawCirclesWave(Canvas canvas){
        mPaint.setColor(mColor);
        for(int i = 0;i < mLength;i++){
            if(ghostEffect){
                mPaint.setAlpha(mElements[mDisplayStart + i].alpha);
            }

            canvas.drawCircle(mElements[mDisplayStart + i].x + mImageSize / 2,
                    mElements[mDisplayStart + i].y + mImageSize / 2,
                    mImageSize / 2,
                    mPaint);
        }
    }

    protected void onDrawSquareWave(Canvas canvas){
        mPaint.setColor(mColor);
        for(int i = 0;i < mLength;i++){
            if(ghostEffect){
                mPaint.setAlpha(mElements[mDisplayStart + i].alpha);
            }

            if(mRectRadius > 0){
                canvas.drawRoundRect(mElements[mDisplayStart + i].x,
                        mElements[mDisplayStart + i].y,
                        mElements[mDisplayStart + i].x + mImageSize,
                        mElements[mDisplayStart + i].y + mImageSize,
                        10,10,
                        mPaint);
            }else{
                canvas.drawRect(mElements[mDisplayStart + i].x,
                        mElements[mDisplayStart + i].y,
                        mElements[mDisplayStart + i].x + mImageSize,
                        mElements[mDisplayStart + i].y + mImageSize,
                        mPaint);
            }
        }
    }

    protected void onDrawRectWave(Canvas canvas){
        mPaint.setColor(mColor);
        for(int i = 0;i < mLength;i++){
            if(ghostEffect){
                mPaint.setAlpha(mElements[mDisplayStart + i].alpha);
            }

            if(mRectRadius > 0){
                canvas.drawRoundRect(mElements[mDisplayStart + i].x,
                        mElements[mDisplayStart + i].y,
                        mElements[mDisplayStart + i].x + mImageSize,
                        getHeight() - getPaddingBottom(),
                        mRectRadius,mRectRadius,
                        mPaint);
            }else{
                canvas.drawRect(mElements[mDisplayStart + i].x,
                        mElements[mDisplayStart + i].y,
                        mElements[mDisplayStart + i].x + mImageSize,
                        getHeight() - getPaddingBottom(),
                        mPaint);
            }
        }
    }

    protected void onDrawNoiseWave(Canvas canvas){
        int nosieWidth = mImageSize / 8;
        int left;
        int bottom = getHeight() - getPaddingBottom();
        int height;
        mPaint.setColor(mColor);
        for(int i = 0;i < mLength;i++) {
            if(ghostEffect){
                mPaint.setAlpha(mElements[mDisplayStart + i].alpha);
            }

            for(int j = 0;j < 4;j++){
                height = (int) (mElements[mDisplayStart + i].y * 0.25
                    + mElements[mDisplayStart + i].y * 0.75 * Math.random());
                left = mElements[mDisplayStart + i].x + nosieWidth * 2 * j;
                canvas.drawRect(left,
                        height,
                        left + nosieWidth,
                        bottom,
                        mPaint);
            }
        }
    }

    protected void onDrawDrawable(Canvas canvas){
        for(int i = 0;i < mLength;i++){
            if(ghostEffect){
                mCustomWaveDrawable.setAlpha(mElements[mDisplayStart + i].alpha);
            }

            mCustomWaveDrawable.setBounds(mElements[mDisplayStart + i].x,
                    mElements[mDisplayStart + i].y,
                    mElements[mDisplayStart + i].x + mImageSize,
                    mElements[mDisplayStart + i].y + mImageSize);

            mCustomWaveDrawable.draw(canvas);
        }
    }

    protected void refresh(){
        move();
        if(running){
            mAnimHandler.sendEmptyMessageDelayed(ANIM_MESSAGE,mDuration);
        }
    }

    protected void move(){
        if(mWaveControler != null){
            mWaveStart = mWaveControler.onRefresh(mWaveStart,0,mElements.length - 1);
        }else{
            mWaveStart++;
        }

        if(mWaveStart >= mElements.length || mWaveStart < 0){
            mWaveStart = 0;
        }
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        recycle();
    }

    /**
     * 启动控件动画
     */
    public void start(){
        if(!running){
            running = true;
            mAnimHandler.sendEmptyMessageDelayed(0,mDuration);
        }
    }

    /**
     * 暂停控件动画
     */
    public void pause(){
        if(running){
            running = false;
            mAnimHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * 设置动画间隔时间，这个时间是指波每移动一步的时间，单位为毫秒
     * @param duration
     */
    public void setDuration(int duration){
        if(duration < 0){
            return;
        }

        mDuration = duration;
    }

    /**
     * 设置元素的颜色，风格为如下几种才会生效
     * {@link WaveLoadingView#IMAGE_TYPE_CIRCLE}
     * {@link WaveLoadingView#IMAGE_TYPE_RECT}
     * {@link WaveLoadingView#IMAGE_TYPE_SQUARE}
     * {@link WaveLoadingView#IMAGE_TYPE_NOISE}
     * @param color
     */
    public void setColor(int color){
        mColor = color;
        invalidate();
    }

    /**
     * 获取波的宽度
     * @return
     */
    public int getWaveLength(){
        return mWaveLength;
    }

    /**
     * 设置波的宽度，风格为如下几种才会生效
     * {@link WaveLoadingView#IMAGE_TYPE_CIRCLE}
     * {@link WaveLoadingView#IMAGE_TYPE_RECT}
     * {@link WaveLoadingView#IMAGE_TYPE_SQUARE}
     * {@link WaveLoadingView#IMAGE_TYPE_NOISE}
     * {@link WaveLoadingView#IMAGE_TYPE_CUSTOM}
     * @param waveLength 波的宽度
     */
    public void setWaveLength(int waveLength){
        boolean isRunning = running;
        pause();

        try{
            if(waveLength + 2 > mLength){
                return;
            }
            mWaveLength = waveLength;

            int elementsTotal = mLength + (mWaveLength - 1) * 2;
            mElements = new Element[elementsTotal];
            for(int i = 0;i < elementsTotal;i++){
                mElements[i] = new Element();
            }

            requestLayout();
        }finally {
            if(isRunning){
                start();
            }
        }
    }

    /**
     * 设置波控制器，默认下波是从左往右移动的，如果设置成功，则原来的波移动方式会被替代
     * @param waveControler
     */
    public void setWaveControler(WaveControler waveControler){
        mWaveControler = waveControler;
    }

    /**
     * 设置元素间的间隔
     * @param interval
     */
    public void setInterval(int interval){
        boolean isRunning = running;
        pause();

        try{
            if(interval < 0){
                return;
            }

            mInterval = interval;
            requestLayout();
        }finally {
            if(isRunning){
                start();
            }
        }
    }

    /**
     * 获取元素间隔
     * @return
     */
    public int getInterval(){
        return mInterval;
    }

    /**
     * 设置波的风格，只能设置如下几种风格，且需要提前设置好波长度{@link WaveLoadingView#setWaveLength}
     * {@link WaveLoadingView#IMAGE_TYPE_CIRCLE}
     * {@link WaveLoadingView#IMAGE_TYPE_RECT}
     * {@link WaveLoadingView#IMAGE_TYPE_SQUARE}
     * {@link WaveLoadingView#IMAGE_TYPE_NOISE}
     * {@link WaveLoadingView#IMAGE_TYPE_CUSTOM}
     * @param type
     */
    public void setType(int type){
        if(mType == IMAGE_TYPE_TEXT){
            return;
        }

        if(mType == IMAGE_TYPE_CUSTOM && mCustomWaveDrawable == null){
            return;
        }

        mType = type;
        invalidate();
    }

    /**
     * 获取当前的风格
     * {@link WaveLoadingView#IMAGE_TYPE_TEXT}
     * {@link WaveLoadingView#IMAGE_TYPE_CIRCLE}
     * {@link WaveLoadingView#IMAGE_TYPE_RECT}
     * {@link WaveLoadingView#IMAGE_TYPE_SQUARE}
     * {@link WaveLoadingView#IMAGE_TYPE_NOISE}
     * {@link WaveLoadingView#IMAGE_TYPE_CUSTOM}
     * @return
     */
    public int getType(){
        return mType;
    }

    /**
     * 是否启用幻影效果
     * @return
     */
    public boolean isGhostEffect(){
        return ghostEffect;
    }

    /**
     * 启用幻影效果
     * @param allowGhostEffect
     */
    public void setGhostEffect(boolean allowGhostEffect){
        ghostEffect = allowGhostEffect;
        invalidate();
    }

    /**
     * 设置幻影效果透明度，透明度范围：0~255,值越低越透明
     * 波峰透明度最高，离波峰越远透明度越低
     * @param minAlpha 透明度下限
     * @param maxAlpha 透明度上限
     */
    public void setGhostAlpha(int minAlpha,int maxAlpha){
        if(minAlpha > maxAlpha || minAlpha < 0 || maxAlpha > GHOST_ALPHA_MAX_DEFAULT || minAlpha > GHOST_ALPHA_MAX_DEFAULT){
            return;
        }

        mGhostAlphaMin = minAlpha;
        mGhostAlphaMax = maxAlpha;
        invalidate();
    }

    /**
     * 设置元素自定义图片,此方法会复制传入的Drawable，所以此Drawable的原始尺寸不要过大，否则会很消耗内存
     * @param drawable
     */
    public void setWaveDrawable(Drawable drawable){
        if(drawable == null){
            return;
        }
        mCustomWaveDrawable = drawable.mutate();
        mType = IMAGE_TYPE_CUSTOM;
        invalidate();
    }

    protected void recycle(){
        mAnimHandler.removeCallbacksAndMessages(null);
        mAnimHandler.recycle();
        mAnimHandler = null;
    }
}
