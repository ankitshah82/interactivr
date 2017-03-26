package com.example.ankit2.controllerapp1;

import android.Manifest;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

public class ControllerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ActivityCompat.OnRequestPermissionsResultCallback  {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //Auto generated code
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);




        //Get the bluetooth adapter
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        }

        //Request user to enable bluetooth
        else if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 10);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent i = new Intent(ControllerActivity.this, FileSystemExplorer.class);
                    startActivity(i);

                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("Info")
                            .setMessage("The file explorer demo requires this permission \n" +
                                    "in order to work. Please consider granting this permission.")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(ControllerActivity.this,
                                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                            10);
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();

                }


    }


    @Override
    public void onBackPressed() {

        //If the drawer is open, close it, else close the activity.
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.controller, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        //Show the VR mode or controller mode fragment, depending on user choice.
        FragmentManager fm = getFragmentManager();
        if (id == R.id.nav_controller_mode) {
            //Hide the welcome screen and show the controller UI.
            findViewById(R.id.welcomeView).setVisibility(View.GONE);
            fm.beginTransaction().replace(R.id.content_frame, new ControllerModeFragment()).commit();

        } else if (id == R.id.nav_vr_mode) {
            //Hide the welcome screen and show the VR mode UI.
            findViewById(R.id.welcomeView).setVisibility(View.GONE);
            fm.beginTransaction().replace(R.id.content_frame, new VRModeFragment()).commit();

        }

        //Close the drawer when the user selects an option
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    //Used by the fragments to set the action bar title.
    public void setActionBarTitle(String title) {
        getSupportActionBar().setTitle(title);
    }


    //This gets called when the user clicks on 'start' in VR mode.
    public void startVRActivity(View v) {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ControllerActivity.this);
        alertDialog.setIcon(R.drawable.ic_menu_gallery);
        alertDialog.setTitle("Select demo:");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(ControllerActivity.this, android.R.layout.select_dialog_singlechoice);
        arrayAdapter.add("3D object manipulation");
        arrayAdapter.add("VR Paint");
        arrayAdapter.add("VR File Explorer");

        alertDialog.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        alertDialog.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Start the selected vr demo
                if (0 == which) {
                    dialog.dismiss();
                    Intent i = new Intent(ControllerActivity.this, TreasureHuntActivity.class);
                    startActivity(i);
                }

                else if (1 == which) {
                    dialog.dismiss();
                    Intent i = new Intent(ControllerActivity.this, VRPaint.class);
                    startActivity(i);
                }

                else if (2 == which) {
                    dialog.dismiss();

                    if (ContextCompat.checkSelfPermission(ControllerActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {


                        ActivityCompat.requestPermissions(ControllerActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                10);
                    }
                    else
                    {
                        Intent i = new Intent(ControllerActivity.this, FileSystemExplorer.class);
                        startActivity(i);
                    }

                }
            }
        });
        alertDialog.show();
    }

    //This gets called when the user clicks on one of the buttons on the welcome screen.
    public void selectMode(View v) {
        //Show the VR mode or controller mode fragment, depending on user choice.
        FragmentManager fm = getFragmentManager();
        if (v.getId() == R.id.controllerButton) {
            //Hide the welcome screen and show the controller UI.
            findViewById(R.id.welcomeView).setVisibility(View.GONE);
            fm.beginTransaction().replace(R.id.content_frame, new ControllerModeFragment()).commit();
        } else if (v.getId() == R.id.vrButton) {
            //Hide the welcome screen and show the VR mode UI.
            findViewById(R.id.welcomeView).setVisibility(View.GONE);
            fm.beginTransaction().replace(R.id.content_frame, new VRModeFragment()).commit();
            //Intent i = new Intent(ControllerActivity.this, FileSystemExplorer.class);
            //startActivity(i);
        }
    }
}
