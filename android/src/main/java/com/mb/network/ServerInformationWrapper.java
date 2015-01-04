package com.mb.network;

import mediabrowser.model.apiclient.ServerInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mark on 11/12/13.
 */
public class ServerInformationWrapper implements Serializable {

    public List<ServerInfo> Servers;

    public ServerInformationWrapper() {
        Servers = new ArrayList<>();
    }
}
