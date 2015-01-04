package com.mb.android.DialogFragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mb.android.R;
import com.mb.android.interfaces.IServerDialogClickListener;
import com.mb.android.logging.FileLogger;
import com.mb.android.utils.Utils;

/**
 * Created by Mark on 12/12/13.
 *
 * DialogFragment that allows a user to enter a server address to connect to. This fragment is used
 * for first time entry only. Editing a server connection is done using the ServerEditDialogFragment.
 */
public class ServerConnectionDialogFragment extends DialogFragment {

    private IServerDialogClickListener mCallback;

    /**
     * Class Constructor
     */
    public ServerConnectionDialogFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();

        @SuppressLint("InflateParams") View mDialogContent = inflater.inflate(R.layout.fragment_server_configuration, null);
        final EditText etHostName = (EditText) mDialogContent.findViewById(R.id.etHostName);
        final EditText etPort = (EditText) mDialogContent.findViewById(R.id.etPortValue);

        final AlertDialog d = new AlertDialog.Builder(getActivity())
                .setView(mDialogContent)
                .setPositiveButton(getResources().getString(R.string.ok_button), null)
                .setNegativeButton(getResources().getString(R.string.cancel_button), null)
                .create();

        // Setting the onClick events in this manor allows for overriding the dialog close.
        // That means the values can be validated before closing.
        d.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button bPositive = d.getButton(AlertDialog.BUTTON_POSITIVE);
                bPositive.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (!Utils.isAddressValid(etHostName)) {

                            Toast.makeText(getActivity(), "Address is not valid", Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (!Utils.isPortValid(etPort)) {

                            Toast.makeText(getActivity(), "Port is not valid", Toast.LENGTH_LONG).show();
                            return;
                        }

                        try {
                            mCallback = (IServerDialogClickListener) getActivity();
                        } catch (ClassCastException e) {
                            FileLogger.getFileLogger().ErrorException("Error casting callback class ", e);
                        }

                        String address = etHostName.getText().toString();

                        if (address != null && !address.startsWith("http")) {
                            address = "http://" + address;
                        }

                        address = address + ":" + etPort.getText().toString();

                        if (mCallback != null) {
                            mCallback.onOkClick(address);
                        }
                        d.dismiss();
                    }
                });
                Button bNegative = d.getButton(AlertDialog.BUTTON_NEGATIVE);
                bNegative.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            mCallback = (IServerDialogClickListener) getActivity();
                            if (mCallback != null) {
                                mCallback.onCancelClick();
                            }
                        } catch (ClassCastException e) {
                            FileLogger.getFileLogger().ErrorException("Error casting callback class ", e);
                        }
                        d.dismiss();
                    }
                });
            }
        });

        return d;
    }
}
