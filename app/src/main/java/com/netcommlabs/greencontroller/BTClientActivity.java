package com.netcommlabs.greencontroller;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class BTClientActivity extends AppCompatActivity {

    private static final String TAG = "BTClientActivity";
    private BluetoothAdapter mBluetoothAdapter;
    Context mContext;
    private final String UUID_STRING_WELL_KNOWN_SPP =
            "00001101-0000-1000-8000-00805F9B34FB";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btclient);

        mContext = this;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(mContext, "Device don't support Bluetooth", Toast.LENGTH_SHORT).show();
        } else {
           /* if (mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.disable();
                Toast.makeText(mContext, "Bluetooth is OFF", Toast.LENGTH_SHORT).show();
                return;
            }*/
            Intent intentBTEnableRqst = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intentBTEnableRqst, 3333);
        }
    }

   /* @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 3333 && resultCode == Activity.RESULT_OK) {
            //startDvcDiscovery();
            Toast.makeText(this, "Bluetooth is ON", Toast.LENGTH_SHORT).show();

            Intent intentDscvrble = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            startActivity(intentDscvrble);

            new ConnectThread().start();
        } else {
            Toast.makeText(this, "BT ON request failed", Toast.LENGTH_SHORT).show();
        }
    }*/

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(UUID_STRING_WELL_KNOWN_SPP));
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            Toast.makeText(mContext, "CLIENT Ready to manage connection", Toast.LENGTH_SHORT).show();
            // manageMyConnectedSocket(mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }
}
