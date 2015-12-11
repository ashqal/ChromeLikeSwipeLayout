package com.asha;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;
import android.widget.ScrollView;

import com.asha.library.R;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by hzqiujiadi on 15/11/20.
 * hzqiujiadi ashqalcn@gmail.com
 */
public class ChromeLikeSwipeLayout extends ViewGroup {

    static {
        try {
            sRecyclerViewClz = Class.forName("android.support.v7.widget.RecyclerView");
        } catch (ClassNotFoundException e) {
            // ignore
            // e.printStackTrace();
        }
    }

    public class StatusManager {
        private int mStatus = STATUS_IDLE;
        private static final int STATUS_IDLE       = 0;
        private static final int STATUS_CHANGED    = 1;
        private static final int STATUS_BUSY       = 2;
        private static final int STATUS_RESTORE    = 3;

        public void toIdle(){
            mStatus = STATUS_IDLE;
        }

        public void toBusy(){
            mStatus = STATUS_BUSY;
        }

        public void toRestore(){
            mStatus = STATUS_RESTORE;
        }

        public void toChanged(){
            mStatus = STATUS_CHANGED;
        }

        public boolean isChanged(){
            return  mStatus == STATUS_CHANGED;
        }

        public boolean isBusying(){
            return mStatus == STATUS_BUSY;
        }

        public boolean isRestoring(){
            return mStatus == STATUS_RESTORE;
        }

        public boolean isIdle(){
            return  mStatus == STATUS_IDLE;
        }
    }

    private static final String TAG = "ChromeLikeSwipeLayout";
    private static final int INVALID_POINTER = -1;
    private static final int sThreshold = dp2px(120);
    private static final int sThreshold2 = dp2px(400);
    private static Class sRecyclerViewClz;
    private View mTarget; // the target of the gesture
    private ChromeLikeLayout mChromeLikeLayout;
    private boolean mBeginDragging;
    private int mTopOffset;
    private int mTouchSlop;
    private int mCollapseDuration = 300;
    private float mTouchDownActor;
    private int mActivePointerId = INVALID_POINTER;
    private StatusManager mStatusManager = new StatusManager();

    private IOnItemSelectedListener mOnItemSelectedListener;
    private LinkedList<IOnExpandViewListener> mExpandListeners = new LinkedList<>();

    public ChromeLikeSwipeLayout(Context context) {
        this(context, null);
    }

    public ChromeLikeSwipeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChromeLikeSwipeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();

