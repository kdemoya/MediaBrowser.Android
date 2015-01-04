package com.mb.android.ui.main;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mb.android.DialogFragments.IncognitoLoginDialogFragment;
import com.mb.android.DialogFragments.LoginPasswordDialogFragment;
import com.mb.android.DialogFragments.ServerConnectionDialogFragment;
import com.mb.android.MB3Application;
import com.mb.android.R;
import com.mb.android.ui.mobile.homescreen.HomescreenActivity;
import com.mb.android.interfaces.IServerDialogClickListener;
import com.mb.android.logging.FileLogger;
import com.mb.android.logging.LogLevel;
import com.mb.android.ui.tv.homescreen.HomeScreenActivity;
import mediabrowser.apiinteraction.ConnectionResult;
import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.AndroidApiClient;
import mediabrowser.apiinteraction.android.AndroidCredentialProvider;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.model.apiclient.ServerCredentials;
import mediabrowser.model.apiclient.ServerInfo;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.net.HttpException;
import mediabrowser.model.users.AuthenticationResult;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;


public class ConnectionActivity extends FragmentActivity implements IServerDialogClickListener, ILoginDialogListener {

    private GridView mContentGrid;
    private LinearLayout mHeader;
    private TextView mHeaderText;
    private Button mChangeServerButton;
    private Button mLoginIncognitoButton;
    private UserDto[] mUsers;
    private AlertDialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showWelcomeScreen = preferences.getBoolean("is_first_run", true);

        if (showWelcomeScreen && isKnownTVDevice()) {
            preferences.edit().putString("pref_application_profile", "Television").apply();
        }

        boolean showServerSelection = false;
        boolean showUserSelection = false;

        Intent mainIntent = getIntent();
        if (mainIntent != null) {
            showWelcomeScreen = mainIntent.getBooleanExtra("show_welcome", showWelcomeScreen);
            showServerSelection = mainIntent.getBooleanExtra("show_servers", false);
            showUserSelection = mainIntent.getBooleanExtra("show_users", false);
        }

