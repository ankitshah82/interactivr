package com.example.ankit2.controllerapp1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.os.Message;
import android.widget.Toast;

/**
 * Created by Ankit on 2/12/2017.
 */


class PinchGestureDetector extends ScaleGestureDetector.SimpleOnScaleGestureListener {
    @Override
    public boolean onScale(ScaleGestureDetector detector) {

        if (ControllerModeFragment.connected) {

            float x = detector.getCurrentSpanX();
            float y = detector.getCurrentSpanY();
            //Send scale gesture packet
            Message message = Message.obtain();
            Packet packet = new Packet();
            packet.msgType = PacketData.GESTURE_PACKET_HEADER;
            packet.gestureType = PacketData.GESTURE_TYPE_PINCH;
            packet.xPosOrVel = x;
            packet.yPosOrVel = y;

            message.obj = packet;
            ControllerModeFragment.mHandler.sendMessage(message);
        }
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {

        if (ControllerModeFragment.connected) {

            float x = detector.getCurrentSpanX();
            float y = detector.getCurrentSpanY();
            //Send scale gesture packet
            Message message = Message.obtain();
            Packet packet = new Packet();
            packet.msgType = PacketData.GESTURE_PACKET_HEADER;
            packet.gestureType = PacketData.GESTURE_TYPE_PINCH_START;
            packet.xPosOrVel = x;
            packet.yPosOrVel = y;
            message.obj = packet;
            ControllerModeFragment.mHandler.sendMessage(message);
        }

        return super.onScaleBegin(detector);
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

}

//For drawing on the circular touch area.
class TouchArea extends SurfaceView implements SurfaceHolder.Callback, GestureDetector.OnGestureListener {
    private SurfaceHolder touchAreaHolder;
    private Canvas canvas;
    private Paint touchAreaPaint;
    private Paint touchPointPaint;
    private Paint movingTouchPointPaint;
    private float touchAreaRadius;
    private float touchPointRadius;
    private float touchAreaCenterX;
    private float touchAreaCenterY;
    private ScaleGestureDetector scaleGestureDetector;
    private int numPointers = 0;

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

        movingTouchPointPaint = new Paint();
        movingTouchPointPaint.setDither(true);
        movingTouchPointPaint.setAntiAlias(true);
        movingTouchPointPaint.setStyle(Paint.Style.FILL);
        movingTouchPointPaint.setColor(Color.BLUE);

        touchAreaRadius = this.getWidth() / 2 - 20;
        touchPointRadius = 60;
        touchAreaCenterX = this.getWidth() / 2;
        touchAreaCenterY = this.getHeight() / 2;
        canvas = touchAreaHolder.lockCanvas();
        canvas.drawColor(0xffececec);
        touchAreaHolder.unlockCanvasAndPost(canvas);
        gestureDetector = new GestureDetectorCompat(getContext(), this);
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new PinchGestureDetector());
        setClickable(false);

        if (!drawnOnce) {
            new Thread() {
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (ControllerModeFragment.connected) {
                            setClickable(true);
                            break;
                        }
                    }
                    firstDrawTouchArea();
                }

            }.start();

            drawnOnce = true;
        }
    }

    //Draw the expanding touch area circle
    public void firstDrawTouchArea() {
        for (int i = 0; i < touchAreaRadius; i += 12) {
            canvas = touchAreaHolder.lockCanvas();
            canvas.drawColor(0xffececec);
            canvas.drawCircle(touchAreaCenterX, touchAreaCenterY, i, touchAreaPaint);
            touchAreaHolder.unlockCanvasAndPost(canvas);
        }

        canvas = touchAreaHolder.lockCanvas();
        canvas.drawColor(0xffececec);
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

        int action = event.getActionMasked();
        int index = MotionEventCompat.getActionIndex(event);


        if (ControllerModeFragment.connected) {
            float touchX = MotionEventCompat.getX(event, index);
            float touchY = MotionEventCompat.getY(event, index);
            if (isWithinBounds(event)) {

                if (ControllerModeFragment.connected && action == MotionEvent.ACTION_MOVE) {
                    //Send drag gesture packet
                    Message message = Message.obtain();
                    Packet packet = new Packet();
                    packet.msgType = PacketData.GESTURE_PACKET_HEADER;
                    packet.gestureType = PacketData.GESTURE_TYPE_DRAG;
                    packet.xPosOrVel = touchX;
                    packet.yPosOrVel = touchY;
                    message.obj = packet;
                    ControllerModeFragment.mHandler.sendMessage(message);
                }

                scaleGestureDetector.onTouchEvent(event);
                this.gestureDetector.onTouchEvent(event);

                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
                    canvas = touchAreaHolder.lockCanvas();
                    canvas.drawColor(0xffececec);
                    canvas.drawCircle(touchAreaCenterX, touchAreaCenterY, touchAreaRadius, touchAreaPaint);
                    touchAreaHolder.unlockCanvasAndPost(canvas);
                }

                //Touched circle follows the user's finger.
                else {
                    canvas = touchAreaHolder.lockCanvas();
                    canvas.drawColor(0xffececec);
                    canvas.drawCircle(touchAreaCenterX, touchAreaCenterY, touchAreaRadius, touchAreaPaint);
                    for (int i = 0; i < event.getPointerCount(); i++) {

                        canvas.drawCircle(MotionEventCompat.getX(event, i), MotionEventCompat.getY(event, i), touchPointRadius, movingTouchPointPaint);
                    }
                    touchAreaHolder.unlockCanvasAndPost(canvas);
                }


            }

            else
            {
                canvas = touchAreaHolder.lockCanvas();
                canvas.drawColor(0xffececec);
                canvas.drawCircle(touchAreaCenterX, touchAreaCenterY, touchAreaRadius, touchAreaPaint);
                touchAreaHolder.unlockCanvasAndPost(canvas);
            }


        }

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
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        if (ControllerModeFragment.connected) {
            //Send tap packet
            Message message = Message.obtain();
            Packet packet = new Packet();
            packet.msgType = PacketData.GESTURE_PACKET_HEADER;
            packet.gestureType = PacketData.GESTURE_TYPE_TAP;
            packet.xPosOrVel = e.getX();
            packet.yPosOrVel = e.getY();
            message.obj = packet;
            ControllerModeFragment.mHandler.sendMessage(message);
        }
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
        if (ControllerModeFragment.connected) {
            //Send swipe gesture packet
            Message message = Message.obtain();
            Packet packet = new Packet();
            packet.msgType = PacketData.GESTURE_PACKET_HEADER;
            packet.gestureType = PacketData.GESTURE_TYPE_SWIPE;
            packet.xPosOrVel = velocityX;
            packet.yPosOrVel = velocityY;
            message.obj = packet;
            ControllerModeFragment.mHandler.sendMessage(message);
        }
        return true;
    }

    class ShrinkingCircle extends Thread {
        float x, y;

        public ShrinkingCircle(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public void run() {
            for (int i = (int) touchPointRadius; i >= 0; i -= 4) {
                canvas = touchAreaHolder.lockCanvas();
                canvas.drawColor(0xffececec);
                canvas.drawCircle(touchAreaCenterX, touchAreaCenterY, touchAreaRadius, touchAreaPaint);
                canvas.drawCircle(x, y, i, touchPointPaint);
                touchAreaHolder.unlockCanvasAndPost(canvas);
            }
        }
    }
}