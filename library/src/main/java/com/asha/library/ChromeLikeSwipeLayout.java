package com.asha.library;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;
import android.widget.ScrollView;

/**
 * Created by hzqiujiadi on 15/11/20.
 * hzqiujiadi ashqalcn@gmail.com
 */
public class ChromeLikeSwipeLayout extends ViewGroup {
    private static final String TAG = "ChromeLikeSwipeLayout";
    private static final int sThreshold = 300;

    private View mTarget; // the target of the gesture
    private View mChromeLikeView;
    private boolean mBeginDragging;
    private int mPrevTopOffset;
    private int mTopOffset;
    private int mTouchSlop;
    private float mTouchDownActor;


    public ChromeLikeSwipeLayout(Context context) {
        super(context);
        init();
    }

    public ChromeLikeSwipeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ChromeLikeSwipeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ChromeLikeSwipeLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();

        mChromeLikeView = new ChromeLikeView(getContext());
        addView(mChromeLikeView);
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent event)
    {
        float getY = event.getY();

        int action = event.getAction();

        if ( canChildDragDown() ) return false;

        switch ( action & MotionEvent.ACTION_MASK  )
        {
            case MotionEvent.ACTION_DOWN:
                mBeginDragging = false;
                mTouchDownActor = getY;
                mPrevTopOffset = mTopOffset;
                Log.d(TAG, String.format("onInterceptTouchEvent ACTION_DOWN"));
                break;
            case MotionEvent.ACTION_UP:
                mTouchDownActor = 0;
                mPrevTopOffset = mTopOffset;
                mPrevTopOffset = 0;
                mTopOffset = 0;
                mBeginDragging = false;
                Log.d(TAG, String.format("onInterceptTouchEvent ACTION_UP"));
                break;
            case MotionEvent.ACTION_MOVE:
                mTopOffset = (int) (getY - mTouchDownActor + mPrevTopOffset);
                Log.e(TAG, String.format("onInterceptTouchEvent ACTION_MOVE moving:%f",(getY - mTouchDownActor)));
                if ( !mBeginDragging && getY - mTouchDownActor > mTouchSlop )
                {
                    mBeginDragging = true;
                }
                Log.d(TAG, String.format("onInterceptTouchEvent ACTION_MOVE mBeginDragging=%b",mBeginDragging));
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                Log.d(TAG, String.format("onInterceptTouchEvent ACTION_POINTER_DOWN"));
                break;
            case MotionEvent.ACTION_POINTER_UP:
                Log.d(TAG, String.format("onInterceptTouchEvent ACTION_POINTER_UP"));
                break;
        }
        //mBeginDragging = true;
        Log.d(TAG, String.format("onInterceptTouchEvent return %b", mBeginDragging));

        return mBeginDragging;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        //first point
        float getY = event.getY();

        switch ( event.getAction() ) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, String.format("ACTION_DOWN"));
                break;
            case MotionEvent.ACTION_UP:
                mBeginDragging = false;

                Log.d(TAG,String.format("ACTION_UP"));
                mTouchDownActor = 0;
                mPrevTopOffset = mTopOffset;

                //reset
                mPrevTopOffset = 0;
                mTopOffset = 0;
                startAnim();

                break;
            case MotionEvent.ACTION_MOVE:
                mTopOffset = (int) (getY - mTouchDownActor + mPrevTopOffset);
                ensureTarget();
                View child = mTarget;
                int currentTop = child.getTop();
                //Log.d(TAG, String.format("ACTION_MOVE:%d,childTop:%d,< sThreshold:%b,", mTopOffset,currentTop,currentTop <= sThreshold));
                Log.d(TAG,String.format("ACTION_MOVE %b",mBeginDragging));
                if ( mBeginDragging ) {
                    if ( currentTop <= sThreshold ) {
                        if ( mTopOffset < sThreshold ) {
                            childOffsetTopAndBottom(mTopOffset - currentTop);
                        } else {
                            childOffsetTopAndBottom(sThreshold - currentTop);
                        }
                    } else {
                        childOffsetTopAndBottom(sThreshold - currentTop);
                    }
                }
                invalidate();
                //requestLayout();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                Log.d(TAG,String.format("ACTION_POINTER_DOWN"));
                break;
            case MotionEvent.ACTION_POINTER_UP:
                Log.d(TAG,String.format("ACTION_POINTER_UP"));
                break;
        }

        return true;
    }

    private void childOffsetTopAndBottom(int offset){
        mTarget.offsetTopAndBottom( offset );
        mChromeLikeView.offsetTopAndBottom( offset );
        mChromeLikeView.invalidate();
    }

    private void startAnim() {
        ensureTarget();
        final int from = mTarget.getTop();
        final int to = 0;

        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                //Log.e(TAG, "applyTransformation:" + interpolatedTime);
                float step = (to - from) * interpolatedTime + from;
                childOffsetTopAndBottom((int) (step - mTarget.getTop()));
            }
        };
        animation.setDuration(300);
        animation.setInterpolator(new DecelerateInterpolator());
        mTarget.clearAnimation();
        mTarget.startAnimation(animation);

    }

    @Override
    public void addView(View child, int index, LayoutParams params) {
        boolean touchAlwaysTrue =  child instanceof ScrollView
                || child instanceof AbsListView
                || child instanceof TouchAlwaysTrueLayout
                || child instanceof ChromeLikeView;

        if ( !touchAlwaysTrue ) child = TouchAlwaysTrueLayout.wrap(child);
        super.addView(child,index,params);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        if (getChildCount() == 0) {
            return;
        }
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        View child = mTarget;
        int childLeft = getPaddingLeft();
        int childTop = getPaddingTop()+ mTopOffset;
        int childWidth = width - getPaddingLeft() - getPaddingRight();
        int childHeight = height - getPaddingTop() - getPaddingBottom();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);

        child = mChromeLikeView;
        childLeft = getPaddingLeft();
        childTop = getPaddingTop() + mTopOffset - child.getMeasuredHeight();
        childWidth = width - getPaddingLeft() - getPaddingRight();
        childHeight = child.getMeasuredHeight();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);

    }

    @Override
    protected void onDraw(Canvas canvas) {

    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // Nope.
        //super.requestDisallowInterceptTouchEvent(b);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        final int width = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        mTarget.measure(MeasureSpec.makeMeasureSpec(
                width,
                MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
                getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));

        mChromeLikeView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(300, MeasureSpec.EXACTLY));
        Log.e(TAG,String.format("%d %d",mChromeLikeView.getMeasuredWidth(),mChromeLikeView.getMeasuredHeight()));
    }

    private boolean canChildDragDown()
    {
        ensureTarget();
        boolean result = ViewCompat.canScrollVertically(mTarget,-1) ;
        Log.e(TAG,"canChildDragDown:" + result + ",scrollY:" + mTarget.getScrollY() );
        return result ;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean dispatch = super.dispatchTouchEvent(ev);
        Log.d(TAG, String.format("dispatchTouchEvent ChromeLikeSwipeLayout %d dispatch=%b", ev.getAction(), dispatch));
        return dispatch;
    }

    private void ensureTarget() {
        // Don't bother getting the parent height if the parent hasn't been laid
        // out yet.
        if (mTarget == null) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (!child.equals(mChromeLikeView)) {
                    mTarget = child;
                    mChromeLikeView.bringToFront();
                    break;
                }
            }
        }
    }
}
