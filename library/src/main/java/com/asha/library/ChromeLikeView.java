package com.asha.library;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.support.v4.animation.AnimatorCompatHelper;
import android.support.v4.animation.AnimatorListenerCompat;
import android.support.v4.animation.AnimatorUpdateListenerCompat;
import android.support.v4.animation.ValueAnimatorCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;


/**
 * Created by hzqiujiadi on 15/11/18.
 * hzqiujiadi ashqalcn@gmail.com
 */
public class ChromeLikeView extends View  {
    private static final String TAG = "ChromeLikeView";
    private static final float sMagicNumber = 0.55228475f;
    private static Interpolator sBounceInterpolator = new BounceInterpolator();
    private static Interpolator sInterpolator = new FastOutSlowInInterpolator();
    private Paint mPaint;
    private Paint mCirclePaint;
    private Paint mDebugPaint;
    private Path mPath;
    private float mPrevX;
    private float mDegrees;
    private boolean mIsDown;
    private float mTranslate;
    private int mCurrentFlag = 1;
    private int mSize = 3;
    private int mRadius = 80;
    private GummyAnimatorHelper mGummyAnimatorHelper = new GummyAnimatorHelper();
    private RippleAnimatorHelper mRippleAnimatorHelper = new RippleAnimatorHelper();

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

        mPath = new Path();

