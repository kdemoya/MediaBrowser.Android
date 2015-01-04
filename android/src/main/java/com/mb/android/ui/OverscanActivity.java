package com.mb.android.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.mb.android.R;

/**
 * Created by Mark on 2014-07-01.
 *
 * Activity that allows the user to adjust the overscan settings.
 */
public class OverscanActivity extends Activity {

    private SharedPreferences _prefs;
    private RelativeLayout _overscanLayout;
    private ViewGroup.MarginLayoutParams _overscanMargins;
    private int _left;
    private int _top;
    private int _right;
    private int _bottom;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_overscan_adjustment);
        _overscanLayout = (RelativeLayout) findViewById(R.id.rlOverscanPadding);

        _prefs = PreferenceManager.getDefaultSharedPreferences(this);
        _left = _prefs.getInt("overscan_left", 0);
        _top = _prefs.getInt("overscan_top", 0);
        _right = _prefs.getInt("overscan_right", 0);
        _bottom = _prefs.getInt("overscan_bottom", 0);

        _overscanMargins = (ViewGroup.MarginLayoutParams) _overscanLayout.getLayoutParams();
        _overscanMargins.setMargins(_left, _top, _right, _bottom);
        _overscanLayout.requestLayout();
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {

        if (keyEvent.getAction() != KeyEvent.ACTION_DOWN) {
            return super.onKeyDown(i, keyEvent);
        }

        switch (i) {

            case KeyEvent.KEYCODE_DPAD_LEFT:
                // Shrink left/right margins
                _left++;
                _right++;
                break;

            case KeyEvent.KEYCODE_DPAD_UP:
                // Grow top/bottom margins
                _top--;
                _bottom--;
                break;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                // Grow left/right margins
                _left--;
                _right--;
                break;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                // Shrink top/bottom margins
                _top++;
                _bottom++;
                break;

            default:
                return super.onKeyDown(i, keyEvent);
        }

        _overscanMargins.setMargins(_left, _top, _right, _bottom);
        _overscanLayout.requestLayout();

        _prefs.edit()
                .putInt("overscan_left", _left)
                .putInt("overscan_top", _top)
                .putInt("overscan_right", _right)
                .putInt("overscan_bottom", _bottom)
                .apply();

        return true;
    }
}
