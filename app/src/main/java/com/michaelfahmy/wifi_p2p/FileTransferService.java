package com.michaelfahmy.wifi_p2p;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class FileTransferService extends IntentService {

    private static final String LOG_TAG = "WiFiDirect/" + FileTransferService.class.getSimpleName();
    public static final String ACTION_SEND_FILE = "com.appenza.lms.SEND_FILE";
    public static final String HOST = "192.168.49.1";
    public static final int PORT = 8000;

    public FileTransferService(String name) {
        super(name);
    }

    public FileTransferService() {
        super("FileTransferService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_FILE)) {
            Uri uri = intent.getData();
            Socket socket = new Socket();

            try {
                Log.d(LOG_TAG, "Client: socket opened");
                socket.bind(null);
                Log.d(LOG_TAG, "Client: connection requested");
                socket.connect(new InetSocketAddress(HOST, PORT));
                Log.d(LOG_TAG, "Client: socket connected");


                ContentResolver cr = context.getContentResolver();
                InputStream inputStream = cr.openInputStream(uri);
                OutputStream outputStream = socket.getOutputStream();

                if(copyFile(inputStream, outputStream)) {
                    Log.d(LOG_TAG, "File copied");
                } else {
                    Log.d(LOG_TAG, "File not copied");
                }

                outputStream.close();
                socket.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, e.toString());
            }

        }
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
            Log.e(LOG_TAG, e.toString());
            return false;
        }
        return true;
    }

}