        reset();
    }

    public void onActionDown(MotionEvent event){
        mIsDown = true;
        mPrevX = event.getX();
        reset();
    }

    public void onActionMove(MotionEvent event){
        if ( !mIsDown ) return;
        float currentX = event.getX();
        if ( mGummyAnimatorHelper.isAnimationStarted() ){
            mGummyAnimatorHelper.updateFromX(currentX);
            return;
        }
        update( currentX, mPrevX, mRadius, false );
        if ( Math.abs( currentX - mPrevX ) > mRadius * 1.5 ){
            if ( currentX > mPrevX ){
                mCurrentFlag++;
                mCurrentFlag %= mSize;
            } else {
                mCurrentFlag--;
                mCurrentFlag += mSize;
                mCurrentFlag %= mSize;
            }
            mGummyAnimatorHelper.launchAnim(
                    currentX
                    , mPrevX
                    , mTranslate
                    , flag2TargetTranslate(mCurrentFlag) );
        }
    }

    public void onActionUpOrCancel(MotionEvent event, boolean isRipple){
        if ( !mIsDown ) return;
        float currentX = event.getX();
        mIsDown = false;
        if ( isRipple ){
            if ( mRippleAnimatorHelper.isAnimationStarted() ) return;
            mRippleAnimatorHelper.launchAnim(mRadius,getMeasuredWidth());
        } else {
            if ( mGummyAnimatorHelper.isAnimationStarted() ) return;
            mGummyAnimatorHelper.launchAnim(
                    currentX
                    , mPrevX
                    , mTranslate
                    , flag2TargetTranslate(mCurrentFlag) );
        }

    }

    private void update(float currentX, float prevX, int radius, boolean animate){
        //mRadius
        update(currentX,0,prevX,0,radius,animate);
    }

    private void updateAlpha( float alpha ){
        mPaint.setAlpha(Math.round(255 * alpha));
    }

    private void reset(){
        updateAlpha(1);
        update(0, 0, mRadius, false);
        mCurrentFlag = 1;
        mTranslate = flag2TargetTranslate(mCurrentFlag);
    }

    private void update(float currentX, float currentY, float prevX, float prevY, int radius, boolean animate ){
        float distance = distance(prevX, prevY, currentX, currentY);
        float tempDegree = points2Degrees(prevX, prevY, currentX, currentY);
        if ( animate ){
            if ( Math.abs( mDegrees - tempDegree ) > 5 ) distance = -distance;
        } else {
            //if ( distance < mTouchSlop ) distance = 0;
            mDegrees = tempDegree;
        }
        float realLong = radius + distance;
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

        canvas.drawCircle(centerX, centerY, mRadius / 8, mCirclePaint);
        canvas.drawCircle(centerX + mRadius *3,centerY, mRadius /8,mCirclePaint);
        canvas.drawCircle(centerX - mRadius *3,centerY, mRadius /8,mCirclePaint);

        //canvas.drawCircle(mPrevX,mPrevY,5,mDebugPaint);
    }

    private int flag2TargetTranslate( int flag ){
        return flag * mRadius *3 - mRadius *3;
    }

    private static float points2Degrees(float x1, float y1, float x2, float y2){
        double angle = Math.atan2(y2-y1,x2-x1);
        return (float) Math.toDegrees(angle);
    }


    public class RippleAnimatorHelper implements AnimatorUpdateListenerCompat, AnimatorListenerCompat {

        private float mAnimFromRadius;
        private float mAnimToRadius;
        private ValueAnimatorCompat mRippleAnimator;
        private boolean mAnimationStarted;

        @Override
        public void onAnimationCancel(ValueAnimatorCompat animation) {
            mAnimationStarted = false;
        }

        @Override
        public void onAnimationRepeat(ValueAnimatorCompat animation) {

        }

        @Override
        public void onAnimationUpdate(ValueAnimatorCompat animation) {
            float interpolation = sInterpolator.getInterpolation(animation.getAnimatedFraction());
            int currentRadius = FloatEvaluator.evaluate(interpolation,mAnimFromRadius,mAnimToRadius).intValue();
            update(0, 0, currentRadius, true);
            updateAlpha(1-interpolation);
        }

        @Override
        public void onAnimationStart(ValueAnimatorCompat animation) {
            mAnimationStarted = true;
        }

        @Override
        public void onAnimationEnd(ValueAnimatorCompat animation) {
            mAnimationStarted = false;
        }

        public void launchAnim(float fromRadius, float toRadius) {

            if ( mRippleAnimator == null ){
                mRippleAnimator = AnimatorCompatHelper.emptyValueAnimator();
                mRippleAnimator.setDuration(300);
                mRippleAnimator.addUpdateListener(this);
                mRippleAnimator.setTarget(ChromeLikeView.this);
                mRippleAnimator.addListener(this);
            }
            mAnimFromRadius = fromRadius;
            mAnimToRadius = toRadius;
            mRippleAnimator.cancel();
            mRippleAnimator.start();
        }

        public boolean isAnimationStarted() {
            return mAnimationStarted;
        }
    }


    public class GummyAnimatorHelper implements AnimatorUpdateListenerCompat, AnimatorListenerCompat {

        private float mAnimFromX;
        private float mAnimToX;
        private float mAnimFromTranslate;
        private float mAnimToTranslate;
        private ValueAnimatorCompat mGummyAnimator;
        private boolean mAnimationStarted;

        @Override
        public void onAnimationCancel(ValueAnimatorCompat animation) {
            mAnimationStarted = false;
        }

        @Override
        public void onAnimationRepeat(ValueAnimatorCompat animation) {

        }

        @Override
        public void onAnimationUpdate(ValueAnimatorCompat animation) {
            float interpolation = sBounceInterpolator.getInterpolation(animation.getAnimatedFraction());
            Float currentX = FloatEvaluator.evaluate(interpolation,mAnimFromX,mAnimToX);
            mTranslate = FloatEvaluator.evaluate(interpolation, mAnimFromTranslate, mAnimToTranslate);
            update(currentX, mAnimToX, mRadius, true);
        }

        @Override
        public void onAnimationStart(ValueAnimatorCompat animation) {
            mAnimationStarted = true;
        }

        @Override
        public void onAnimationEnd(ValueAnimatorCompat animation) {
            mAnimationStarted = false;
            mPrevX = mAnimFromX;
        }

        public void launchAnim(float fromX, float toX, float fromTranslate, float toTranslate) {

            if ( mGummyAnimator == null ){
                mGummyAnimator = AnimatorCompatHelper.emptyValueAnimator();
                mGummyAnimator.setDuration(200);
                mGummyAnimator.addUpdateListener(this);
                mGummyAnimator.setTarget(ChromeLikeView.this);
                mGummyAnimator.addListener(this);
            }
            mAnimFromX = fromX;
            mAnimToX = toX;
            mAnimFromTranslate = fromTranslate;
            mAnimToTranslate = toTranslate;
            mGummyAnimator.cancel();
            mGummyAnimator.start();
        }

        public boolean isAnimationStarted() {
            return mAnimationStarted;
        }

        public void updateFromX(float currentX) {
            mAnimFromX = currentX;
        }
    }

    public static class FloatEvaluator {

        public static Float evaluate(float fraction, Number startValue, Number endValue) {
            float startFloat = startValue.floatValue();
            return startFloat + fraction * (endValue.floatValue() - startFloat);
        }
    }
}
