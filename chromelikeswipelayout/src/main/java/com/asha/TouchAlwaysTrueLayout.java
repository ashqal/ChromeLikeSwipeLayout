package com.asha;

import android.content.Context;
import android.graphics.PointF;
import android.support.v4.view.ViewCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by hzqiujiadi on 15/11/23.
 * hzqiujiadi ashqalcn@gmail.com
 */
public class TouchAlwaysTrueLayout extends ViewGroup {
    public TouchAlwaysTrueLayout(Context context) {
        super(context);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
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
}
