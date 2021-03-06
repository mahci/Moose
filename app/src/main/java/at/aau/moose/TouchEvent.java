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

    private String TAG = "Moose_TouchEvent";

    private long time;
    private MotionEvent event;
    private TouchState destate;

    private final int INVALID_POINTER_ID = -1;
    private int leftFingerID = INVALID_POINTER_ID;
    private MotionEvent.PointerCoords leftPointerCoords = new MotionEvent.PointerCoords();

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
        }

        // Set the ID of the left finger
        int leftIndex;
        switch (me.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
            leftFingerID = me.getPointerId(0); // The only finger
            me.getPointerCoords(0, leftPointerCoords); // Fill the coords
            break;
        case MotionEvent.ACTION_POINTER_DOWN:
            leftIndex = findLeftFingerIndex(me);
            leftFingerID = me.getPointerId(leftIndex); // Set ID
            me.getPointerCoords(leftIndex, leftPointerCoords); // Fill the coords
            break;
        case MotionEvent.ACTION_UP:
            leftFingerID = INVALID_POINTER_ID; // No figner on the screen
            leftPointerCoords.clear();
            break;
        case MotionEvent.ACTION_POINTER_UP:
            leftIndex = findLeftFingerIndex(me);
            leftFingerID = me.getPointerId(leftIndex); // Set ID
            me.getPointerCoords(leftIndex, leftPointerCoords); // Fill the coords
            break;
        }
    }

    /**
     * Find the left finger index when multiple fingers are on the screen
     * @param me MotionEvent
     * @return index of the left finger
     */
    private int findLeftFingerIndex(MotionEvent me) {
        int leftFingerIndex = 0;
        Log.d(TAG, "fLFI - num pointers= " + me.getPointerCount());
        for (int pindex = 1; pindex < me.getPointerCount(); pindex++) {
            if (me.getX(pindex) < me.getX(0)) // Lefter finger!
                leftFingerIndex = pindex;
            if (pindex == 2) Log.d(TAG, "fLFI - 3rd pointer X= " + me.getX(pindex));
        }
        Log.d(TAG, "fLFI - leftFingerIndex= " + leftFingerIndex);
        return leftFingerIndex;
    }

    public void fillPointerCoords(int pointerID, MotionEvent.PointerCoords coords) {
        if (coords != null && pointerID > -1) {
            for (int i = 0; i < event.getPointerCount(); i++) {
                Log.d(TAG, "[fPC] index= " + i + " - ID= " + event.getPointerId(i));
            }
            Log.d(TAG, "[fPC]-----------------------------");
            Log.d(TAG, "[fPC] input ID= " + pointerID);
            int pointerIndex = event.findPointerIndex(pointerID);
            Log.d(TAG, "[fPC] pointerIndex= " + pointerIndex);
            if (pointerIndex > -1) event.getPointerCoords(pointerIndex, coords);
        }
    }

    /**
     * Get the left finger ID
     * @return Left finger ID
     */
    public int getLeftFingerID() {
        return leftFingerID;
    }

    /**
     * Get the PointerCoords for the left finger
     * @return
     */
    public MotionEvent.PointerCoords getLeftPointerCoords() {
        return leftPointerCoords;
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

    public MotionEvent getEvent() {
        return event;
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

        return new Foint(event.getX(lmInd), event.getY(lmInd));
    }

    /**
     * Find and return the top leftmost finger on the Touch Screen
     * @return Top left most finger
     */
    public Foint getTopLeftFingerPos() {
        int tlFingerInd = -1;
        float minX = 720;
//        Log.d(TAG, "N Points = " + event.getPointerCount());
        for (int pi = 0; pi < event.getPointerCount(); pi++) {
            if (event.getX(pi) < minX &&
                    event.getY(pi) < Config.PALM_AREA_Y) {
                tlFingerInd = pi;
                minX = event.getX(pi);
            }
        }

        if (tlFingerInd > -1)
            return new Foint(event.getX(tlFingerInd), event.getY(tlFingerInd));
        else
            return new Foint();
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

    /**
     * Is this the top leftmost finger?
     * @return Boolean
     */
    public boolean isTLFinger() {
        // Find the active index
        int activePntInd =
                (event.getAction() &
                        MotionEvent.ACTION_POINTER_INDEX_MASK)
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
            Log.d(TAG, "Active Index = " + activePntInd);
        // Is it the leftmost pointer?
        float activePntX = event.getX(activePntInd);
        float activePntY = event.getY(activePntInd);
        if (activePntY > Config.PALM_AREA_Y) return false; // It is really low!
        else {
            Log.d(TAG, "Inside");
            for (int pi = 0; pi < event.getPointerCount(); pi++) {
                if (event.getX(pi) < activePntX) return false;
            }
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

    /**
     * Get the main info from the class
     * @return ACTION + TIME
     */
    public String getParams() {
        return "TouchEvent{" +
                "action= " + Config.actionToString(event.getActionMasked()) +
                ", time= " + time +
                '}';
    }

    @Override
    public String toString() {
        if (event != null) {
            return "event=" + event +
                    ", time= " + time;
        } else {
            return "{Empty TouchEvent}";
        }

    }
}
