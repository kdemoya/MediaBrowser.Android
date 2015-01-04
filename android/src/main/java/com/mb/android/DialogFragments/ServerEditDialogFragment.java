package com.mb.android.DialogFragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mb.android.R;
import com.mb.android.interfaces.IServerDialogClickListener;
import com.mb.android.utils.Utils;
import mediabrowser.model.apiclient.ServerInfo;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by Mark on 12/12/13.
 *
 * DialogFragment that is displayed when the user long-presses a server connection. It is used when
 * the user wants to change the internal & external address/port values
 */
public class ServerEditDialogFragment extends DialogFragment {

    private ServerInfo mServer;
    private IServerDialogClickListener mCallback;
    private EditText etHostName;
    private EditText etExtAddress;
    private EditText etPort;
    private EditText etExtPort;

    /**
     * Class Constructor
     */
    public ServerEditDialogFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            mCallback = (IServerDialogClickListener) getTargetFragment();
        } catch (ClassCastException e) {
            Log.d("ServerEditDialogFragment", "Error casting IServerDialogClickListener");
        }
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        mServer = (ServerInfo) getArguments().getSerializable("ServerInfo");

        LayoutInflater inflater = getActivity().getLayoutInflater();

        View dialogContent = inflater.inflate(R.layout.fragment_server_edit, null);

        etHostName = (EditText) dialogContent.findViewById(R.id.etHostName);
        etExtAddress = (EditText) dialogContent.findViewById(R.id.etExtHostName);
        etPort = (EditText) dialogContent.findViewById(R.id.etInternalPort);
        etExtPort = (EditText) dialogContent.findViewById(R.id.etExternalPort);

        populateInitialValues();

        final AlertDialog d = new AlertDialog.Builder(getActivity())
                .setView(dialogContent)
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

                        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(etHostName.getText().toString())) {
                            if (!Utils.isAddressValid(etHostName)) {

                                Toast.makeText(getActivity(), "Internal address is not valid", Toast.LENGTH_LONG).show();
                                return;
                            }

                            if (!Utils.isPortValid(etPort)) {

                                Toast.makeText(getActivity(), "Internal port is not valid", Toast.LENGTH_LONG).show();
                                return;
                            }

                            String address = etHostName.getText().toString();
                            if (address != null && !address.startsWith("http")) {
                                address = "http://" + address;
                            }
                            mServer.setLocalAddress(address + ":" + etPort.getText().toString());
                        }

                        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(etExtAddress.getText().toString())) {
                            if (!Utils.isAddressValid(etExtAddress)) {

                                Toast.makeText(getActivity(), "External address is not valid", Toast.LENGTH_LONG).show();
                                return;
                            }

                            if (!Utils.isPortValid(etExtPort)) {

                                Toast.makeText(getActivity(), "External port is not valid", Toast.LENGTH_LONG).show();
                                return;
                            }

                            String address = etExtAddress.getText().toString();
                            if (address != null && !address.startsWith("http")) {
                                address = "http://" + address;
                            }
                            mServer.setRemoteAddress(address + ":" + etExtPort.getText().toString());
                        }

                        mCallback.onEditOkClick(mServer);
                        d.dismiss();
                    }
                });

                Button bNegative = d.getButton(AlertDialog.BUTTON_NEGATIVE);
                bNegative.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCallback.onCancelClick();
                        d.dismiss();
                    }
                });
            }
        });

        return d;

    }

    private void populateInitialValues() {

        URI internalUri = null;
        URI externalUri = null;

        if (mServer.getLocalAddress() != null) {
            try {
                internalUri = new URI(mServer.getLocalAddress());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        if (mServer.getRemoteAddress() != null) {
            try {
                externalUri = new URI(mServer.getRemoteAddress());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        if (internalUri != null) {
            String address;
            address = !tangible.DotNetToJavaStringHelper.isNullOrEmpty(internalUri.getScheme()) ? internalUri.getScheme() + "://" : "http://";
            address += internalUri.getHost();
            etHostName.setText(address);
            etPort.setText(String.valueOf(internalUri.getPort()));
        }

        if (externalUri != null) {
            String address;
            address = !tangible.DotNetToJavaStringHelper.isNullOrEmpty(externalUri.getScheme()) ? externalUri.getScheme() + "://" : "http://";
            address += externalUri.getHost();
            etExtAddress.setText(address);
            etExtPort.setText(String.valueOf(externalUri.getPort()));
        }
    }

}
