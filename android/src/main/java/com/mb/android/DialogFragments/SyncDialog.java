package com.mb.android.DialogFragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;

import com.mb.android.R;

/**
 * Created by Mark on 2014-12-19.
 */
public class SyncDialog extends DialogFragment {

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();

        @SuppressLint("InflateParams") View dialogContent = inflater.inflate(R.layout.dialog_sync, null);

        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle("Media Sync")
                .setView(dialogContent)
                .setPositiveButton(getResources().getString(R.string.ok_button), null)
                .setNegativeButton(getResources().getString(R.string.cancel_button), null)
                .create();

        return dialog;
    }
}
