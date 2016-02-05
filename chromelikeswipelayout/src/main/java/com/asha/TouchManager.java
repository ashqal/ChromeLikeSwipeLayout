package com.asha;

import android.graphics.PointF;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;

import static com.asha.ChromeLikeSwipeLayout.dp2px;

/**
 * Created by hzqiujiadi on 15/12/22.
 * hzqiujiadi ashqalcn@gmail.com
 */
public class TouchManager {
    public static final int INVALID_POINTER = -1;
    private boolean mBeginDragging;
    private int mTopOffset;
    private int mTouchSlop;
    private float mTouchDownActor;
    private int mActivePointerId = INVALID_POINTER;
    private PointF mTmpPoint = new PointF();
    private ITouchCallback mTouchCallback;
    private int mThreshold = dp2px(120);
    private static final int sThreshold2 = dp2px(400);
    private int mMotionX;
    private boolean mInterceptEnabled = true;

    public TouchManager(ITouchCallback mTouchHelper) {
        this.mTouchCallback = mTouchHelper;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            setActivePointerId(ev, newPointerIndex);
        }
    }

    private void setActivePointerId(MotionEvent event, int defaultId){
        mActivePointerId = MotionEventCompat.getPointerId(event, defaultId);
        final float initialDownY = getCurrentMotionEventY(event);
        if ( initialDownY == -1 ) return;
        if ( mBeginDragging ){
            mTouchDownActor = motionY2TouchDown(initialDownY);
        }
    }

    private void resetActivePointerId(){
        mActivePointerId = INVALID_POINTER;
    }

    private float getCurrentMotionEventY(MotionEvent ev) {
        final int index = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
        if (index < 0) {
            return -1;
        }
        return MotionEventCompat.getY(ev, index);
    }

    public void setTouchSlop(int touchSlop) {
        this.mTouchSlop = touchSlop;
    }

    public void endDrag(){
        mBeginDragging = false;
    }

    public float calExpandProgress(int currentTop){
        return currentTop * 1.0f / mThreshold;
    }

    public int calTargetTopOffset(int currentTop){
        return calTargetTopOffset(currentTop,getTopOffset());
    }

    public int calTargetTopOffset(int currentTop, int offset){
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
        return target;
    }

    private void setTopOffset(float y) {
        mTopOffset = motionY2TopOffset(y);
    }

    public int getTopOffset() {
        return mTopOffset;
    }

    public boolean isBeginDragging() {
        return mBeginDragging;
    }

    public PointF event2Point(MotionEvent event){
        mTmpPoint.set(event.getX(),event.getY());
        return mTmpPoint;
    }

    private float motionY2TouchDown(float y){
        float diff;
        if ( mTopOffset < 0 ){
            diff = 0;
        } else if( mTopOffset > mThreshold){
            diff = (mTopOffset - mThreshold) / 0.3f / 0.6f + mThreshold / 0.6f;
        } else {
            diff = mTopOffset / 0.6f;
        }
        return y - diff;
    }

    private int motionY2TopOffset(float y){
        float original = y - mTouchDownActor;
        float basic = original * 0.6f;
        if ( basic > mThreshold){
            basic = mThreshold + (basic - mThreshold) * 0.3f;
        }
        return (int) basic;
    }

    public boolean onFeedInterceptEvent(MotionEvent event){
        int action = event.getAction();
        switch ( action & MotionEvent.ACTION_MASK  ) {
            case MotionEvent.ACTION_DOWN:
                setActivePointerId(event, 0);
                if ( mBeginDragging ){
                    return true;
                }
                final float initialDownY = getCurrentMotionEventY(event);
                if (initialDownY == -1) return false;
                mTouchDownActor = initialDownY;
                mBeginDragging = false;
                if ( mTouchCallback != null ) mTouchCallback.onActionDown();
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                resetActivePointerId();
                break;
            case MotionEvent.ACTION_MOVE:
                if ( mActivePointerId == TouchManager.INVALID_POINTER) {
                    //Log.e(TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }
                final float y = getCurrentMotionEventY(event);
                if (y == -1) {
                    return false;
                }
                // if diff > mTouchSlop
                // let's drag!
                if ( mInterceptEnabled && !mBeginDragging && y - mTouchDownActor > mTouchSlop ) {
                    mBeginDragging = true;
                    if (mTouchCallback != null) mTouchCallback.onBeginDragging();
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                break;
        }
        return mBeginDragging;
    }

    public boolean onFeedTouchEvent(MotionEvent event){
        final int action = MotionEventCompat.getActionMasked(event);
        int pointerIndex = MotionEventCompat.findPointerIndex(event, mActivePointerId);
        if (pointerIndex < 0) {
            //Log.e(TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
            return false;
        }
        final float y = MotionEventCompat.getY(event, pointerIndex);
        setTopOffset(y);

        boolean isExpanded = mTopOffset >= mThreshold && mBeginDragging;
        //first point

        switch ( action ) {
            case MotionEvent.ACTION_DOWN:
                setActivePointerId(event, 0);
                break;
            case MotionEvent.ACTION_CANCEL:
                if ( mTouchCallback != null ) mTouchCallback.onActionCancel(isExpanded);
                break;
            case MotionEvent.ACTION_UP:
                if ( mTouchCallback != null ) mTouchCallback.onActionUp(isExpanded);
                resetActivePointerId();
                break;
            case MotionEvent.ACTION_MOVE:
                mMotionX = (int) MotionEventCompat.getX(event,pointerIndex);
                if ( mTouchCallback != null ) mTouchCallback.onActionMove(isExpanded, this);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                pointerIndex = MotionEventCompat.getActionIndex(event);
                if (pointerIndex < 0) {
                    //Log.e(TAG, "Got ACTION_POINTER_DOWN event but have an invalid action index.");
                    return false;
                }
                setActivePointerId(event, pointerIndex);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                break;
        }
        return true;
    }

    public int getMotionX() {
        return mMotionX;
    }

    public void setInterceptEnabled(boolean interceptEnabled) {
        this.mInterceptEnabled = interceptEnabled;
    }

    public void setMaxHeight(int maxHeight) {
        this.mThreshold = maxHeight;
    }

    public interface ITouchCallback {
        void onActionDown();
        void onActionUp(boolean isExpanded);
        void onActionCancel(boolean isExpanded);
        void onActionMove(boolean isExpanded, TouchManager touchManager);
        void onBeginDragging();
    }

}
