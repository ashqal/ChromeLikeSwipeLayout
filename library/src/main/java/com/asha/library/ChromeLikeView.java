package com.asha.library;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.BounceInterpolator;

/**
 * Created by hzqiujiadi on 15/11/18.
 * hzqiujiadi ashqalcn@gmail.com
 */
public class ChromeLikeView extends View implements ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
    private static final String TAG = "ChromeLikeView";
    private static final int radius = 80;
    private static final float sMagicNumber = 0.55228475f;
    private Paint mPaint;
    private Paint mCirclePaint;
    private Paint mDebugPaint;
    private Path mPath;
    private int mSize;
    private float mPrevX;
    private float mPrevY;
    private float mDegrees;
    private boolean mIsDown;
    private float mTranslate;
    private int mCurrentFlag = 1;
    private float mAnimToX;
    private float mAnimToY;
    private float mAnimFromX;
    private float mAnimFromY;

    public ChromeLikeView(Context context) {
        super(context);
        init();
    }

    public ChromeLikeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ChromeLikeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ChromeLikeView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setColor(0xFFFFCC11);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeWidth(5);

        mCirclePaint = new Paint();
        mCirclePaint.setColor(0xFFCC11FF);
        mCirclePaint.setStyle(Paint.Style.FILL);
        mCirclePaint.setAntiAlias(true);

        mDebugPaint = new Paint();
        mDebugPaint.setColor(0xFF00FF00);
        mDebugPaint.setStyle(Paint.Style.FILL);
        mDebugPaint.setAntiAlias(true);

        mSize = 3;

        mPath = new Path();
        //PathMeasure measure = new PathMeasure();
        //measure.setPath(mPath,false);
        mCurrentFlag = 1;
        mTranslate = flag2TargetTranslate(mCurrentFlag);
        update(0, 0, 0, 0, false);

    }


    public void onActionDown(MotionEvent event){
        mIsDown = true;
        float currentX = event.getX();
        float currentY = event.getY();
        mPrevX = currentX;
        mPrevY = currentY;
    }

    public void onActionMove(MotionEvent event){
        if ( !mIsDown ) return;
        float currentX = event.getX();
        float currentY = event.getY();
        if ( mAnimationStarted ){
            mAnimFromX = currentX;
            return;
        }
        update( currentX, 0, mPrevX, 0, false );
        if ( Math.abs( currentX - mPrevX ) > radius*2 ){
            if ( currentX > mPrevX ){
                mCurrentFlag++;
                mCurrentFlag %= mSize;
            } else {
                mCurrentFlag--;
                mCurrentFlag += mSize;
                mCurrentFlag %= mSize;
            }
            launchAnim(currentX, currentY, flag2TargetTranslate(mCurrentFlag) );
        }
    }

    public void onActionUpOrCancel(MotionEvent event){
        if ( !mIsDown ) return;
        float currentX = event.getX();
        float currentY = event.getY();
        mIsDown = false;
        if ( mAnimationStarted ) return;
        launchAnim( currentX, currentY, flag2TargetTranslate(mCurrentFlag) );
    }

    private void update(float currentX, float currentY, float prevX, float prevY, boolean animate ){
        float distance = distance(prevX, prevY, currentX, currentY);
        float tempDegree = points2degress(prevX, prevY, currentX, currentY);
        if ( animate ){
            if ( Math.abs( mDegrees - tempDegree ) > 5 ) distance = -distance;
        } else {
            //if ( distance < mTouchSlop ) distance = 0;
            mDegrees = tempDegree;
        }
        float realLong = radius  + distance;
        float realShort = radius - distance * 0.1f;

        mPath.reset();

        mPath.lineTo(0, -radius);
        mPath.cubicTo(radius * sMagicNumber, -radius
                , realLong, -radius * sMagicNumber
                , realLong, 0);
        mPath.lineTo(0, 0);

        mPath.lineTo(0, radius);
        mPath.cubicTo(radius * sMagicNumber, radius
                , realLong, radius * sMagicNumber
                , realLong, 0);
        mPath.lineTo(0, 0);

        mPath.lineTo(0, -radius);
        mPath.cubicTo(-radius * sMagicNumber, -radius
                , -realShort, -radius * sMagicNumber
                , -realShort, 0);
        mPath.lineTo(0, 0);

        mPath.lineTo(0, radius);
        mPath.cubicTo(-radius * sMagicNumber, radius
                , -realShort, radius * sMagicNumber
                , -realShort, 0);
        mPath.lineTo(0, 0);

        invalidate();
    }

    ValueAnimator valueAnimator;
    private void launchAnim(float fromX, float fromY, float toTranslate) {
        if ( valueAnimator == null ){
            valueAnimator = new ValueAnimator();
            valueAnimator.setDuration(200);
            valueAnimator.addUpdateListener(this);
            valueAnimator.setInterpolator(new BounceInterpolator());
            valueAnimator.addListener(this);
        }
        mAnimFromX = fromX;
        mAnimFromY = fromY;
        mAnimToX = mPrevX;
        mAnimToY = mPrevY;
        valueAnimator.setValues(PropertyValuesHolder.ofFloat("x", fromX, mPrevX)
                , PropertyValuesHolder.ofFloat("y", fromY, mPrevY)
                , PropertyValuesHolder.ofFloat("translate", mTranslate , toTranslate)
        );
        valueAnimator.cancel();
        valueAnimator.start();
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        Float currentX = (Float) animation.getAnimatedValue("x");
        Float currentY = (Float) animation.getAnimatedValue("y");
        mTranslate = (Float) animation.getAnimatedValue("translate");
        update(currentX, 0, mAnimToX, 0, true);
    }


    public static float distance(float x1,float y1, float x2, float y2){
        return (float) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int centerX = getMeasuredWidth() >> 1;
        int centerY = getMeasuredHeight() >> 1;


        canvas.drawColor(0xFFDDDDDD);
        canvas.save();
        canvas.translate(centerX + mTranslate, centerY);
        canvas.rotate(mDegrees);
        canvas.drawPath(mPath, mPaint);
        canvas.restore();

        canvas.drawCircle(centerX, centerY, radius / 4, mCirclePaint);
        canvas.drawCircle(centerX + radius*3,centerY,radius/4,mCirclePaint);
        canvas.drawCircle(centerX - radius*3,centerY,radius/4,mCirclePaint);

        canvas.drawCircle(mPrevX,mPrevY,5,mDebugPaint);

        //canvas.drawArc(mRectF, 90, 180, true, mPaint);
    }

    private int flag2TargetTranslate( int flag ){
        return flag * radius*3 - radius*3;
    }

    private static float points2degress(float x1, float y1, float x2, float y2){
        double angrad = Math.atan2(y2-y1,x2-x1);
        return (float) Math.toDegrees(angrad);
    }

    private boolean mAnimationStarted;
    @Override
    public void onAnimationStart(Animator animation) {
        mAnimationStarted = true;
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        mAnimationStarted = false;
        mPrevX = mAnimFromX;
    }

    @Override
    public void onAnimationCancel(Animator animation) {
        mAnimationStarted = false;
    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }
}
