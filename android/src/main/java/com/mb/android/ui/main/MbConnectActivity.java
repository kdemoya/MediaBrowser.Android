package com.mb.android.ui.main;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.logging.FileLogger;
import com.mb.android.logging.LogLevel;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.AndroidConnectionManager;
import mediabrowser.apiinteraction.connectionmanager.ConnectionManager;
import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.model.connect.PinCreationResult;
import mediabrowser.model.connect.PinExchangeResult;
import mediabrowser.model.connect.PinStatusResult;
import mediabrowser.model.extensions.StringHelper;
import mediabrowser.model.net.HttpException;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Mark on 2014-10-17.
 */
public class MbConnectActivity extends FragmentActivity {

    private EditText mConnectUserName;
    private EditText mConnectPassword;
    private TextView mPin;
    private AlertDialog dialog;
    private AndroidConnectionManager connectionManager;
    private String deviceId;
    private PinCreationResult pcr;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Always show debug logging during initial connection
        FileLogger.getFileLogger().setLoggingLevel(LogLevel.Debug);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPrefs.getString("pref_application_profile", "Mobile").equalsIgnoreCase("Mobile")) {
            setContentView(R.layout.activity_welcome2);
            findViewById(R.id.btnSignIn).setOnClickListener(onSignInClick);
            mConnectUserName = (EditText) findViewById(R.id.etUsername);
            mConnectPassword = (EditText) findViewById(R.id.etPassword);
            TextView linkText = (TextView) findViewById(R.id.tvParagraph1);
            linkText.requestFocus();
            linkText.setText(Html.fromHtml(getResources().getString(R.string.mb_connect_welcome_text_with_url)));
            linkText.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            setContentView(R.layout.activity_welcome2_pin);
            mPin = (TextView) findViewById(R.id.tvPin);
            connectionManager = (AndroidConnectionManager) MB3Application.getInstance().getConnectionManager();
            deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            connectionManager.CreatePin(deviceId, pinCreationResultResponse);
        }
        Button skip = (Button) findViewById(R.id.btnSkip);
        skip.setOnClickListener(onSkipClick);
        skip.requestFocus();
    }

    @Override
    public void onDestroy() {
        dismissActivityDialog();
        stopPinTimer();
        super.onDestroy();
    }

    View.OnClickListener onSignInClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ConnectionManager connectionManager = (ConnectionManager) MB3Application.getInstance().getConnectionManager();
            try {
                showActivityDialog();
                connectionManager.LoginToConnect(
                        mConnectUserName.getText().toString(),
                        mConnectPassword.getText().toString(),
                        connectLoginResponse
                );
            } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
    };

    View.OnClickListener onSkipClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            proceedToConnectionManagement();
        }
    };

    EmptyResponse connectLoginResponse = new EmptyResponse() {
        @Override
        public void onResponse() {
            dismissActivityDialog();
            proceedToConnectionManagement();
        }
        @Override
        public void onError(Exception ex) {
            dismissActivityDialog();
            try {
                HttpException exception = (HttpException) ex;
                if (exception.getStatusCode() != null && exception.getStatusCode() == 401) {
                    Toast.makeText(MbConnectActivity.this, "Incorrect username or password", Toast.LENGTH_LONG).show();
                    return;
                }
            } catch (ClassCastException cce) {
                FileLogger.getFileLogger().Error("failed to read HTTP status code for MB Connect failure");
            }
            Toast.makeText(MbConnectActivity.this, "Error logging into connect. Please try again later", Toast.LENGTH_LONG).show();
        }
    };

    private Response<PinCreationResult> pinCreationResultResponse = new Response<PinCreationResult>() {

        @Override
        public void onResponse(PinCreationResult result) {
            pcr = result;
            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(result.getPin())) {
                displayPinOnScreen(result.getPin());
                startPinTimer();
            }
        }
        @Override
        public void onError(Exception e) {

        }
    };

    private void displayPinOnScreen(String pin) {
        mPin.setText(pin);
    }


    private void startPinTimer() {
        pinTimerHandler.postDelayed(pinTimerRunnable, 10);
    }


    private void stopPinTimer() {
        pinTimerHandler.removeCallbacks(pinTimerRunnable);
    }


    Handler pinTimerHandler = new Handler();
    Runnable pinTimerRunnable = new Runnable() {
        @Override
        public void run() {
            connectionManager.GetPinStatus(pcr, pinStatusResponse);
            pinTimerHandler.postDelayed(this, 100);
        }
    };


    Response<PinStatusResult> pinStatusResponse = new Response<PinStatusResult>() {
        @Override
        public void onResponse(PinStatusResult result) {
            if (result.getIsExpired()) {
                stopPinTimer();
                connectionManager.CreatePin(deviceId, pinCreationResultResponse);
            } else if (result.getIsConfirmed()) {
                stopPinTimer();
                connectionManager.ExchangePin(pcr, pinExchangeResponse);
            }
        }
    };


    Response<PinExchangeResult> pinExchangeResponse = new Response<PinExchangeResult>() {

        @Override
        public void onResponse(PinExchangeResult result) {
            proceedToConnectionManagement();
        }
    };


    private void proceedToConnectionManagement() {
        Intent intent = new Intent(this, ConnectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("show_welcome", false);
        startActivity(intent);
    }


    private void showActivityDialog() {
        dialog = new AlertDialog.Builder(this)
                .setTitle("Media Browser")
                .setMessage("Logging In")
                .setCancelable(false)
                .create();
        dialog.show();
    }


    private void dismissActivityDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
