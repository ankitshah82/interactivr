package com.example.ankit2.controllerapp1;

import android.Manifest;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
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
public class Fragment1 extends Fragment implements SensorEventListener {

    View myView;
    int reqCode = 1001;
    BroadcastReceiver mReceiver;
    public Handler mHandler;
    BluetoothAdapter bluetoothAdapter;
    private float lastX, lastY, lastZ;

    private SensorManager sensorManager;
    private Sensor accelerometer;




    private boolean startSwipe = true;

    private long lastUpdate = 0;

    private float vibrateThreshold = 0;
    public Vibrator v;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.controller_mode_layout, container, false);
        ((ControllerActivity) getActivity()).setActionBarTitle("Controller Mode");

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.bluetooth.devicepicker.action.DEVICE_SELECTED")) {
                    context.unregisterReceiver(this);
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Toast.makeText(context, "Connecting to " + device.getName(), Toast.LENGTH_SHORT).show();
                    new CommunicationThread(device).start();

                }
            }
        };
        getActivity().registerReceiver(mReceiver, new IntentFilter("android.bluetooth.devicepicker.action.DEVICE_SELECTED"));

        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, reqCode);
                } else {
                    Toast.makeText(getActivity(), "Please select your headset from the list", Toast.LENGTH_LONG).show();
                    showDevicePicker();
                }
            }
        }
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null) {
            // success! we have an accelerometer

            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            vibrateThreshold = accelerometer.getMaximumRange() / 2;
            lastUpdate = System.currentTimeMillis();
        } else {
            // fai! we dont have an accelerometer!
        }

        //initialize vibration
        v = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);


        return myView;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if ((requestCode == reqCode) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(getActivity(), "Please select your headset from the list", Toast.LENGTH_LONG).show();
            showDevicePicker();
        }
    }

    public void showDevicePicker() {
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
        getActivity().unregisterReceiver(mReceiver);
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }


    private class CommunicationThread extends Thread {
        BluetoothSocket bsock;
        byte messageBytes[];


        public CommunicationThread(BluetoothDevice bDevice) {

            try {
                bsock = bDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00002415-0000-1000-8000-00805F9B34FB"));

            } catch (IOException e) {
                e.printStackTrace();
            }
            messageBytes = new byte[4];
            messageBytes[0]=0x7F;
            messageBytes[1]=0x01;
            messageBytes[2]=0x06;
            messageBytes[3]=0x00;


        }

        public void run() {
            Looper.prepare();
            OutputStream os = null;
            try {
                bsock.connect();
                os = bsock.getOutputStream();
                os.write(messageBytes);

            } catch (IOException e) {
                e.printStackTrace();
            }
            final OutputStream bos = os;

            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (bsock.isConnected()) {
                        messageBytes[3] = (byte) (msg.what);
                        messageBytes[2] = (byte) (msg.what >>8);
                        messageBytes[1] = (byte) (msg.what >>16);
                        messageBytes[0] = (byte) (msg.what >>24);



                        try {

                            bos.write(messageBytes);
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
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if ((System.currentTimeMillis() - lastUpdate) < 300)
            return;


        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];


        double accVectorLen = Math.sqrt(x * x + y * y + z * z);
        double deltaAcc = Math.abs(accVectorLen - Math.sqrt(lastX * lastX + lastY * lastY + lastZ * lastZ));
        if (deltaAcc > 10) {
            if (startSwipe)     //This is the start of movement.
                startSwipe = false;

            else {
                startSwipe = true;
                v.vibrate(50);
                mHandler.sendEmptyMessage(0x7F010700);
                lastUpdate = System.currentTimeMillis();

            }

        }
        lastX = x;
        lastY = y;
        lastZ = z;


    }
}
