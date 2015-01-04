package com.mb.android.ui.main;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.mb.android.SavedSessionInfo;
import com.mb.android.logging.LogLevel;
import com.mb.android.ui.main.ConnectionActivity;
import com.mb.android.utils.FileUtils;
import com.mb.android.R;
import com.mb.android.logging.FileLogger;
import com.mb.android.widget.customswitchpreference.CustomSwitchPreference;
import mediabrowser.model.extensions.StringHelper;

/**
 * Created by Mark on 12/12/13.
 */
public class MainSettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * Class Constructor
     */
    public MainSettingsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);

        try {
            FileUtils fileUtils = new FileUtils();

            SavedSessionInfo savedSessionInfo = fileUtils.LoadSavedSessionInfo(getActivity());

            if (savedSessionInfo != null) {
                if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(savedSessionInfo.serverName))
                    this.findPreference("pref_server_name").setTitle(savedSessionInfo.serverName);
                else
                    this.findPreference("pref_server_name").setTitle(savedSessionInfo.useInternalAddress
                            ? savedSessionInfo.internalAddress
                            : savedSessionInfo.externalAddress);
            }

        } catch (Exception e) {
            FileLogger.getFileLogger().ErrorException("Exception handled", e);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        FileLogger.getFileLogger().Info("onSharedPreferenceChanged");
        FileLogger.getFileLogger().Info("key = " + key);
        if (key.equals("pref_enable_external_player")) {
            final CustomSwitchPreference csp = (CustomSwitchPreference) getPreferenceScreen().findPreference(key);

            if (csp.isChecked()) {

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getResources().getString(R.string.warning_string))
                        .setMessage(getResources().getString(R.string.external_player_warning))
                        .setPositiveButton(getResources().getString(R.string.proceed_button), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        })
                        .setNegativeButton(getResources().getString(R.string.cancel_button), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                csp.setChecked(false);

                            }
                        })
                        .show();

            } else {

            }
        } else if (key.equals("pref_application_profile")) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getResources().getString(R.string.warning_string))
                    .setMessage(getResources().getString(R.string.restart_warning))
                    .setPositiveButton(getResources().getString(R.string.restart_button), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(getActivity(), ConnectionActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

                            startActivity(intent);
                            getActivity().finish();
                        }
                    })
                    .setNegativeButton(getResources().getString(R.string.cancel_button), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    })
                    .show();
        } else if (key.equals("pref_debug_logging_enabled")) {
            final CustomSwitchPreference csp = (CustomSwitchPreference) getPreferenceScreen().findPreference(key);
            if (csp == null) return;
            FileLogger.getFileLogger().setLoggingLevel(csp.isChecked() ? LogLevel.Debug : LogLevel.Info);
        }

    }
}
