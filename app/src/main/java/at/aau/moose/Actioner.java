package at.aau.moose;

import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;

import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

/**
 * Singleton class for analyzing all actions performed on the screen
 */
public class Actioner {

    private final String TAG = "Moose_Actioner";
    //==============================================

    private static Actioner self;

    //--- Defined gestures and the current gesture
    private enum INTERACTION {
        SWIPE_LCLICK,
        TAP_LCLICK
    }
    private INTERACTION interaction = INTERACTION.SWIPE_LCLICK;
    private boolean toVibrate = false;

    // Position of the leftmost finger
    private Foint lmFingerDownPos;

    // Is virtually pressed?
    private boolean vPressed = false;
    private long actionStartTime; // Both for time keeping and checking if action is started

    // [TEST]
    public Vibrator vibrator;

    // Is trial running on Expenvi? (for logging)
    public boolean isTrialRunning;

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
        // Sent to Mologger for logging (if isLogging there)
        Mologger.get().logAll(tevent);

        //--- Process the TOUCH EVENT based on the gesture
        switch (interaction) {
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
                if (dY > Config._swipeLClickDyMin && dX < Config._swipeLClickDxMax) {
                    Log.d(TAG, "dX = " + dX + " | " + "dY = " + dY);
                    if (!vPressed) {
                        Log.d(TAG, "------- Pressed ---------");
                        Networker.get().sendAction(Config.ACT_PRESS_PRI);
                        // Log
                        Mologger.get().log(tevent +
                                "--dX=" + dX +
                                "--dY=" + dY);

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
                    Networker.get().sendAction(Config.ACT_RELEASE_PRI);
                    // Log
                    Mologger.get().log(tevent.toString());

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
//                Log.d(TAG, "------- LM Down ---------");
                actionStartTime = System.currentTimeMillis();
            }
            break;

        // Check for significant movement
        case MotionEvent.ACTION_MOVE:
            break;

        // TAP starts from UP
        case MotionEvent.ACTION_UP: case MotionEvent.ACTION_POINTER_UP:
            long time = System.currentTimeMillis();
            long dt = time - actionStartTime;
            if (dt < Config.TAP_DUR) { // Was it a tap?
                Log.d(TAG, "------- TAP! ---------");
                if (toVibrate) vibrate(100);
                Networker.get().sendAction(Config.ACT_CLICK);
                // Log
                Mologger.get().log(tevent + "--dt=" + dt);
            }

            break;
        }
    }

    /**
     * [TESTING] vibrate for duration
     * @param duration Duration (ms)
     */
    private void vibrate(int duration) {
        vibrator.vibrate(duration);
    }

}
