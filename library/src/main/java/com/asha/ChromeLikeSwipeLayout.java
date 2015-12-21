package com.asha;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ScrollingView;
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

    private static final String TAG = "ChromeLikeSwipeLayout";
    private static final int sThreshold = dp2px(120);
    private static final int sThreshold2 = dp2px(400);
    private View mTarget; // the target of the gesture
    private ChromeLikeLayout mChromeLikeLayout;
    private int mCollapseDuration = 300;
    private StatusManager mStatusManager = new StatusManager();
    private TouchManager mTouchManager = new TouchManager();
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
        mTouchManager.setTouchSlop(configuration.getScaledTouchSlop());

        mChromeLikeLayout = new ChromeLikeLayout(getContext());
        mChromeLikeLayout.setRippleListener(new ChromeLikeLayout.IOnRippleListener() {
            @Override
            public void onRippleAnimFinished(int index) {
                mStatusManager.toRestore();
                if ( !mAnimationStarted ) launchResetAnim();
                mTouchManager.endDrag();
                if ( mOnItemSelectedListener != null )
                    mOnItemSelectedListener.onItemSelected(index);
            }
        });
        addOnExpandViewListener(mChromeLikeLayout);
        addView(mChromeLikeLayout);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if ( mAnimationStarted ) return false;
        if ( canChildDragDown(mTouchManager.event2Point(event)) ) return false;
        int action = event.getAction();
        switch ( action & MotionEvent.ACTION_MASK  ) {
            case MotionEvent.ACTION_DOWN:
                mTouchManager.setActivePointerId(event, 0);
                final float initialDownY = mTouchManager.getCurrentMotionEventY(event);
                if (initialDownY == -1) {
                    return false;
                }
                if ( mTouchManager.mBeginDragging ){
                    return true;
                }
                mTouchManager.mTouchDownActor = initialDownY;
                mTouchManager.mBeginDragging = false;
                mChromeLikeLayout.onActionDown();
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mTouchManager.resetActivePointerId();
                break;
            case MotionEvent.ACTION_MOVE:
                if ( mTouchManager.mActivePointerId == TouchManager.INVALID_POINTER) {
                    //Log.e(TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }
                final float y = mTouchManager.getCurrentMotionEventY(event);
                if (y == -1) {
                    return false;
                }
                // if diff > mTouchSlop
                // let's drag!
                if ( !mTouchManager.mBeginDragging && y - mTouchManager.mTouchDownActor > mTouchManager.mTouchSlop ) {
                    mTouchManager.mBeginDragging = true;
                    mStatusManager.toChanged();
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mTouchManager.onSecondaryPointerUp(event);
                break;
        }
        return mTouchManager.mBeginDragging;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = MotionEventCompat.getActionMasked(event);
        int pointerIndex = MotionEventCompat.findPointerIndex(event, mTouchManager.mActivePointerId);
        if (pointerIndex < 0) {
            //Log.e(TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
            return false;
        }
        final float y = MotionEventCompat.getY(event, pointerIndex);
        mTouchManager.setTopOffset(y);

        boolean isExpanded = mTouchManager.mTopOffset >= sThreshold && mTouchManager.mBeginDragging;
        //first point

        switch ( action ) {
            case MotionEvent.ACTION_DOWN:
                mTouchManager.setActivePointerId(event, 0);
                break;
            case MotionEvent.ACTION_CANCEL:
                mChromeLikeLayout.onActionUpOrCancel(isExpanded);
                break;
            case MotionEvent.ACTION_UP:
                executeAction(isExpanded);
                mChromeLikeLayout.onActionUpOrCancel(isExpanded);
                mTouchManager.resetActivePointerId();
                break;
            case MotionEvent.ACTION_MOVE:
                mChromeLikeLayout.onActionMove(event, pointerIndex, isExpanded);
                ensureTarget();
                View child = mTarget;
                int currentTop = child.getTop();
                if ( mTouchManager.mBeginDragging ) {
                    if ( !isExpanded )
                        notifyOnExpandListeners( currentTop * 1.0f / sThreshold, true);
                    childOffsetTopAndBottom(currentTop, mTouchManager.mTopOffset);
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                pointerIndex = MotionEventCompat.getActionIndex(event);
                if (pointerIndex < 0) {
                    //Log.e(TAG, "Got ACTION_POINTER_DOWN event but have an invalid action index.");
                    return false;
                }
                mTouchManager.setActivePointerId(event, pointerIndex);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mTouchManager.onSecondaryPointerUp(event);
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

        mChromeLikeLayout.offsetTopAndBottom( target );
        requestLayout();
    }

    // only execute on ACTION_UP
    private void executeAction(boolean isExpanded) {
        if ( isExpanded ){
            mStatusManager.toBusy();
        } else {
            if ( mStatusManager.isBusying() ) return;
            if ( mAnimationStarted ) return;
            launchResetAnim();
            mTouchManager.endDrag();
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

        boolean touchAlwaysTrue = child instanceof ScrollView
                || child instanceof AbsListView
                || child instanceof ScrollingView
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

    private boolean canChildDragDown(PointF pointF){
        ensureTarget();
        if ( mTarget instanceof TouchAlwaysTrueLayout )
            return ((TouchAlwaysTrueLayout) mTarget).canChildDragDown(pointF);
        else return ViewCompat.canScrollVertically(mTarget,-1);

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

    public static class StatusManager {

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

    public class TouchManager {
        public static final int INVALID_POINTER = -1;
        public boolean mBeginDragging;
        public int mTopOffset;
        public int mTouchSlop;
        public float mTouchDownActor;
        public int mActivePointerId = INVALID_POINTER;
        private PointF mTmpPoint = new PointF();

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

        public void setActivePointerId(MotionEvent event, int defaultId){
            int id = MotionEventCompat.getPointerId(event, defaultId);
            mActivePointerId = id;
            final float initialDownY = mTouchManager.getCurrentMotionEventY(event);
            if ( initialDownY == -1 ) return;
            if ( mTouchManager.mBeginDragging ){
                mTouchManager.mTouchDownActor = mTouchManager.calculateTouchDown(initialDownY);
            }
        }

        public void resetActivePointerId(){
            mActivePointerId = INVALID_POINTER;
        }

        private float getCurrentMotionEventY(MotionEvent ev) {
            final int index = MotionEventCompat.findPointerIndex(ev, mTouchManager.mActivePointerId);
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

        private int calculateTopOffset(float y){
            float original = y - mTouchDownActor;
            float basic = original * 0.6f;
            if ( basic > sThreshold ){
                basic = sThreshold + (basic - sThreshold) * 0.3f;
            }
            return (int) basic;
        }

        private float calculateTouchDown(float y){
            float diff;
            if ( mTopOffset < 0 ){
                diff = 0;
            } else if( mTopOffset > sThreshold ){
                diff = (mTopOffset - sThreshold ) / 0.3f / 0.6f + sThreshold / 0.6f;
            } else {
                diff = mTopOffset / 0.6f;
            }
            return y - diff;
        }

        public void setTopOffset(float y) {
            mTopOffset = calculateTopOffset(y);
        }

        public PointF event2Point(MotionEvent event){
            mTmpPoint.set(event.getX(),event.getY());
            return mTmpPoint;
        }
    }
}