        if (showWelcomeScreen) {
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            this.finish();
        } else {
            setContentView(R.layout.activity_connection);
            setOverscanValues();
            mContentGrid = (GridView) findViewById(R.id.gvContent);
            mHeader = (LinearLayout) findViewById(R.id.llContent);
            mHeaderText = (TextView) findViewById(R.id.tvHeaderText);
            mLoginIncognitoButton = (Button) findViewById(R.id.ivIncognito);
            mLoginIncognitoButton.setOnClickListener(onLoginIncognitoClick);
            mChangeServerButton = (Button) findViewById(R.id.ivChangeServer);
            mChangeServerButton.setOnClickListener(onChangeServerClick);
            Button connectSignInButton = (Button) findViewById(R.id.btnConnect);
            connectSignInButton.setOnClickListener(onMbConnectClick);

            // Always show debug logging during initial connection
            FileLogger.getFileLogger().setLoggingLevel(LogLevel.Debug);
            if (showServerSelection) {
                onServerSelection();
            } else if (showUserSelection) {
                mChangeServerButton.setOnClickListener(onChangeServerClick);
                updateHeader(getResources().getString(R.string.select_mb_user), false);
                if (MB3Application.getInstance().API != null) {
                    MB3Application.getInstance().API.GetPublicUsersAsync(getPublicUsersResponse);
                }
            } else {
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        MB3Application.getInstance().getConnectionManager().Connect(connectionResponse);
                    }
                };
                thread.start();
                showActivityDialog("Connecting");
            }

        }
    }

    private Response<ConnectionResult> connectionResponse = new Response<ConnectionResult>() {
        @Override
        public void onResponse(ConnectionResult result) {

            switch (result.getState()) {
                case Unavailable:
                    ConnectionActivity.this.onUnavailable(result);
                    break;
                case ServerSelection:
                    ConnectionActivity.this.onServerSelection();
                    break;
                case ServerSignIn:
                    ConnectionActivity.this.onServerSignIn(result);
                    break;
                case SignedIn:
                    ConnectionActivity.this.onSignedIn(result);
                    break;
                case ConnectSignIn:
                    ConnectionActivity.this.onConnectSignIn(result);
                    break;
            }
        }
        @Override
        public void onError(Exception ex) {
            ConnectionActivity.this.onError(ex);
        }
    };

    private void onUnavailable(ConnectionResult result) {
        // No servers found. User must manually enter connection info.
        FileLogger.getFileLogger().Info("**** UNAVAILABLE ***");
        dismissActivityDialog();
        MB3Application.getInstance().setKnownServers(result.getServers());
        showServerSelection();
        Toast.makeText(this, "Server Unreachable", Toast.LENGTH_LONG).show();
    }

    private void onServerSelection() {
        // Multiple servers available
        // Display a selection screen
        FileLogger.getFileLogger().Info("**** SERVER SELECTION ****");
        MB3Application.getInstance().getConnectionManager().GetAvailableServers(getAvailableServersResponse);
    }

    private void onServerSignIn(ConnectionResult result) {
        // A server was found and the user needs to login.
        // Display a login screen and authenticate with the server using result.ApiClient
        FileLogger.getFileLogger().Info("**** SERVER SIGN IN ****");
        dismissActivityDialog();
        MB3Application.getInstance().setKnownServers(result.getServers());
        showUserSelection(result);
    }

    private void onSignedIn(ConnectionResult result) {
        // A server was found and the user has been signed in using previously saved credentials.
        // Ready to browse using result.ApiClient
        FileLogger.getFileLogger().Info("**** SIGNED IN ****");
        dismissActivityDialog();
        MB3Application.getInstance().API = (AndroidApiClient)result.getApiClient();
        MB3Application.getInstance().user = new UserDto();
        MB3Application.getInstance().user.setId(MB3Application.getInstance().API.getCurrentUserId());
        MB3Application.getInstance().setKnownServers(result.getServers());
        proceedToHomescreen();
    }

    private void onConnectSignIn(ConnectionResult result) {
        FileLogger.getFileLogger().Info("**** CONNECT SIGN IN ****");
        dismissActivityDialog();
        MB3Application.getInstance().setKnownServers(result.getServers());
        showServerSelection();
    }

    private void onError(Exception ex) {
        FileLogger.getFileLogger().Info("**** ON ERROR ***");
        FileLogger.getFileLogger().ErrorException("Connection error handled: ", ex);
        dismissActivityDialog();
        showServerSelection();
    }


    private Response<ArrayList<ServerInfo>> getAvailableServersResponse = new Response<ArrayList<ServerInfo>>() {
        @Override
        public void onResponse(ArrayList<ServerInfo> servers) {
            dismissActivityDialog();
            MB3Application.getInstance().setKnownServers(servers);
            showServerSelection();
        }
        @Override
        public void onError(Exception e) {
            dismissActivityDialog();
            showServerSelection();
        }
    };


    private void showServerSelection() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                FileLogger.getFileLogger().Debug("Updating header buttons for server selection");
                mChangeServerButton.setOnClickListener(onAddServerClick);
                updateHeader(getResources().getString(R.string.select_mb_server), true);
                if (MB3Application.getInstance().getKnownServers() == null) {
                    MB3Application.getInstance().setKnownServers(new ArrayList<ServerInfo>());
                }
                FileLogger.getFileLogger().Debug("Creating server list");
                FileLogger.getFileLogger().Debug(String.valueOf(MB3Application.getInstance().getKnownServers().size()) + " servers to display");

                mContentGrid.setAdapter(new ServerAdapter(MB3Application.getInstance().getKnownServers(), ConnectionActivity.this, null));
                mContentGrid.setOnItemClickListener(onServerClick);
            }
        });
    }

    private void showUserSelection(final ConnectionResult result) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                FileLogger.getFileLogger().Debug("Updating header buttons for user selection");
                mChangeServerButton.setOnClickListener(onChangeServerClick);
                updateHeader(getResources().getString(R.string.select_mb_user), false);
                if (result.getApiClient() != null) {
                    MB3Application.getInstance().API = (AndroidApiClient)result.getApiClient();
                    result.getApiClient().GetPublicUsersAsync(getPublicUsersResponse);
                }
            }
        });
    }

    private Response<UserDto[]> getPublicUsersResponse = new Response<UserDto[]>() {
        @Override
        public void onResponse(UserDto[] users) {
            FileLogger.getFileLogger().Debug("Get public users response received");
            if (users == null) {
                users = new UserDto[0];
            }
            mUsers = users;
            buildUserGrid();
        }
        @Override
        public void onError(Exception ex) {
            FileLogger.getFileLogger().Debug("Error handled getting public users: " + ex);
            mUsers = new UserDto[0];
            buildUserGrid();
        }
    };

    private void buildUserGrid() {
        FileLogger.getFileLogger().Debug("Building user selection grid");
        FileLogger.getFileLogger().Debug(String.valueOf(mUsers.length) + " public users to display");
        mContentGrid.setAdapter(new UserAdapter(mUsers));
        mContentGrid.setOnItemClickListener(onUserClick);
    }

    private void updateHeader(final String text, final boolean isServerSelectionVisible) {
        mHeader.setVisibility(View.VISIBLE);
        mHeaderText.setText(text);
        if (isServerSelectionVisible) {
            mLoginIncognitoButton.setVisibility(View.INVISIBLE);
        } else {
            mLoginIncognitoButton.setVisibility(View.VISIBLE);
        }
    }

    private AdapterView.OnItemClickListener onServerClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            FileLogger.getFileLogger().Debug("Server at position " + String.valueOf(i) + " clicked");
            ServerInfo server = (ServerInfo) adapterView.getItemAtPosition(i);
            if (server == null) {
                FileLogger.getFileLogger().Debug("Adapter returned null for Server at position " + String.valueOf(i));
                return;
            }
            FileLogger.getFileLogger().Debug("Passing server to ConnectionManager");
            MB3Application.getInstance().getConnectionManager().Connect(server, connectionResponse);
            showActivityDialog(!tangible.DotNetToJavaStringHelper.isNullOrEmpty(server.getName()) ? "Connecting to " + server.getName() : "Connecting");
        }
    };

    private AdapterView.OnItemClickListener onUserClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            FileLogger.getFileLogger().Debug("User at position " + String.valueOf(i) + " clicked");
            UserDto user = (UserDto) adapterView.getItemAtPosition(i);
            if (user == null) {
                FileLogger.getFileLogger().Debug("Adapter returned null for User at position " + String.valueOf(i));
                return;
            }
            if (user.getHasPassword()) {
                FileLogger.getFileLogger().Debug("User requires password: Showing login dialog");
                showLoginDialog(user);
            } else {
                FileLogger.getFileLogger().Debug("User does not require a password: Logging in.");
                onLoginDialogPositiveButtonClick(user, "");
            }
        }
    };

    private void showLoginDialog(UserDto user) {

        LoginPasswordDialogFragment dialog = new LoginPasswordDialogFragment();
        dialog.setUser(user);
        dialog.show(ConnectionActivity.this.getSupportFragmentManager(), "LoginDialog");
        FileLogger.getFileLogger().Debug("Login dialog visible");
    }

    //******************************************************************************************************************
    // Server Connection methods
    //******************************************************************************************************************

    private View.OnClickListener onAddServerClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            FileLogger.getFileLogger().Debug("Showing manual server entry dialog");
            DialogFragment dialog = new ServerConnectionDialogFragment();
            dialog.show(ConnectionActivity.this.getSupportFragmentManager(), "ServerConnectionDialog");
            FileLogger.getFileLogger().Debug("Dialog visible");
        }
    };

    private View.OnClickListener onChangeServerClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (MB3Application.getInstance().API != null) {
                MB3Application.getInstance().API.Logout(new EmptyResponse());
            }
            showServerSelection();
        }
    };

    @Override
    public void onOkClick(String address) {
        FileLogger.getFileLogger().Debug("Attempting to manually connect to " + address);
        MB3Application.getInstance().getConnectionManager().Connect(address, connectionResponse);
    }

    @Override
    public void onEditOkClick(ServerInfo serverInfo) {

    }

    @Override
    public void onCancelClick() {

    }

    private View.OnClickListener onMbConnectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            FileLogger.getFileLogger().Debug("Switching to MbConnect login activity");
            Intent intent = new Intent(ConnectionActivity.this, MbConnectActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    };

    //******************************************************************************************************************
    // User Login methods
    //******************************************************************************************************************

    private View.OnClickListener onLoginIncognitoClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            DialogFragment dialog = new IncognitoLoginDialogFragment();
            dialog.show(ConnectionActivity.this.getSupportFragmentManager(), "IncognitoLoginDialog");
        }
    };

    @Override
    public void onLoginDialogPositiveButtonClick(UserDto user, String password) {
        onLoginDialogPositiveButtonClick(user.getName(), password);
    }

    @Override
    public void onLoginDialogPositiveButtonClick(String username, String password) {
        try {
            FileLogger.getFileLogger().Debug("Attempting to log in user " + username);
            MB3Application.getInstance().API.AuthenticateUserAsync(username, password, authenticationResultResponse);
            showActivityDialog("Logging In");
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            FileLogger.getFileLogger().ErrorException("Error handled attempting login request: ", e);
            e.printStackTrace();
        }
    }

    @Override
    public void onLoginDialogNegativeButtonClick() {

    }

    private Response<AuthenticationResult> authenticationResultResponse = new Response<AuthenticationResult>() {
        @Override
        public void onResponse(AuthenticationResult result) {
            FileLogger.getFileLogger().Debug("AuthenticationResult received");
            if (result == null || result.getUser() == null) {
                FileLogger.getFileLogger().Debug("AuthenticationResult or AuthenticationResult.User is null");
                return;
            }
            dismissActivityDialog();
            MB3Application.getInstance().user = result.getUser();
            updateStoredCredentials(MB3Application.getInstance().API.getServerInfo(), result);
            proceedToHomescreen();
        }
        @Override
        public void onError(Exception ex) {
            dismissActivityDialog();
            FileLogger.getFileLogger().Debug("Error handled authenticating user");
            try {
                HttpException exception = (HttpException) ex;
                if (exception.getStatusCode() != null && exception.getStatusCode() == 401) {
                    Toast.makeText(ConnectionActivity.this, "Error logging in. Possibly incorrect password", Toast.LENGTH_LONG).show();
                    FileLogger.getFileLogger().Debug("Login failure: Incorrect password");
                    return;
                }
            } catch (ClassCastException cce) {
                // silently fall through.
            }
            Toast.makeText(ConnectionActivity.this, "Error logging in. Please try again later", Toast.LENGTH_LONG).show();
            FileLogger.getFileLogger().ErrorException("Exception handled: ", ex);
        }
    };

    private void updateStoredCredentials(ServerInfo serverInfo, AuthenticationResult authenticationResult) {
        if (serverInfo == null) {
            return;
        }
        if (authenticationResult == null) {
            return;
        }

        serverInfo.setAccessToken(authenticationResult.getAccessToken());
        serverInfo.setUserId(authenticationResult.getUser().getId());
        serverInfo.setUserLinkType(authenticationResult.getUser().getConnectLinkType());

        AndroidCredentialProvider credentialProvider = new AndroidCredentialProvider(new GsonJsonSerializer(), MB3Application.getInstance());
        ServerCredentials credentials = credentialProvider.GetCredentials();

        if (credentials == null) return;

        credentials.AddOrUpdateServer(serverInfo);
        credentialProvider.SaveCredentials(credentials);
    }

    //******************************************************************************************************************

    private void proceedToHomescreen() {

        FileLogger.getFileLogger().Debug("ensuring is_first_run is now false");
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPrefs.edit().putBoolean("is_first_run", false).apply();

        MB3Application.getInstance().setIsConnected(true);

        // Restore logging to the level defined in the user preferences
        FileLogger.getFileLogger().setLoggingLevel(
                sharedPrefs.getBoolean("pref_debug_logging_enabled", false)
                        ? LogLevel.Debug
                        : LogLevel.Info
        );

        MB3Application.getInstance().startContentSync();

        Intent intent;
        if (sharedPrefs.getString("pref_application_profile", "Mobile").equalsIgnoreCase("Mobile")) {
            FileLogger.getFileLogger().Info("proceeding to mobile homescreen");
            // proceed to the mobile layouts
            intent = new Intent(this, HomescreenActivity.class);
        } else {
            FileLogger.getFileLogger().Info("proceeding to living room homescreen");
            // proceed to the livingroom layouts.
            intent = new Intent(this, HomeScreenActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    protected void setOverscanValues() {
        RelativeLayout overscanLayout = (RelativeLayout) findViewById(R.id.rlOverscanPadding);

        if (overscanLayout == null) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int left = prefs.getInt("overscan_left", 0);
        int top = prefs.getInt("overscan_top", 0);
        int right = prefs.getInt("overscan_right", 0);
        int bottom = prefs.getInt("overscan_bottom", 0);

        ViewGroup.MarginLayoutParams overscanMargins = (ViewGroup.MarginLayoutParams) overscanLayout.getLayoutParams();
        overscanMargins.setMargins(left, top, right, bottom);
        overscanLayout.requestLayout();
    }

    private boolean isKnownTVDevice() {

        if (Build.MANUFACTURER != null && Build.MANUFACTURER.equalsIgnoreCase("amazon")) {
            if (Build.MODEL != null && !Build.MODEL.isEmpty()) {
                if (Build.MODEL.startsWith("AFT"))
                    return true;
            }
        }

        return false;
    }

    private void showActivityDialog(String message) {

        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(message)) {
            return;
        }

        dialog = new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.app_name))
                .setMessage(message)
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
