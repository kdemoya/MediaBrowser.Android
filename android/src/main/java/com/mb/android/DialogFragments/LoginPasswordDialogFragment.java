package com.mb.android.DialogFragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.mb.android.R;
import com.mb.android.ui.main.ILoginDialogListener;
import mediabrowser.model.dto.UserDto;


public class LoginPasswordDialogFragment extends DialogFragment {

    View dialogContent;
    private UserDto mUser;

    public void setUser(UserDto user) {
        mUser = user;
    }

    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();

        dialogContent = inflater.inflate(R.layout.fragment_login_dialog, null);

        TextView caption = (TextView) dialogContent.findViewById(R.id.tvLoginDialogDescription);
        caption.setText(caption.getText() + " " + mUser.getName());

        final EditText etPassword = (EditText) dialogContent.findViewById(R.id.etPassword);
        etPassword.requestFocus();

        builder.setView(dialogContent)
                .setPositiveButton(getResources().getString(R.string.ok_button), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        ILoginDialogListener activity = (ILoginDialogListener) getActivity();
                        activity.onLoginDialogPositiveButtonClick(mUser, etPassword.getText().toString());
                    }
                })
                .setNegativeButton(getResources().getString(R.string.cancel_button), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        ILoginDialogListener activity = (ILoginDialogListener) getActivity();
                        activity.onLoginDialogNegativeButtonClick();
                    }
                });

        Dialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        return dialog;
    }
}
