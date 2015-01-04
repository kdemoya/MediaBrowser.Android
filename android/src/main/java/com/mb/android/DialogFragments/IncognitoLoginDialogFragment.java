package com.mb.android.DialogFragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.mb.android.R;
import com.mb.android.ui.main.ILoginDialogListener;

/**
 * Created by Mark on 12/12/13.
 */
public class IncognitoLoginDialogFragment extends DialogFragment {

    View dialogContent;

    /**
     * Class Constructor
     */
    public IncognitoLoginDialogFragment() {}

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();

        dialogContent = inflater.inflate(R.layout.fragment_incognito_login_dialog, null);

        final EditText etUsername = (EditText) dialogContent.findViewById(R.id.etUsername);
        final EditText etPassword = (EditText) dialogContent.findViewById(R.id.etPassword);

        builder.setView(dialogContent)
                .setPositiveButton(getResources().getString(R.string.ok_button), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        ILoginDialogListener activity = (ILoginDialogListener) getActivity();
                        String password = "";

                        if (etPassword.getText().toString() != null && !etPassword.getText().toString().isEmpty())
                            password = etPassword.getText().toString();

                        activity.onLoginDialogPositiveButtonClick(etUsername.getText().toString(), password);
                    }
                })
                .setNegativeButton(getResources().getString(R.string.cancel_button), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        return builder.create();

    }
}
