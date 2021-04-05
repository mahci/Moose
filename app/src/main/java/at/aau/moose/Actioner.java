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

    //--- Defined gestures and the current gesture
    private enum GESTURE {
        SWIPE_LCLICK,
        TAP_LCLICK
    }
    private GESTURE gesture = GESTURE.TAP_LCLICK;

    // Position of the leftmost finger
    private Foint lmFingerDownPos;

    // Is virtually pressed?
    private boolean vPressed = false;
    private long actionStartTime; // Both for time keeping and checking if action is started

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
//        Log.d(TAG, "Action: " + Const.actionToString(tevent.getAction()));
        //--- Process the TOUCH EVENT based on the gesture
        switch (gesture) {
        case SWIPE_LCLICK:
            doSwipeLClick(tevent);
            break;
        case TAP_LCLICK:
            doTapLClick(tevent);
            break;
        }

    }

    /**
     * Swipe down with leftmost finger to perform press-release
     * @param tevent TouchEvent
     */
    private void doSwipeLClick(TouchEvent tevent) {
        switch (tevent.getAction()) {
        // Any number of fingers down, get the leftmost finger's position
        case MotionEvent.ACTION_DOWN: case MotionEvent.ACTION_POINTER_DOWN:
            // Save the initial position of the leftmost finger
            lmFingerDownPos = tevent.getLmFingerPos();
            break;

        // Check for significant movement
        case MotionEvent.ACTION_MOVE:
//            Log.d(TAG, lmFingerDownPos.toString());
            if (lmFingerDownPos.hasCoord()) { // Only check if prev. finger down
                float dY = tevent.getLmFingerPos().y - lmFingerDownPos.y;
                float dX = tevent.getLmFingerPos().x - lmFingerDownPos.x;
                // Is it (only) down Y? => [PRESS]
                if (dY > Const.PRESS_DY_MIN_PX && dX < Const.PRESS_DX_MAX_PX) {
                    Log.d(TAG, "dX = " + dX + " | " + "dY = " + dY);
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

    /**
     * Tap with leftmost finger (Up-Down-Up) for left click
     * @param tevent TouchEvent
     */
    private void doTapLClick(TouchEvent tevent) {
        switch (tevent.getAction()) {
        // Any number of fingers down, save the leftmost finger's position
        case MotionEvent.ACTION_DOWN: case MotionEvent.ACTION_POINTER_DOWN:
            lmFingerDownPos = tevent.getLmFingerPos();
            if (tevent.isLmFinger()) {
                Log.d(TAG, "------- LM Down ---------");
                actionStartTime = System.currentTimeMillis();
            }
            break;

        // Check for significant movement
        case MotionEvent.ACTION_MOVE:


            break;

        // TAP starts from UP
        case MotionEvent.ACTION_UP: case MotionEvent.ACTION_POINTER_UP:
            long time = System.currentTimeMillis();
            if (time - actionStartTime < Const.TAP_DUR) { // Was it a tap?
                Log.d(TAG, "------- TAP! ---------");
                Networker.get().sendAction(Const.ACT_CLICK);
            }

            break;
        }
    }

}
