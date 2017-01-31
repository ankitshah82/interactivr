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
import android.support.annotation.Nullable;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by Ankit on 1/5/2017.
 */
public class Fragment1 extends Fragment{

    View myView;
    int reqCode = 1001;
    BroadcastReceiver mReceiver;
    BluetoothAdapter bluetoothAdapter;
    BluetoothSocket bsock;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.controller_mode_layout,container,false);
        ((ControllerActivity)getActivity()).setActionBarTitle("Controller Mode");

         bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.bluetooth.devicepicker.action.DEVICE_SELECTED")) {
                    context.unregisterReceiver(this);
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Toast.makeText(context, "Device" + device.getAddress(), Toast.LENGTH_SHORT).show();
                    try {
                        bsock = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00002415-0000-1000-8000-00805F9B34FB"));
                        bsock.connect();
                        //Send and receive data logic follows

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        getActivity().registerReceiver(mReceiver, new IntentFilter("android.bluetooth.devicepicker.action.DEVICE_SELECTED"));

        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, reqCode);
                }

                else
                {
                    Toast.makeText(getActivity(),"Please select your headset from the list", Toast.LENGTH_LONG).show();
                    showDevicePicker();
                }
            }
        }




        return myView;
    }



    @Override
    public void onRequestPermissionsResult (int requestCode, String[] permissions, int[] grantResults)
    {

        if((requestCode == reqCode) && (grantResults[0] == PackageManager.PERMISSION_GRANTED))
        {
            Toast.makeText(getActivity(),"Please select your headset from the list", Toast.LENGTH_LONG).show();
            showDevicePicker();
        }
    }

    public void showDevicePicker()
    {
        //Launch built in bluetooth device picker activity
        startActivity( new Intent("android.bluetooth.devicepicker.action.LAUNCH")
                .putExtra("android.bluetooth.devicepicker.extra.NEED_AUTH", false)
                .putExtra("android.bluetooth.devicepicker.extra.FILTER_TYPE", 0)
                .putExtra("android.bluetooth.devicepicker.extra.LAUNCH_PACKAGE","com.example.ankit2.controllerapp1")
                .putExtra("android.bluetooth.devicepicker.extra.DEVICE_PICKER_LAUNCH_CLASS",ControllerActivity.class.getClass())
                .setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS));

    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mReceiver);



        // Don't forget to unregister the ACTION_FOUND receiver.
       // getActivity().unregisterReceiver(mReceiver);
    }



}
