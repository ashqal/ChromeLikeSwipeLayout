package com.asha.library;

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
public class ChromeLikeView extends View implements ValueAnimator.AnimatorUpdateListener {
    private static final String TAG = "ChromeLikeView";
    private Paint mPaint;
    private Paint mCirclePaint;
    private Path mPath;

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

        mPath = new Path();

        //PathMeasure measure = new PathMeasure();
        //measure.setPath(mPath,false);
        mCurrentFlag = 1;
        mTranslate = flag2TargetTranslate(mCurrentFlag);
        update(0, 0, false);

    }
    int radius = 80;


    private static final float sMagicNumber = 0.55228475f;
    private void update(float x, float y, boolean animate ){
        float distance = distance(mPrevX,mPrevY,x,y);
        float tempDegree = points2degress(mPrevX,mPrevY,x,y);
        if ( animate ){
            if ( Math.abs( mDegrees - tempDegree ) > 5 ) distance = -distance;
        } else {
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

    private float mPrevX;
    private float mPrevY;
    private float mDegrees;
    private boolean mIsDown;
    private float mTranslate;
    private int mCurrentFlag = 1;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch ( action ){
            case MotionEvent.ACTION_DOWN:
                mIsDown = true;
                mPrevX = event.getX();
                mPrevY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if ( !mIsDown ) break;
                update( event.getX(), event.getY(),false);
                if ( Math.abs( event.getX() - mPrevX ) > radius*2 ){
                    if ( event.getX() > mPrevX ){
                        mCurrentFlag++;
                        mCurrentFlag %= 3;
                    } else {
                        mCurrentFlag--;
                        mCurrentFlag += 3;
                        mCurrentFlag %= 3;
                    }
                    event.setAction(MotionEvent.ACTION_CANCEL);
                    onTouchEvent(event);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if ( !mIsDown ) break;
                float endX = event.getX();
                float endY = event.getY();
                launchAnim(endX, endY, flag2TargetTranslate(mCurrentFlag) );
                mIsDown = false;
                //update(mPrevX,mPrevY,true);
                break;
        }
        return true;
    }
    ValueAnimator valueAnimator;
    private void launchAnim(float endX, float endY, float toTranslate) {
        if ( valueAnimator == null ){
            valueAnimator = new ValueAnimator();
            valueAnimator.setDuration(200);
            valueAnimator.addUpdateListener(this);
            valueAnimator.setInterpolator(new BounceInterpolator());
        }
        valueAnimator.setValues(PropertyValuesHolder.ofFloat("x", endX, mPrevX)
                , PropertyValuesHolder.ofFloat("y", endY, mPrevY)
                , PropertyValuesHolder.ofFloat("translate", mTranslate , toTranslate)
        );
        valueAnimator.cancel();
        valueAnimator.start();
    }


    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        Float x = (Float) animation.getAnimatedValue("x");
        Float y = (Float) animation.getAnimatedValue("y");
        mTranslate = (Float) animation.getAnimatedValue("translate");

        update(x, y, true);
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

        //canvas.drawArc(mRectF, 90, 180, true, mPaint);
    }

    private int flag2TargetTranslate( int flag ){
        return flag * radius*3 - radius*3;
    }

    private static float points2degress(float x1, float y1, float x2, float y2){
        double angrad = Math.atan2(y2-y1,x2-x1);
        return (float) Math.toDegrees(angrad);
    }


}
