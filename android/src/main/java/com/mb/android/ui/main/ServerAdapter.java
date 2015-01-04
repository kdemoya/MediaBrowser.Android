package com.mb.android.ui.main;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mb.android.R;
import mediabrowser.model.apiclient.ServerInfo;

import java.util.List;

/**
 * Created by Mark on 12/12/13.
 *
 * BaseAdapter that shows a user a list of available servers
 */
public class ServerAdapter extends BaseAdapter {

    List<ServerInfo> mAvailableServers;
    Context mContext;
    LayoutInflater mLayoutInflater;
    ServerInfo mCurrentServer;


    public ServerAdapter(List<ServerInfo> servers, Context context, ServerInfo currentServer) {
        mAvailableServers = servers;
        mContext = context;
        mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mCurrentServer = currentServer;

    }


    public void addServer(ServerInfo server) {
        mAvailableServers.add(server);
        notifyDataSetChanged();
    }

    public void updateExistingServer(ServerInfo server) {

        if (mAvailableServers == null || mAvailableServers.size() == 0 || server == null) return;

        int index = -1;

        for (ServerInfo info : mAvailableServers) {
            if (info.getId().equalsIgnoreCase(server.getId()))
                index = mAvailableServers.indexOf(info);
        }

        if (index >= 0) {
            mAvailableServers.set(index, server);
            notifyDataSetChanged();
        }
    }

    public void removeServer(String serverId) {

        if (mAvailableServers == null || mAvailableServers.size() == 0 || serverId == null) return;

        int index = -1;

        for (ServerInfo info : mAvailableServers) {
            if (info.getId().equalsIgnoreCase(serverId))
                index = mAvailableServers.indexOf(info);
        }

        if (index >= 0) {
            mAvailableServers.remove(index);
            notifyDataSetChanged();
        }
    }

    public int getCount() {
        if (mAvailableServers != null)
            return mAvailableServers.size();
        else
            return 0;
    }


    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.widget_server_tile, parent, false);
        }

        TextView overlayConnected = (TextView) convertView.findViewById(R.id.tvConnected);

        if (mCurrentServer != null && mCurrentServer.getId() != null && mCurrentServer.getId().equalsIgnoreCase(mAvailableServers.get(position).getId())) {
            overlayConnected.setVisibility(View.VISIBLE);
        } else {
            overlayConnected.setVisibility(View.INVISIBLE);
        }

        TextView tileFriendlyName = (TextView) convertView.findViewById(R.id.tvServerTileName);
        TextView tileIpAddress = (TextView) convertView.findViewById(R.id.tvServerIp);
        TextView tileExternalIpAddress = (TextView) convertView.findViewById(R.id.tvServerExternalIp);

        tileFriendlyName.setText(mAvailableServers.get(position).getName());
        tileIpAddress.setText(mAvailableServers.get(position).getLocalAddress());
        tileExternalIpAddress.setText(mAvailableServers.get(position).getRemoteAddress());

        return convertView;
    }


    public Object getItem(int position) {
        return mAvailableServers.get(position);
    }


    public long getItemId(int position) {
        return 0;
    }
}
