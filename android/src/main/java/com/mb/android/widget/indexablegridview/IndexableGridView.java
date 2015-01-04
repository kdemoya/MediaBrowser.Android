package com.mb.android.widget.indexablegridview;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.GridView;
import android.widget.ListAdapter;

/**
 * Created by Mark on 12/12/13.
 */
public class IndexableGridView extends GridView {

    private boolean isFastScrollEnabled_ = false;
    private IndexScroller scroller_ = null;
    private GestureDetector gestureDetector_ = null;

    public IndexableGridView(Context context) {
        super(context);
        scroller_ = new IndexScroller(getContext(), this);
    }

    public IndexableGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        scroller_ = new IndexScroller(getContext(), this);
    }

    public IndexableGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        scroller_ = new IndexScroller(getContext(), this);
    }

    @Override
    public boolean isFastScrollEnabled() {
        return isFastScrollEnabled_;
    }

    @Override
    public void setFastScrollEnabled(boolean enabled) {
        isFastScrollEnabled_ = enabled;
        Log.i("IndexableGridView", "setFastScrollEnabled");
        if (isFastScrollEnabled_) {
            if (scroller_ == null)
                scroller_ = new IndexScroller(getContext(), this);
        } else {
            if (scroller_ != null) {
                scroller_.hide();
                scroller_ = null;
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        if (scroller_ != null)
            scroller_.draw(canvas);

    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Intercept GridView's touch event
        if (scroller_ != null && scroller_.onTouchEvent(ev))
            return true;

        if (gestureDetector_ == null) {
            gestureDetector_ = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2,
                                       float velocityX, float velocityY) {
                    // If fling happens, index bar shows
                    scroller_.show();
                    return super.onFling(e1, e2, velocityX, velocityY);
                }
            });
        }
        gestureDetector_.onTouchEvent(ev);

        return super.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);
        if (scroller_ != null)
            scroller_.setAdapter(adapter);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.i("IndexableGridView", "onSizeChanged");
        if (scroller_ != null)
            scroller_.onSizeChanged(w, h, oldw, oldh);
    }
}
