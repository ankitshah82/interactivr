package com.example.ankit2.controllerapp1;

import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import android.content.Intent;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.util.Log;
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
public class Fragment2 extends Fragment{

     BluetoothAdapter mBluetoothAdapter;
    View myView;
    BluetoothSocket socket = null;
    boolean connected = false;
    HandlerThread ht = new HandlerThread("xyz");
    AcceptThread accthread;
    ReadThread rdthread;

   public  Handler  mHandler;
    private static final int REQUEST_ENABLE_BT = 10;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.vr_mode_layout,container,false);
        ((ControllerActivity)getActivity()).setActionBarTitle("VR Mode");

        ht.start();
        mHandler = new Handler(ht.getLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {

                TextView  tv = (TextView) getView().findViewById(R.id.textView3);
                tv.setText("Received Message");


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
               // Toast.makeText(getActivity(), "listening", Toast.LENGTH_SHORT).show();
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("BT_SERVER",
                        UUID.fromString("00002415-0000-1000-8000-00805F9B34FB"));
            } catch (IOException e) {
               // Toast.makeText(getActivity(), "listen exception", Toast.LENGTH_SHORT).show();
            }
            mmServerSocket = tmp;
        }

        public void run() {


            // Keep listening until exception occurs or a socket is returned.
            while (!connected) {
                try {

                    socket = mmServerSocket.accept();


                } catch (Exception e) {
                    e.printStackTrace();
                    break;

                }

                if(socket != null)
                {
                    new ReadThread().start();
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

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


        public ReadThread() {

        }

        public void run() {
           // Toast.makeText(getActivity(), "read thread started", Toast.LENGTH_SHORT).show();


            while(true) {
                if(connected)
                try {
                    if (socket != null) {

                        InputStream tmpIn;
                        byte[] mmBuffer = new byte[3];
                        tmpIn = socket.getInputStream();

                        tmpIn.read(mmBuffer);
                        mHandler.sendEmptyMessage(5);

                        String readString = new String(mmBuffer);
                        socket.close();
                        break;
                    }



                } catch (IOException e) {
                    //do something

                }
            }
        }
    }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {

        }
    }



