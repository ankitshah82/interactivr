package com.example.ankit2.controllerapp1;

/**
 * Created by Ankit on 2/12/2017.
 * IVRGestureListener: Any activity that wants to receive InteractiVR
 * gesture events should implement IVRGestureListener and override each of these
 * methods.
 */

interface IVRGestureListener {

    void onFlick();

    void onTap();

    void onSwipe();


}
