package at.aau.moose;

import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

/**
 * Singleton class for analyzing all actions performed on the screen
 */
public class Actioner {

    private static Actioner self;
    private final String TAG = "Moose_Actioner";

    //----- Actions parameters
    private int TAP_MV_LIMIT = 10; // In any direction

    private int FLING_DY_LIMIT = 100;
    private int FLING_DX_LIMIT = 10;
    private int FLING_TM_LIMIT = 150;

    // --- States and Events
    private List<TouchEvent> eventsList = new ArrayList<>();
    private List<TouchState> statesList = new ArrayList<>();

    //-- Defined gestures
    // num of fingers|finger index|sequece|duration(min-max)ms
    private String LEFT_CLICK = "-|0|PU-PD-PU|0-20";

    // Position of the leftmost finger
    private Foint lmFingerDownPos;

    private boolean vPressed = false;

    /**
     * Get the instance
     * @return Actioner instance
     */
    public static Actioner get() {
        if (self == null) {
            self = new Actioner();
        }
        return self;
    }

    /**
     * Set the initial state
     * @param inSt Initial TouchState
     */
    public void setInitState(TouchState inSt) {
        statesList.clear();
        statesList.add(inSt);
        Log.d(TAG, "Initial State: " + inSt.toSring());
    }


    /**
     * Add a state to the list
     * @param ts TouchState
     */
    public void addState(TouchState ts) {
        statesList.add(ts);
//        Log.d(TAG, "Added State: " + ts.toSring());
    }

    /**
     * Add an event to the list
     * @param te TouchEvent
     */
    public void addEvent(TouchEvent te) {
        eventsList.add(te);
//        Log.d(TAG, "Added Event: " + te.toString());
    }

    /**
     * Subscribe to get the TouchEvents
     * @param tePublisher TouchEvent Publisher
     */
    public void subscribeToEvents(PublishSubject<TouchEvent> tePublisher) {
        tePublisher
                .observeOn(Schedulers.io())
                .subscribe(tevent -> {
                    processEvent(tevent);
                });
    }


    /**
     * Process an input event
     * @param tevent TouchEvent
     */
    private void processEvent(TouchEvent tevent) {
        Log.d(TAG, "Action: " + Const.actionToString(tevent.getAction()));
        // Check the event (It's TouchEvent, not MotionEvent!!)
        switch (tevent.getAction()) {

        // Any number of fingers down, get the leftmost finger's position
        case MotionEvent.ACTION_DOWN: case MotionEvent.ACTION_POINTER_DOWN:
            // Save the initial position of the leftmost finger
            lmFingerDownPos = tevent.getLmFingerPos();
            Log.d(TAG, "DOWN: " + lmFingerDownPos.toString());
            break;

        // Check for significant movement
        case MotionEvent.ACTION_MOVE:
            Log.d(TAG, lmFingerDownPos.toString());
            if (lmFingerDownPos.hasCoord()) { // Only check if prev. finger down
                float dY = tevent.getLmFingerPos().y - lmFingerDownPos.y;
                float dX = tevent.getLmFingerPos().x - lmFingerDownPos.x;
                Log.d(TAG, "dX = " + dX + " | " + "dY = " + dY);
                // Is it (only) down Y? => [PRESS]
                if (dY > Const.PRESS_DY_MIN_PX && dX < Const.PRESS_DX_MAX_PX) {
                    if (!vPressed) {
                        Log.d(TAG, "------- Pressed ---------");
                        Networker.get().sendAction(Const.ACT_PRESS_PRI);
                        vPressed = true;
                    }
                }
            }

            break;

        // LM finger up? => [RELEASE]
        case MotionEvent.ACTION_UP: case MotionEvent.ACTION_POINTER_UP:
            if (tevent.isLmFinger()) {
                if (vPressed) {
                    Log.d(TAG, "------- Released ---------");
                    Networker.get().sendAction(Const.ACT_RELEASE_PRI);
                    vPressed = false;
                }
            }

            break;
        }
    }

}
