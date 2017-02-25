package com.example.ankit2.controllerapp1;

import android.app.Fragment;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Vibrator;

/**
 * Created by Ankit on 1/5/2017.
 */
public class ControllerModeFragment extends Fragment implements SensorEventListener {

    View myView;

    BroadcastReceiver mReceiver;
    public static Handler mHandler;
    private float lastX, lastY, lastZ;


    private SensorManager sensorManager;
    private Sensor accelerometer;


    static boolean connected;
    private boolean startSwipe = true;

    private long lastUpdate = 0;


    public Vibrator v;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.controller_mode_layout, container, false);
        ((ControllerActivity) getActivity()).setActionBarTitle("Controller Mode");


        //Receiver to get the user choice from device picker activity.
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.bluetooth.devicepicker.action.DEVICE_SELECTED")) {
                    context.unregisterReceiver(this);

                    //Get the device from the intent.
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Toast.makeText(context, "Connecting to " + device.getName(), Toast.LENGTH_SHORT).show();

                    //Start the thread to communicate with that device.
                    new CommunicationThread(device).start();

                }

            }
        };
        getActivity().registerReceiver(mReceiver, new IntentFilter("android.bluetooth.devicepicker.action.DEVICE_SELECTED"));

        showDevicePicker();

        //initialize sensors
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

            lastUpdate = System.currentTimeMillis();
        } else {
            // No accelerometer
        }

        //initialize vibration
        v = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);


        return myView;
    }


    public void showDevicePicker() {
        Toast.makeText(getActivity(), "Please select your VR headset phone from the list", Toast.LENGTH_LONG).show();
        //Launch built in bluetooth device picker activity
        startActivity(new Intent("android.bluetooth.devicepicker.action.LAUNCH")
                .putExtra("android.bluetooth.devicepicker.extra.NEED_AUTH", false)
                .putExtra("android.bluetooth.devicepicker.extra.FILTER_TYPE", 0)
                .putExtra("android.bluetooth.devicepicker.extra.LAUNCH_PACKAGE", "com.example.ankit2.controllerapp1")
                .putExtra("android.bluetooth.devicepicker.extra.DEVICE_PICKER_LAUNCH_CLASS", ControllerActivity.class.getClass())
                .setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS));

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }


    private class CommunicationThread extends Thread {
        BluetoothSocket bsock;
        Packet firstMessage;


        CommunicationThread(BluetoothDevice bDevice) {

            try {
                //Create a socket to connect to the remote device.
                bsock = bDevice.createRfcommSocketToServiceRecord(UUID.fromString("00002415-0000-1000-8000-00805F9B34FB"));

            } catch (IOException e) {
                e.printStackTrace();
            }
            connected = true;
            firstMessage = new Packet();
            firstMessage.msgType = PacketData.SESSION_START_HEADER;
        }

        public void run() {
            Looper.prepare();
            OutputStream os;
            ObjectOutputStream tmpObjOs = null;
            try {
                //Connect the bluetooth socket and get the output streams
                bsock.connect();
                os = bsock.getOutputStream();
                tmpObjOs = new ObjectOutputStream(os);

                //Send the session start message
                tmpObjOs.writeObject(firstMessage);

            } catch (IOException e) {
                e.printStackTrace();
            }
            final ObjectOutputStream objOs = tmpObjOs;

            mHandler = new Handler() {
                @Override
                //Whenever a message is received here (from the sensor gesture listener or the touch gesture listener),
                //send it on the socket.
                public void handleMessage(Message msg) {
                    if (bsock.isConnected()) {
                        try {
                            Packet pck = (Packet)msg.obj;
                            objOs.writeObject(pck);
                            objOs.flush();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            Looper.loop();
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onSensorChanged(SensorEvent event) {

        //Don't immediately detect another flick after one has occurred.
        if ((System.currentTimeMillis() - lastUpdate) < 300)
            return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        //Check the magnitude of change in acceleration.
        double accVectorLen = Math.sqrt(x * x + y * y + z * z);
        double deltaAcc = Math.abs(accVectorLen - Math.sqrt(lastX * lastX + lastY * lastY + lastZ * lastZ));
        if (deltaAcc > 10) {
            if (startSwipe)     //This is the start of movement.
                startSwipe = false;

            else {
                //The movement has ended
                startSwipe = true;
                v.vibrate(50);

                //Send a flick gesture packet
                Packet packetObj = new Packet();
                packetObj.msgType = PacketData.GESTURE_PACKET_HEADER;
                packetObj.gestureType = PacketData.GESTURE_TYPE_FLICK;
                Message message = Message.obtain();
                message.obj = packetObj;
                mHandler.sendMessage(message);

                lastUpdate = System.currentTimeMillis();
            }
        }

        lastX = x;
        lastY = y;
        lastZ = z;
    }
}
