package com.michaelfahmy.wifi_p2p;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class WiFiDirectActivity extends Activity implements WifiP2pManager.ConnectionInfoListener {

    private final String LOG_TAG = WiFiDirectActivity.class.getSimpleName();
    private static boolean server_running = false;

    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    BroadcastReceiver receiver;
    IntentFilter filter;
    ArrayAdapter<String> adapter;
    ArrayList<WifiP2pDevice> peers;
    ListView list;

    ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_direct);

        Log.d(LOG_TAG, "Inside " + LOG_TAG + "/onCreate");


        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);

        list = (ListView) findViewById(R.id.listView);


        filter = new IntentFilter();
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        progress = new ProgressDialog(WiFiDirectActivity.this);

    }


    public void discover(View view) {
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(WiFiDirectActivity.this, "Peers discovery initiated", Toast.LENGTH_SHORT).show();
                Log.d(LOG_TAG, "Peers discovered successfully!");
            }

            @Override
            public void onFailure(int i) {
                Log.d(LOG_TAG, "Peers discovery fails!");
            }
        });
    }

    private static final int CHOOSE_FILE_REQUEST_CODE = 10;

    public void chooseFile(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == CHOOSE_FILE_REQUEST_CODE) {
            Uri uri = data.getData();
            Intent serviceIntent = new Intent(this, FileTransferService.class);
            serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
            serviceIntent.setData(uri);
            startService(serviceIntent);
            if (!server_running) {
                new FileServerTask().execute();
                server_running = true;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }


    public void populateList(ArrayAdapter<String> adapter, final ArrayList<WifiP2pDevice> peers) {
        this.adapter = adapter;
        this.peers = peers;
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(LOG_TAG, "device clicked");
                onDeviceSelected(position);
                progress.setMessage("Connecting to " + peers.get(position).deviceName);
                progress.show();
            }
        });
//        PeersDialog deviceListDialog = new PeersDialog();
//        deviceListDialog.show(getFragmentManager(), "devices");
    }


//    public static class PeersDialog extends DialogFragment {
//
//        @Override
//        public Dialog onCreateDialog(Bundle savedInstanceState) {
//
//            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//
//            builder.setTitle("Choose devices")
//
//                    .setSingleChoiceItems(adapter, 0, WiFiDirectActivity.this)
////                    .setMultiChoiceItems(adapter, null, new DialogInterface.OnMultiChoiceClickListener() {
////                        @Override
////                        public void onClick(DialogInterface dialogInterface, int i, boolean isChecked) {
////                            if (isChecked) {
////                                selectedDevices.add(i);
////                            } else if (selectedDevices.contains(i)) {
////                                selectedDevices.remove(i);
////                            }
////                        }
////                    })
////                    .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
////                        @Override
////                        public void onClick(DialogInterface dialog, int i) {
////
////                        }
////                    })
////                    .setNeutralButton("Check all", new DialogInterface.OnClickListener() {
////                        @Override
////                        public void onClick(DialogInterface dialog, int i) {
////                            selectedDevices = ;
////                            dialog.dismiss();
////                        }
////                    })
//                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int i) {
//                            dialog.dismiss();
//                        }
//                    });
//
//
//            return builder.create();
//        }
//    }



//    @Override
//    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//        Log.d(LOG_TAG, "device clicked");
//        onDeviceSelected(position);
//        progress.setMessage("Connecting to " + peers.get(position).deviceName);
//        progress.show();
//    }


    public void onDeviceSelected(int i) {
        WifiP2pDevice device = peers.get(i);
        if (device == null) return;

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        Log.d(LOG_TAG, "Connecting with " + device.toString());
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // broadcast will trigger an action
            }

            @Override
            public void onFailure(int i) {
                Log.e(LOG_TAG, "Device connection failed");
            }
        });
    }


    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        progress.dismiss();
        Log.d(LOG_TAG, "Connection Info Available - Group owner IP: " + info.groupOwnerAddress.getHostAddress() + "=====================>" + info.isGroupOwner);
        if (!server_running) {
            new FileServerTask().execute();
            server_running = true;
        }
    }





    private class FileServerTask extends AsyncTask<Void, Void, File> {

        private final String LOG_TAG = "WiFiDirect/FileServer:";

        @Override
        protected File doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(FileTransferService.PORT);
                Log.d(LOG_TAG, "Server: socket opened");
                Socket client = serverSocket.accept();
                Log.d(LOG_TAG, "Server: connection accepted");


                File file = new File(Environment.getExternalStorageDirectory() + "/WifiDirectSharedFiles/file.jpg");
                File dirs = new File(file.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                if(!file.createNewFile()) {
                    Log.d(LOG_TAG, "file not created");
                }

                InputStream inputStream = client.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(file);

                if(copyFile(inputStream, outputStream)) {
                    Log.d(LOG_TAG, "File copied");
                } else {
                    Log.d(LOG_TAG, "File not copied");
                }

                serverSocket.close();
                server_running = false;
                return file;
//                return file.getAbsolutePath();

            } catch (IOException e) {
                e.printStackTrace();
                Log.e(LOG_TAG, e.toString());
            }
            return null;
        }

        private boolean copyFile(InputStream inputStream, OutputStream out) {
            byte buf[] = new byte[1024];
            int len;
            try {
                while ((len = inputStream.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                out.close();
                inputStream.close();
            } catch (IOException e) {
                Log.d(LOG_TAG, e.toString());
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(File f) {
            Log.d(LOG_TAG, "File Uri: " + Uri.fromFile(f));
            if (f != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(f), "image/*");
                startActivity(intent);
            }
        }

    }


}
