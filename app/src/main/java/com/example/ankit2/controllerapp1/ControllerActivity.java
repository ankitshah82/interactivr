package com.example.ankit2.controllerapp1;

import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import static com.example.ankit2.controllerapp1.R.id.panButton;

public class ControllerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    Button panorama;
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
        Intent i = new Intent(ControllerActivity.this, TreasureHuntActivity.class);
        startActivity(i);
    }

    //This gets called when the user clicks on one of the buttons on the welcome screen.
    public void selectMode(View v) {
        //Show the VR mode or controller mode fragment, depending on user choice.
        FragmentManager fm = getFragmentManager();
        if (v.getId() == R.id.controllerButton) {
            //Hide the welcome screen and show the controller UI.
            findViewById(R.id.welcomeView).setVisibility(View.GONE);
            fm.beginTransaction().replace(R.id.content_frame, new ControllerModeFragment()).commit();
        }

        else if (v.getId() == R.id.vrButton) {
            //Hide the welcome screen and show the VR mode UI.
            findViewById(R.id.welcomeView).setVisibility(View.GONE);
            fm.beginTransaction().replace(R.id.content_frame, new VRModeFragment()).commit();
        }
    }



}
