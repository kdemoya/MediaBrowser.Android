package com.mb.android.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;

import com.mb.android.R;

/**
 * Created by Mark on 2014-10-17.
 */
public class WelcomeActivity extends FragmentActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        Button next = (Button) findViewById(R.id.btnNext);
        next.setOnClickListener(onNextClick);
        next.requestFocus();
    }

    View.OnClickListener onNextClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(WelcomeActivity.this, MbConnectActivity.class);
            startActivity(intent);
            WelcomeActivity.this.finish();
        }
    };
}
