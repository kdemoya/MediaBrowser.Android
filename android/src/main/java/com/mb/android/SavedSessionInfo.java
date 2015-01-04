package com.mb.android;

import java.io.Serializable;

/**
 * Created by Mark on 2014-09-18.
 */
public class SavedSessionInfo implements Serializable {

    public String serverId;
    public String serverName;
    public String internalAddress;
    public String externalAddress;
    public boolean useInternalAddress;
    public String userId;
    public String userName;
    public String authToken;
}
