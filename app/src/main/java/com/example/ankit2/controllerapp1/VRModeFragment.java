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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.UUID;


/**
 * Created by Ankit on 1/5/2017.
 */
public class VRModeFragment extends Fragment {

    View myView;

    BluetoothSocket socket = null;
    BluetoothAdapter mBluetoothAdapter;

    public static float controllerDPI;


    //For connecting over bluetooth
    ConnectionThread connThread;

    /*The handler thread will receive messages from the read thread
    and call the appropriate overridden gesture functions in the VR activity.*/
    public Handler mHandler;
    HandlerThread ht = new HandlerThread("xyz");

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.vr_mode_layout, container, false);

        //Change the action bar title when this fragment is created.
        ((ControllerActivity) getActivity()).setActionBarTitle("VR Mode");

        //start the message handler thread
        ht.start();

        //The Looper will run this code in a loop.
        mHandler = new Handler(ht.getLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                final Packet packetObj = (Packet) inputMessage.obj;

                //We have received a session start packet.
                if ((packetObj.msgType == PacketData.SESSION_START_HEADER) &&
                        (packetObj.protocolVersion == PacketData.PROTOCOL_VERSION))

                    //Make the following changes from the UI thread
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            //Display the message 'controller connected'
                            TextView tv = (TextView) myView.findViewById(R.id.textView3);
                            tv.setText("Controller connected ");

                            //Remove the circular progress bar
                            ProgressBar connectionWaitBar = (ProgressBar) myView.findViewById(R.id.progressBar2);
                            connectionWaitBar.setVisibility(View.INVISIBLE);

                            //Show the tick mark and headset images
                            ImageView tickMark = (ImageView) myView.findViewById(R.id.imageView4);
                            tickMark.setVisibility(View.VISIBLE);
                            ImageView insertImg = (ImageView) myView.findViewById(R.id.imageView);
                            insertImg.setVisibility(View.VISIBLE);

                            //Display instructions
                            tv = (TextView) myView.findViewById(R.id.textView5);
                            tv.setVisibility(View.VISIBLE);

                            tv = (TextView) myView.findViewById(R.id.textView6);
                            tv.setVisibility(View.VISIBLE);

                            //Show the start button
                            Button startButton = (Button) myView.findViewById(R.id.button);
                            startButton.setVisibility(View.VISIBLE);
                            startButton.setClickable(true);

                            controllerDPI = packetObj.controllerDPI;

                        }
                    });

                //Received a gesture packet. Only process it if the VR activity is running
                else if ((packetObj.msgType == PacketData.GESTURE_PACKET_HEADER) &&
                        (TreasureHuntActivity.isAlive()))
                {
                    if (packetObj.gestureType == PacketData.GESTURE_TYPE_FLICK)
                    {
                        TreasureHuntActivity.getInstance().onFlick();
                    }
                    else if (packetObj.gestureType == PacketData.GESTURE_TYPE_SWIPE)
                    {
                        TreasureHuntActivity.getInstance().onSwipe(packetObj.xPosOrVel, packetObj.yPosOrVel);
                    }
                    else if (packetObj.gestureType == PacketData.GESTURE_TYPE_TAP)
                    {
                        TreasureHuntActivity.getInstance().onTap(packetObj.xPosOrVel, packetObj.yPosOrVel);
                    }

                    else if (packetObj.gestureType == PacketData.GESTURE_TYPE_DRAG)
                    {
                        TreasureHuntActivity.getInstance().onDrag(packetObj.xPosOrVel, packetObj.yPosOrVel);
                    }

                    else if (packetObj.gestureType == PacketData.GESTURE_TYPE_PINCH_START)
                    {
                        TreasureHuntActivity.getInstance().onPinchStart(packetObj.xPosOrVel, packetObj.yPosOrVel);
                    }

                    else if (packetObj.gestureType == PacketData.GESTURE_TYPE_PINCH)
                    {
                        TreasureHuntActivity.getInstance().onPinch(packetObj.xPosOrVel, packetObj.yPosOrVel);
                    }
                }
            }
        };

        //get bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //ask for permission to make the device discoverable
        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
        startActivity(discoverableIntent);

        //staert the connection
        connThread = new ConnectionThread();
        connThread.start();

        return myView;
    }

    class ConnectionThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public ConnectionThread() {

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

            try {
                socket = mmServerSocket.accept();
            }

            catch (Exception e)
            {
                e.printStackTrace();
            }

            if (socket != null)
            {
                //Start reading from the socket if it is connected.
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

        Packet controllerMessage;

        public void run() {
            InputStream tmpIn ;
            ObjectInputStream tmpOis = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOis = new ObjectInputStream(tmpIn);

            } catch (IOException e) {
                e.printStackTrace();
            }

            while (socket.isConnected()) {

                try {
                    //Read the object from the socket
                    controllerMessage = (Packet) tmpOis.readObject();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //Get a message object, add the packet to it and send it.
                Message msg = Message.obtain();
                msg.obj = controllerMessage;
                mHandler.sendMessage(msg);
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


    public void panoview(View v){
        Intent intent = new Intent(getActivity(),SimpleVrPanoramaActivity.class);
        startActivity(intent);

    }
}



