package com.asha;

import android.content.Context;
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
}
