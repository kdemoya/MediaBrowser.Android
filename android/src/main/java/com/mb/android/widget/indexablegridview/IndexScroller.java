package com.mb.android.widget.indexablegridview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Adapter;
import android.widget.GridView;
import android.widget.SectionIndexer;

/**
 * Created by Mark on 12/12/13.
 */
public class IndexScroller {

    private static final int STATE_HIDDEN = 0;
    private int state_ = STATE_HIDDEN;
    private static final int STATE_SHOWING = 1;
    private static final int STATE_SHOWN = 2;
    private static final int STATE_HIDING = 3;
    private float indexbarWidth_;
    private float indexbarMargin_;
    private float previewPadding_;
    private float density_;
    private float scaledDensity_;
    private float alphaRate_;
    private Handler handler_ = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (state_) {
                case STATE_SHOWING:
                    // Fade in effect
                    alphaRate_ += (1 - alphaRate_) * 0.2;
                    if (alphaRate_ > 0.9) {
                        alphaRate_ = 1;
                        setState(STATE_SHOWN);
                    }

                    gridView_.invalidate();
                    fade(10);
                    break;
                case STATE_SHOWN:
                    // If no action, hide automatically
                    setState(STATE_HIDING);
                    break;
                case STATE_HIDING:
                    // Fade out effect
                    alphaRate_ -= alphaRate_ * 0.2;
                    if (alphaRate_ < 0.1) {
                        alphaRate_ = 0;
                        setState(STATE_HIDDEN);
                    }

                    gridView_.invalidate();
                    fade(10);
                    break;
            }
        }
    };
    private int gridViewHeight_;
    private int gridViewWidth_;
    private int currentSection_ = -1;
    private boolean isIndexing_;
    private GridView gridView_;
    private SectionIndexer indexer_;
    private String[] sections_ = null;
    private RectF indexbarRect_;

    public IndexScroller(Context context, GridView gv) {
        density_ = context.getResources().getDisplayMetrics().density;
        scaledDensity_ = context.getResources().getDisplayMetrics().scaledDensity;
        gridView_ = gv;
        setAdapter(gridView_.getAdapter());

        indexbarWidth_ = 20 * density_;
        indexbarMargin_ = 10 * density_;
        previewPadding_ = 5 * density_;
    }

    public void draw(Canvas canvas) {
        if (state_ == STATE_HIDDEN)
            return;

        Paint indexbarPaint = new Paint();
        indexbarPaint.setColor(Color.BLACK);
        indexbarPaint.setAlpha((int) (64 * alphaRate_));
        indexbarPaint.setAntiAlias(true);

        canvas.drawRoundRect(indexbarRect_, 5 * density_, 5 * density_, indexbarPaint);

        if (sections_ != null && sections_.length > 0) {
            // Preview is shown when currentSection_ is set
            if (currentSection_ >= 0) {
                Paint previewPaint = new Paint();
                previewPaint.setColor(Color.BLACK);
                previewPaint.setAlpha(96);
                previewPaint.setAntiAlias(true);
                previewPaint.setShadowLayer(3, 0, 0, Color.argb(64, 0, 0, 0));

                Paint previewTextPaint = new Paint();
                previewTextPaint.setColor(Color.WHITE);
                previewTextPaint.setTextSize(50 * scaledDensity_);

                float previewTextWidth = previewTextPaint.measureText(sections_[currentSection_]);
                float previewSize = 2 * previewPadding_ + previewTextPaint.descent() - previewTextPaint.ascent();
                RectF previewRect = new RectF((gridViewWidth_ - previewSize) / 2
                        , (gridViewHeight_ - previewSize) / 2
                        , (gridViewWidth_ - previewSize) / 2 + previewSize
                        , (gridViewHeight_ - previewSize) / 2 + previewSize);

                canvas.drawRoundRect(previewRect, 5 * density_, 5 * density_, previewPaint);
                canvas.drawText(sections_[currentSection_], previewRect.left + (previewSize - previewTextWidth) / 2 - 1
                        , previewRect.top + previewPadding_ - previewTextPaint.ascent() + 1, previewTextPaint);
            }

            Paint indexPaint = new Paint();
            indexPaint.setColor(Color.WHITE);
            indexPaint.setAlpha((int) (255 * alphaRate_));
            indexPaint.setAntiAlias(true);
            indexPaint.setTextSize(12 * scaledDensity_);

            float sectionHeight = (indexbarRect_.height() - 2 * indexbarMargin_) / sections_.length;
            float paddingTop = (sectionHeight - (indexPaint.descent() - indexPaint.ascent())) / 2;
            for (int i = 0; i < sections_.length; i++) {
                float paddingLeft = (indexbarWidth_ - indexPaint.measureText(sections_[i])) / 2;
                canvas.drawText(sections_[i], indexbarRect_.left + paddingLeft
                        , indexbarRect_.top + indexbarMargin_ + sectionHeight * i + paddingTop - indexPaint.ascent(), indexPaint);
            }
        }
    }

    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // If down event occurs inside index bar region, start indexing
                if (state_ != STATE_HIDDEN && contains(ev.getX(), ev.getY())) {
                    setState(STATE_SHOWN);

                    // It demonstrates that the motion event started from the index bar
                    isIndexing_ = true;
                    // Determine which section the point is in, and move the Grid to that section
                    currentSection_ = getSectionByPoint(ev.getY());
                    gridView_.setSelection(indexer_.getPositionForSection(currentSection_));
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isIndexing_) {
                    // If this event moves inside index bar
                    if (contains(ev.getX(), ev.getY())) {
                        // Determine which section the point is in, and move the grid to that section
                        currentSection_ = getSectionByPoint(ev.getY());
                        gridView_.setSelection(indexer_.getPositionForSection(currentSection_));
                    }
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (isIndexing_) {
                    isIndexing_ = false;
                    currentSection_ = -1;
                }
                if (state_ == STATE_SHOWN)
                    setState(STATE_HIDING);
                break;
        }
        return false;
    }

    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.i("IndexScroller", "onSizeChanged");
        gridViewWidth_ = w;
        gridViewHeight_ = h;
        indexbarRect_ = new RectF(w - indexbarMargin_ - indexbarWidth_
                , indexbarMargin_
                , w - indexbarMargin_
                , h - indexbarMargin_);
    }

    public void show() {
        if (state_ == STATE_HIDDEN)
            setState(STATE_SHOWING);
        else if (state_ == STATE_HIDING)
            setState(STATE_HIDING);
    }

    public void hide() {
        if (state_ == STATE_SHOWN)
            setState(STATE_HIDING);
    }

    public void setAdapter(Adapter adapter) {
        if (adapter instanceof SectionIndexer) {
            indexer_ = (SectionIndexer) adapter;
            sections_ = (String[]) indexer_.getSections();
        }
    }

    private void setState(int state) {
        if (state_ < STATE_HIDDEN || state > STATE_HIDING)
            return;

        state_ = state;
        switch (state_) {
            case STATE_HIDDEN:
                // Cancel any fade effect
                handler_.removeMessages(0);
                break;
            case STATE_SHOWING:
                // Start to fade in
                alphaRate_ = 0;
                fade(0);
                break;
            case STATE_SHOWN:
                // Cancel any fade effect
                handler_.removeMessages(0);
                break;
            case STATE_HIDING:
                // Start to fade out after three seconds
                alphaRate_ = 1;
                fade(3000);
                break;
        }
    }

    private boolean contains(float x, float y) {
        // Determine if the point is in index bar region, which includes the right margin of the bar
        return (x >= indexbarRect_.left && y >= indexbarRect_.top && y <= indexbarRect_.top + indexbarRect_.height());
    }

    private int getSectionByPoint(float y) {
        if (sections_ == null || sections_.length == 0)
            return 0;
        if (y < indexbarRect_.top + indexbarMargin_)
            return 0;
        if (y >= indexbarRect_.top + indexbarRect_.height() - indexbarMargin_)
            return sections_.length - 1;
        return (int) ((y - indexbarRect_.top - indexbarMargin_) / ((indexbarRect_.height() - 2 * indexbarMargin_) / sections_.length));
    }

    private void fade(long delay) {
        handler_.removeMessages(0);
        handler_.sendEmptyMessageAtTime(0, SystemClock.uptimeMillis() + delay);
    }
}
