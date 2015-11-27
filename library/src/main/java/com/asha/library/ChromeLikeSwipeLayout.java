package com.asha.library;

import android.annotation.TargetApi;
import android.content.Context;
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

import com.asha.library.ChromeLikeView.IOnRippleListener;

import java.util.LinkedList;

/**
 * Created by hzqiujiadi on 15/11/20.
 * hzqiujiadi ashqalcn@gmail.com
 */
public class ChromeLikeSwipeLayout extends ViewGroup {
    private static final String TAG = "ChromeLikeSwipeLayout";
    private static final int sThreshold = 250;

    private View mTarget; // the target of the gesture
    private ChromeLikeView mChromeLikeView;
    private boolean mBeginDragging;
    private int mTopOffset;
    private int mTouchSlop;
    private float mTouchDownActor;
    private boolean mIsBusy;
    private LinkedList<IOnExpandViewListener> mExpandListeners = new LinkedList<>();
    private static final int sThreshold2 = 800;


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
        mChromeLikeView.setRippleListener(new IOnRippleListener() {
            @Override
            public void onRippleAnimFinished() {
                mIsBusy = false;
                launchResetAnim(false);
                mBeginDragging = false;
            }
        });
        addOnExpandViewListener(mChromeLikeView);
        addView(mChromeLikeView);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event)
    {
        float getY = event.getY();
        int action = event.getAction();
        if ( canChildDragDown() ) return false;

        switch ( action & MotionEvent.ACTION_MASK  ) {

            case MotionEvent.ACTION_DOWN:
                if ( mBeginDragging ){
                    //Log.d(TAG, String.format("onInterceptTouchEvent ACTION_DOWN %d %d",mTopOffset,sThreshold));
                    float diff;
                    if ( mTopOffset < 0 ){
                        diff = 0;
                    } else if( mTopOffset > sThreshold ){
                        diff = sThreshold;
                    } else {
                        diff = mTopOffset;
                    }
                    mTouchDownActor = getY - diff;
                    return true;
                }
                mTouchDownActor = getY;
                mBeginDragging = false;
                mChromeLikeView.onActionDown(event);
                break;
            case MotionEvent.ACTION_UP:
                Log.d(TAG, String.format("onInterceptTouchEvent ACTION_UP"));
                break;
            case MotionEvent.ACTION_MOVE:
                Log.e(TAG, String.format("onInterceptTouchEvent ACTION_MOVE moving:%f",(getY - mTouchDownActor)));
                if ( !mBeginDragging && getY - mTouchDownActor > mTouchSlop ) {
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
        boolean isExpanded = mTopOffset >= sThreshold;
        //first point
        float getY = event.getY();

        switch ( event.getAction() ) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_CANCEL:
                mChromeLikeView.onActionUpOrCancel(event,isExpanded);
                break;
            case MotionEvent.ACTION_UP:
                mChromeLikeView.onActionUpOrCancel(event,isExpanded);
                executeAction();

                break;
            case MotionEvent.ACTION_MOVE:
                mChromeLikeView.onActionMove(event,isExpanded);
                mTopOffset = (int) ((getY - mTouchDownActor) * 0.6);
                ensureTarget();
                View child = mTarget;
                int currentTop = child.getTop();
                if ( mBeginDragging ) {
                    if ( !isExpanded )
                        notifyOnExpandListeners( currentTop * 1.0f / sThreshold );
                    childOffsetTopAndBottom(currentTop,mTopOffset);
                }
                invalidate();
                //requestLayout();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                //Log.d(TAG,String.format("ACTION_POINTER_DOWN"));
                break;
            case MotionEvent.ACTION_POINTER_UP:
                //Log.d(TAG,String.format("ACTION_POINTER_UP"));
                break;
        }

        return true;
    }


    private void childOffsetTopAndBottom(int currentTop, int offset){
        int target;
        if ( currentTop <= sThreshold2 ) {
            if ( offset < 0 ){
                target = 0 - currentTop;
            } else if ( offset < sThreshold2 ) {
                target = offset - currentTop;
            } else {
                target = sThreshold2 - currentTop;
            }
        } else {
            target = sThreshold2 - currentTop;
        }
        mTarget.offsetTopAndBottom( target );

        mChromeLikeView.offsetTopAndBottom( target );
        requestLayout();
    }

    private void executeAction() {

        if ( mTopOffset >= sThreshold ){
            mIsBusy = true;
        } else {
            if ( mIsBusy ) return;
            launchResetAnim(true);
            mBeginDragging = false;
        }
    }

    private void launchResetAnim(final boolean isFromCancel ){
        ensureTarget();

        final int from = mTarget.getTop();
        final int to = 0;
        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                float step = (to - from) * interpolatedTime + from;
                if ( isFromCancel )
                    notifyOnExpandListeners( mTarget.getTop() * 1.0f / sThreshold );
                childOffsetTopAndBottom( mTarget.getTop(), Math.round(step) );
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
        int childTop = getPaddingTop() + child.getTop();
        int childWidth = width - getPaddingLeft() - getPaddingRight();
        int childHeight = height - getPaddingTop() - getPaddingBottom();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);

        child = mChromeLikeView;
        childLeft = getPaddingLeft();
        childTop = childTop - child.getMeasuredHeight();
        childWidth = width - getPaddingLeft() - getPaddingRight();
        childHeight = child.getMeasuredHeight();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
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
                MeasureSpec.makeMeasureSpec(mTarget.getTop(), MeasureSpec.EXACTLY));
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // Nope.
        //super.requestDisallowInterceptTouchEvent(b);
    }


    private boolean canChildDragDown()
    {
        ensureTarget();
        boolean result = ViewCompat.canScrollVertically(mTarget,-1) ;
        //Log.e(TAG,"canChildDragDown:" + result + ",scrollY:" + mTarget.getScrollY() );
        return result ;
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


    public void notifyOnExpandListeners(float fraction){
        for ( IOnExpandViewListener listener : mExpandListeners )
            listener.onExpandView(fraction);
    }

    public void addOnExpandViewListener(IOnExpandViewListener listener){
        mExpandListeners.add(listener);
    }

    public void removeOnExpandViewListener(IOnExpandViewListener listener){
        mExpandListeners.remove(listener);
    }

    public void removeAllOnExpandViewListener(){
        mExpandListeners.clear();
    }

    public interface IOnExpandViewListener {
        void onExpandView(float fraction);
    }

}
