package com.example.ankit2.controllerapp1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

/**
 * Created by Ankit on 2/12/2017.
 */

//For drawing on the circular touch area.
class TouchArea extends SurfaceView implements SurfaceHolder.Callback, GestureDetector.OnGestureListener {
    private SurfaceHolder touchAreaHolder;
    private Canvas canvas;
    private Paint touchAreaPaint;
    private Paint touchPointPaint;
    private float touchAreaRadius;
    private float touchPointRadius;
    private float touchAreaCenterX;
    private float touchAreaCenterY;

    private boolean drawnOnce;



    private GestureDetectorCompat gestureDetector;

    public TouchArea(Context context) {
        super(context);
        touchAreaHolder = getHolder();
        touchAreaHolder.addCallback(this);
    }

    public TouchArea(Context context, AttributeSet attrs) {
        super(context, attrs);
        touchAreaHolder = getHolder();
        touchAreaHolder.addCallback(this);
    }

    public TouchArea(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        touchAreaHolder = getHolder();
        touchAreaHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        touchAreaHolder = holder;

        touchAreaPaint = new Paint();
        touchAreaPaint.setDither(true);
        touchAreaPaint.setAntiAlias(true);
        touchAreaPaint.setStyle(Paint.Style.FILL);
        touchAreaPaint.setColor(0x8832cd32);

        touchPointPaint = new Paint();
        touchPointPaint.setDither(true);
        touchPointPaint.setAntiAlias(true);
        touchPointPaint.setStyle(Paint.Style.FILL);
        touchPointPaint.setColor(Color.DKGRAY);

        touchAreaRadius = this.getWidth() / 2 -20;
        touchPointRadius = 60;
        touchAreaCenterX = this.getWidth() / 2;
        touchAreaCenterY = this.getHeight() / 2;
        canvas = touchAreaHolder.lockCanvas();
        canvas.drawColor(Color.WHITE);
        touchAreaHolder.unlockCanvasAndPost(canvas);
        gestureDetector = new GestureDetectorCompat(getContext(), this);

        if(!drawnOnce) {
            new Thread() {
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (Fragment1.connected) {

                            break;
                        }
                    }
                    firstDrawTouchArea();
                }

            }.start();

            drawnOnce = true;
        }

    }

    public void firstDrawTouchArea()
    {
        for (int i = 0; i < touchAreaRadius; i += 12) {
            canvas = touchAreaHolder.lockCanvas();
            canvas.drawColor(Color.WHITE);
            canvas.drawCircle(touchAreaCenterX, touchAreaCenterY, i, touchAreaPaint);
            touchAreaHolder.unlockCanvasAndPost(canvas);
        }

        canvas = touchAreaHolder.lockCanvas();
        canvas.drawColor(Color.WHITE);
        canvas.drawCircle(touchAreaCenterX, touchAreaCenterY, touchAreaRadius, touchAreaPaint);
        touchAreaHolder.unlockCanvasAndPost(canvas);

    }



    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float touchX = event.getX();
        float touchY = event.getY();
        if (isWithinBounds(event)) {
            if (event.getAction() == MotionEvent.ACTION_UP) {

                //Touched circle keeps shrinking till it disappears.
                for (int i = (int) touchPointRadius; i >= 0; i -= 4) {
                    canvas = touchAreaHolder.lockCanvas();
                    canvas.drawColor(Color.WHITE);
                    canvas.drawCircle(touchAreaCenterX, touchAreaCenterY, touchAreaRadius, touchAreaPaint);
                    canvas.drawCircle(touchX, touchY, i, touchPointPaint);
                    touchAreaHolder.unlockCanvasAndPost(canvas);
                }


            }

            //Touched circle follows the user's finger.
            else {
                canvas = touchAreaHolder.lockCanvas();
                canvas.drawColor(Color.WHITE);
                canvas.drawCircle(touchAreaCenterX, touchAreaCenterY, touchAreaRadius, touchAreaPaint);
                canvas.drawCircle(event.getX(), event.getY(), touchPointRadius, touchPointPaint);
                touchAreaHolder.unlockCanvasAndPost(canvas);
            }
        }


        this.gestureDetector.onTouchEvent(event);

        return true;
    }

    //Check if the touched point is inside the touch area
    private boolean isWithinBounds(MotionEvent event) {
        float xCoord = event.getX() - this.getWidth() / 2;
        float yCoord = event.getY() - this.getHeight() / 2;
        if (xCoord * xCoord + yCoord * yCoord <= (touchAreaRadius - touchPointRadius) * (touchAreaRadius - touchPointRadius))
            return true;
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        //Send tap packet
        Fragment1.mHandler.sendEmptyMessage(0x0B01037F);
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        //Send swipe gesture packet
        Fragment1.mHandler.sendEmptyMessage(0x0B01027F);
        return true;
    }
}