        Config config = makeConfig();
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ChromeLikeSwipeLayout,defStyleAttr,0);
        if ( ta != null ){
            if (ta.hasValue(R.styleable.ChromeLikeSwipeLayout_circleColor))
                config.circleColor(ta.getColor(R.styleable.ChromeLikeSwipeLayout_circleColor,Config.DEFAULT));
            if ( ta.hasValue(R.styleable.ChromeLikeSwipeLayout_gap))
                config.gap(ta.getDimensionPixelOffset(R.styleable.ChromeLikeSwipeLayout_gap,Config.DEFAULT));
            if ( ta.hasValue(R.styleable.ChromeLikeSwipeLayout_radius))
                config.radius(ta.getDimensionPixelOffset(R.styleable.ChromeLikeSwipeLayout_radius,Config.DEFAULT));
            if ( ta.hasValue(R.styleable.ChromeLikeSwipeLayout_collapseDuration))
                config.collapseDuration(ta.getInt(R.styleable.ChromeLikeSwipeLayout_collapseDuration,Config.DEFAULT));
            if ( ta.hasValue(R.styleable.ChromeLikeSwipeLayout_rippleDuration))
                config.rippleDuration(ta.getInt(R.styleable.ChromeLikeSwipeLayout_rippleDuration,Config.DEFAULT));
            if ( ta.hasValue(R.styleable.ChromeLikeSwipeLayout_gummyDuration))
                config.gummyDuration(ta.getInt(R.styleable.ChromeLikeSwipeLayout_gummyDuration,Config.DEFAULT));
            ta.recycle();
        }
        config.setTo(this);
    }

    private void init() {
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();

        mChromeLikeLayout = new ChromeLikeLayout(getContext());
        mChromeLikeLayout.setRippleListener(new ChromeLikeLayout.IOnRippleListener() {
            @Override
            public void onRippleAnimFinished(int index) {
                mStatusManager.toRestore();
                if ( !mAnimationStarted ) launchResetAnim();
                mBeginDragging = false;
                if ( mOnItemSelectedListener != null )
                    mOnItemSelectedListener.onItemSelected(index);
            }
        });
        addOnExpandViewListener(mChromeLikeLayout);
        addView(mChromeLikeLayout);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if ( canChildDragDown() ) return false;
        if ( mAnimationStarted ) return false;

        switch ( action & MotionEvent.ACTION_MASK  ) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = MotionEventCompat.getPointerId(event, 0);
                final float initialDownY = getMotionEventY(event, mActivePointerId);
                if ( initialDownY == -1 ) return false;
                if ( mBeginDragging ){
                    mTouchDownActor = getNewTouchDownActor(initialDownY);
                    return true;
                }
                mTouchDownActor = initialDownY;
                mBeginDragging = false;
                mChromeLikeLayout.onActionDown();
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mActivePointerId = INVALID_POINTER;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    //Log.e(TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }
                final float y = getMotionEventY(event, mActivePointerId);
                if (y == -1) {
                    return false;
                }
                // if diff > mTouchSlop
                // let's drag!
                if ( !mBeginDragging && y - mTouchDownActor > mTouchSlop ) {
                    mBeginDragging = true;
                    mStatusManager.toChanged();
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

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        final int action = MotionEventCompat.getActionMasked(event);
        int pointerIndex = MotionEventCompat.findPointerIndex(event, mActivePointerId);
        if (pointerIndex < 0) {
            //Log.e(TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
            return false;
        }
        final float y = MotionEventCompat.getY(event, pointerIndex);

        mTopOffset = calculateTopOffset(y - mTouchDownActor);
        boolean isExpanded = mTopOffset >= sThreshold && mBeginDragging;
        //first point

        switch ( action ) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = MotionEventCompat.getPointerId(event, 0);
                break;
            case MotionEvent.ACTION_CANCEL:
                mChromeLikeLayout.onActionUpOrCancel(isExpanded);
                break;
            case MotionEvent.ACTION_UP:
                executeAction(isExpanded);
                mChromeLikeLayout.onActionUpOrCancel(isExpanded);
                mActivePointerId = INVALID_POINTER;
                break;
            case MotionEvent.ACTION_MOVE:
                mChromeLikeLayout.onActionMove(event, pointerIndex, isExpanded);
                ensureTarget();
                View child = mTarget;
                int currentTop = child.getTop();
                if ( mBeginDragging ) {
                    if ( !isExpanded )
                        notifyOnExpandListeners( currentTop * 1.0f / sThreshold, true);
                    childOffsetTopAndBottom(currentTop,mTopOffset);
                }
                //requestLayout();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                pointerIndex = MotionEventCompat.getActionIndex(event);
                if (pointerIndex < 0) {
                    //Log.e(TAG, "Got ACTION_POINTER_DOWN event but have an invalid action index.");
                    return false;
                }
                mActivePointerId = MotionEventCompat.getPointerId(event, pointerIndex);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                break;
        }
        return true;
    }

    private float getNewTouchDownActor(float y){
        float diff;
        if ( mTopOffset < 0 ){
            diff = 0;
        } else if( mTopOffset > sThreshold ){
            diff = sThreshold;
        } else {
            diff = mTopOffset;
        }
        return y - diff;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }
    }

    private float getMotionEventY(MotionEvent ev, int activePointerId) {
        final int index = MotionEventCompat.findPointerIndex(ev, activePointerId);
        if (index < 0) {
            return -1;
        }
        return MotionEventCompat.getY(ev, index);
    }

    private int calculateTopOffset(float original){
        float basic = original * 0.6f;
        if ( basic > sThreshold ){
            basic = sThreshold + (basic - sThreshold) * 0.3f;
        }
        return (int) basic;
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

        mChromeLikeLayout.offsetTopAndBottom( target );
        requestLayout();
    }

    // only execute on ACTION_UP
    private void executeAction(boolean isExpanded) {

        if ( isExpanded ){
            mStatusManager.toBusy();
        } else {
            if ( !mStatusManager.isIdle() ) return;
            if ( mAnimationStarted ) return;
            launchResetAnim();
            mBeginDragging = false;
        }
    }

    private void launchResetAnim(){
        boolean isFromCancel = !mStatusManager.isRestoring();
        launchResetAnim(isFromCancel);
    }

    private boolean mAnimationStarted;
    private void launchResetAnim( final boolean isFromCancel ){
        ensureTarget();

        final int from = mTarget.getTop();
        final int to = 0;
        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                float step = (to - from) * interpolatedTime + from;
                notifyOnExpandListeners( mTarget.getTop() * 1.0f / sThreshold ,isFromCancel);
                childOffsetTopAndBottom( mTarget.getTop(), Math.round(step) );
            }
        };
        animation.setDuration(mCollapseDuration);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.setAnimationListener(new AnimationListenerAdapter() {
            @Override
            public void onAnimationEnd(Animation animation) {
                mAnimationStarted = false;
                mStatusManager.toIdle();
            }
        });
        this.clearAnimation();
        this.startAnimation(animation);
        mAnimationStarted = true;
    }

    @Override
    public void addView(View child, int index, LayoutParams params) {

        boolean touchAlwaysTrue =  child instanceof ScrollView
                || child instanceof AbsListView
                || (sRecyclerViewClz != null && child.getClass().isAssignableFrom(sRecyclerViewClz))
                || child instanceof TouchAlwaysTrueLayout
                || child instanceof ChromeLikeLayout;

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
        int childTop = child.getTop();
        int childWidth = width - getPaddingLeft() - getPaddingRight();
        int childHeight = height - getPaddingBottom();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);

        child = mChromeLikeLayout;
        childLeft = getPaddingLeft();
        childTop = mTarget.getTop() - child.getMeasuredHeight();
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
        final int height = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
        mTarget.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        mChromeLikeLayout.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
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
        return ViewCompat.canScrollVertically(mTarget,-1);
    }

    private void ensureTarget() {
        // Don't bother getting the parent height if the parent hasn't been laid
        // out yet.
        if (mTarget == null) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (!child.equals(mChromeLikeLayout)) {
                    mTarget = child;
                    mChromeLikeLayout.bringToFront();
                    break;
                }
            }
        }
    }

    private void setCollapseDuration(int collapseDuration) {
        this.mCollapseDuration = collapseDuration;
    }

    private void setConfig(Config config){
        if ( config.mIcons != null )
            mChromeLikeLayout.setIcons(config.mIcons);
        if ( config.mBackgroundResId != Config.DEFAULT )
            mChromeLikeLayout.setBackgroundResource(config.mBackgroundResId);
        if ( config.mBackgroundColor != Config.DEFAULT )
            mChromeLikeLayout.setBackgroundColor(config.mBackgroundColor);
        if ( config.mCircleColor != Config.DEFAULT )
            mChromeLikeLayout.setCircleColor(config.mCircleColor);
        if ( config.mRadius != Config.DEFAULT )
            mChromeLikeLayout.setRadius(config.mRadius);
        if ( config.mRadius != Config.DEFAULT )
            mChromeLikeLayout.setGap(config.mGap);
        if ( config.mRippleDuration != Config.DEFAULT )
            mChromeLikeLayout.setRippleDuration(config.mRippleDuration);
        if ( config.mGummyDuration != Config.DEFAULT )
            mChromeLikeLayout.setGummyDuration(config.mGummyDuration);
        if ( config.mCollapseDuration != Config.DEFAULT )
            setCollapseDuration(config.mCollapseDuration);

        mOnItemSelectedListener = config.mOnItemSelectedListener;
    }

    public void notifyOnExpandListeners(float fraction, boolean isFromCancel){
        fraction = fraction < 1 ? fraction : 1;
        for ( IOnExpandViewListener listener : mExpandListeners )
            listener.onExpandView(fraction,isFromCancel);
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
        void onExpandView(float fraction, boolean isFromCancel);
    }

    public static Config makeConfig(){
        return new Config();
    }

    public static class Config{
        private List<Integer> mIcons;
        private IOnItemSelectedListener mOnItemSelectedListener;
        private int mCircleColor = DEFAULT;
        private int mBackgroundResId = DEFAULT;
        private int mBackgroundColor = DEFAULT;
        private int mRadius = DEFAULT;
        private int mGap = DEFAULT;
        private int mCollapseDuration = DEFAULT;
        private int mRippleDuration = DEFAULT;
        private int mGummyDuration = DEFAULT;
        private static final int DEFAULT = -1;

        private Config(){

        }

        public Config addIcon(@DrawableRes int drawableResId){
            if ( mIcons == null ) mIcons = new LinkedList<>();
            mIcons.add(drawableResId);
            return this;
        }

        public Config background(@DrawableRes int backgroundResId){
            this.mBackgroundResId = backgroundResId;
            return this;
        }

        public Config backgroundColor(@ColorInt int color){
            this.mBackgroundColor = color;
            return this;
        }

        public Config circleColor(@ColorInt int color){
            this.mCircleColor = color;
            return this;
        }

        public Config listenItemSelected(IOnItemSelectedListener listener){
            this.mOnItemSelectedListener = listener;
            return this;
        }

        public Config radius(int radius){
            this.mRadius = radius;
            return this;
        }

        public Config gap(int gap){
            this.mGap = gap;
            return this;
        }

        public Config collapseDuration(int duration){
            this.mCollapseDuration = duration;
            return this;
        }

        public Config rippleDuration(int duration){
            this.mRippleDuration = duration;
            return this;
        }

        public Config gummyDuration(int duration){
            this.mGummyDuration = duration;
            return this;
        }

        public void setTo(ChromeLikeSwipeLayout chromeLikeSwipeLayout){
            chromeLikeSwipeLayout.setConfig(this);
        }
    }

    public static int dp2px(float valueInDp) {
        final float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int) (valueInDp * scale + 0.5f);
    }

    public interface IOnItemSelectedListener{
        void onItemSelected(int index);
    }

}
