package at.aau.moose;

import android.util.Log;
import android.view.MotionEvent;

/***
 * Class for every touch events
 *
 * time: time of the event (in millisecs)
 * event: the MotionEvent
 * destate: destination state after the event
 */
public class TouchEvent {

    private String TAG = "TouchEvent";

    private long time;
    private MotionEvent event;
    private TouchState destate;

    private float leftFingerPosX, leftFingerPosY;

    /**
     * Constructor
     * @param me MotionEvent
     * @param t Time of the event
     */
    public TouchEvent(MotionEvent me, long t) {
        event = me;
        time = t;

        // Set the position of the left finger
        if (me.getActionMasked() == MotionEvent.ACTION_DOWN) {
            leftFingerPosX = me.getX();
            leftFingerPosY = me.getY();

            Log.d(TAG, "Finger down Y = " + leftFingerPosY);
        }
    }

    public void setDestate(TouchState ts) {
        destate = ts;
    }

    /**
     * Get the (masked) action
     * @return Action
     */
    public int getAction() {
        return event.getActionMasked();
    }

    /**
     * Return the position of the left finger
     * @return Foint for the position
     */
    public Foint getLmFingerPos() {

        // Find the leftmost figner
        int lmInd = 0;
        for (int pi = 0; pi < event.getPointerCount(); pi++) {
            if (event.getX(pi) < event.getX(lmInd)) lmInd = pi;
        }

        Foint foint = new Foint();
        foint.x = event.getX(lmInd);
        foint.y = event.getY(lmInd);

        return foint;
    }

    /**
     * Is the event done by the leftmost finger?
     * @return true/false
     */
    public boolean isLmFinger() {
        // Find the active index
        int activePntInd =
                (event.getAction() &
                        MotionEvent.ACTION_POINTER_INDEX_MASK)
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
//            Log.d(TAG, "Active Index = " + activePntInd);
        // Is it the leftmost pointer?
        float activePntX = event.getX(activePntInd);
        for (int pi = 0; pi < event.getPointerCount(); pi++) {
            if (event.getX(pi) < activePntX) return false;
        }
        return true;
    }

    public float getLeftFingerYMovement() {
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            float dY = Math.abs(event.getY() - leftFingerPosY);
//            for (int hi = 0; hi < event.getHistorySize(); hi++) {
//                dY += event.getHistoricalY(hi);
//            }

            Log.d(TAG, "init Y = " + leftFingerPosY);
            // Get active pointer index
            int activePntInd =
                    (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                            >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
//            Log.d(TAG, "Active Index = " + activePntInd);
            // Is it the leftmost pointer?
            float activePntX = event.getX(activePntInd);
            for (int pi = 0; pi < event.getPointerCount(); pi++) {
                if (event.getX(pi) < activePntX) return 0;
            }
//            Log.d(TAG, "Left Move");
            // Next, is it only y movement?
            if (event.getAxisValue(MotionEvent.AXIS_X, activePntInd) > 0) return 0;
            else return event.getAxisValue(MotionEvent.AXIS_Y, activePntInd);
        }

        return 0;
    }

    @Override
    public String toString() {
        return "TouchEvent{" +
                "action= " + Const.actionToString(event.getActionMasked()) +
                ", time= " + time +
                '}';
    }
}
