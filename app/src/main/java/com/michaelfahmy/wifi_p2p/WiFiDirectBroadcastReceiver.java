package com.michaelfahmy.wifi_p2p;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by michael on 23/11/15.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = WiFiDirectBroadcastReceiver.class.getSimpleName();

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private WiFiDirectActivity activity;
    private Set<WifiP2pDevice> peers = new HashSet<WifiP2pDevice>();
    private ArrayAdapter<String> adapter;
    private Set<String> deviceNames = new HashSet<String>();
    private Set<String> prevDeviceNames = new HashSet<String>();

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, WiFiDirectActivity activity) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Log.d(LOG_TAG, "WiFi Direct is enabled");
            } else {
                Log.d(LOG_TAG, "WiFi Direct is disabled");
            }

        } else if (action.equals(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)) {
            Log.d(LOG_TAG, "P2P peers changed!");
            if (manager != null) {
                manager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList devices) {
                        peers.clear();
                        peers.addAll(devices.getDeviceList());
                        Log.d(LOG_TAG, "Devices found = " + peers.size());

                        for (WifiP2pDevice device : peers) {
                            deviceNames.add(device.deviceName);
                        }

                        if (deviceNames.size() > 0 && !deviceNames.equals(prevDeviceNames)) {
                            adapter = new ArrayAdapter<String>(activity, R.layout.list_items, R.id.item_textview, new ArrayList<String>(deviceNames));
                            activity.showDeviceListDialog(adapter, new ArrayList<WifiP2pDevice>(peers));
                            prevDeviceNames = deviceNames;
                        } else {
                            Log.d(LOG_TAG, "Device List not changed!");
                        }
                    }
                });
            }

        } else if (action.equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)) {
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            Log.d(LOG_TAG, "Connection status " + networkInfo.isConnected());
            if (networkInfo.isConnected()) {
                manager.requestConnectionInfo(channel, activity);
            }
        }

    }



}
