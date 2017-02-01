package com.example.ankit2.controllerapp1;

import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;


/**
 * Created by Ankit on 1/5/2017.
 */
public class Fragment2 extends Fragment {

    BluetoothAdapter mBluetoothAdapter;
    View myView;
    BluetoothSocket socket = null;

    HandlerThread ht = new HandlerThread("xyz");
    AcceptThread accthread;


    public Handler mHandler;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.vr_mode_layout, container, false);
        ((ControllerActivity) getActivity()).setActionBarTitle("VR Mode");

        ht.start();
        mHandler = new Handler(ht.getLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {



                if ((inputMessage.what&0x0000FF00) == 0x0600)
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            TextView tv = (TextView) myView.findViewById(R.id.textView3);
                            tv.setText("");
                            tv = (TextView) myView.findViewById(R.id.textView2);
                            tv.setText("Controller connected ");

                        }
                    });

                else if((inputMessage.what&0x0000FF00) == 0x0700)
                    Toast.makeText(getActivity(), "Flick gesture detected", Toast.LENGTH_LONG).show();


            }


        };
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
        startActivity(discoverableIntent);


        accthread = new AcceptThread();
        accthread.start();

        return myView;
    }

    class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("BT_SERVER",
                        UUID.fromString("00002415-0000-1000-8000-00805F9B34FB"));
            } catch (IOException e) {
            }
            mmServerSocket = tmp;
        }

        public void run() {


            // Keep listening until exception occurs or a socket is returned.

            try {

                socket = mmServerSocket.accept();


            } catch (Exception e) {
                e.printStackTrace();


            }

            if (socket != null) {
                new ReadThread().start();
                try {
                    mmServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }


        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ReadThread extends Thread {

        byte[] mmBuffer = new byte[4];


        public void run() {



            while (socket.isConnected()) {

                try {


                    InputStream tmpIn = socket.getInputStream();

                    tmpIn.read(mmBuffer);

                    Message msg = Message.obtain();


                    msg.obj = new String(mmBuffer);
                    msg.what = mmBuffer[0];
                    msg.what = msg.what << 8;
                    msg.what += mmBuffer[1];
                    msg.what = msg.what << 8;
                    msg.what += mmBuffer[2];
                    msg.what = msg.what << 8;
                    msg.what += mmBuffer[3];

                    mHandler.sendMessage(msg);


                } catch (IOException e) {
                    e.printStackTrace();

                }

            }
        }
    }

    // Closes the connect socket and causes the thread to finish.
    public void cancel() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}



