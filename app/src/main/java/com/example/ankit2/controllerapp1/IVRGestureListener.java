package com.example.ankit2.controllerapp1;

import android.view.MotionEvent;

/**
 * Created by Ankit on 2/12/2017.
 * IVRGestureListener: Any activity that wants to receive InteractiVR
 * gesture events should implement IVRGestureListener and override each of these
 * methods.
 */

interface IVRGestureListener {
//
    void onFlick();

    void onTap(float x, float y);

    void onSwipe(float velocityX, float velocityY);

    void onDrag(float newX, float newY);

    void onPinch(float newX, float newY);

    void onPinchStart(float newX, float newY);


    //More gestures will be added as they are implemented.
}
