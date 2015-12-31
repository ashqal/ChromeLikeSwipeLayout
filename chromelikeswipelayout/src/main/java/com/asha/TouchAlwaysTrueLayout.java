package com.asha;

import android.content.Context;
import android.graphics.PointF;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.ViewCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by hzqiujiadi on 15/11/23.
 * hzqiujiadi ashqalcn@gmail.com
 */
public class TouchAlwaysTrueLayout extends ViewGroup implements NestedScrollingChild {

    private final NestedScrollingChildHelper mScrollingChildHelper;
    /**
     * Used during scrolling to retrieve the new offset within the window.
     */
    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private int mNestedYOffset;
    private int mLastMotionY;

    public TouchAlwaysTrueLayout(Context context) {
        super(context);
        mScrollingChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();

        MotionEvent vtev = MotionEvent.obtain(event);
        if (action == MotionEvent.ACTION_DOWN) {
            mNestedYOffset = 0;
        }
        vtev.offsetLocation(0, mNestedYOffset);

        switch (action & MotionEventCompat.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = (int) event.getY();
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                stopNestedScroll();
                break;
            case MotionEvent.ACTION_MOVE:
                final int y = (int) MotionEventCompat.getY(event, 0);
                int deltaY = mLastMotionY - y;
                if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset)) {
                    deltaY -= mScrollConsumed[1];
                    vtev.offsetLocation(0, mScrollOffset[1]);
                    mNestedYOffset += mScrollOffset[1];
                }
                final int scrolledDeltaY = getScrollY();
                final int unconsumedY = deltaY - scrolledDeltaY;
                dispatchNestedScroll(0, scrolledDeltaY, 0, unconsumedY, mScrollOffset);
                break;
        }

        return true;
    }

    public static ViewGroup wrap(View view){
        Context context = view.getContext();
        TouchAlwaysTrueLayout wrapper = new TouchAlwaysTrueLayout(context);
        wrapper.addView(view, LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);
        return wrapper;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for ( int i = 0 ; i < getChildCount() ; i++ ){
            getChildAt(i).layout(l,t,r,b);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildren(widthMeasureSpec,heightMeasureSpec);
    }

    public boolean canChildDragDown(PointF pointF){
        return canChildDragDownTraversal(this, pointF.x, pointF.y);
    }

    private boolean canChildDragDownTraversal(View view, float x, float y){
        if ( !inside(view, x, y) ) return false;
        if ( ViewCompat.canScrollVertically(view,-1) ) return true;
        boolean canDragDown;
        if ( view instanceof ViewGroup ){
            ViewGroup vp = (ViewGroup) view;
            int count = vp.getChildCount();
            float newX = x - view.getLeft();
            float newY = y - view.getTop();
            View sub;
            for ( int i = 0 ; i < count; i++ ){
                sub = vp.getChildAt(i);
                canDragDown = canChildDragDownTraversal(sub,newX,newY);
                if ( canDragDown ) return true;
            }
        }
        return false;
    }

    private boolean inside(View view, float x, float y ){
        if ( view.getLeft() <= x && view.getRight() >= x && view.getTop() <= y && view.getBottom() >= y )
            return true;
        else return false;
    }

    // NestedScrollingChild
    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mScrollingChildHelper.isNestedScrollingEnabled();

    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return mScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }
}
