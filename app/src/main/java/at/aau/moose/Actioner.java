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

    private static Actioner self;

    //--- Defined gestures and the current gesture
    public enum TECHNIQUE {
        SWIPE,
        TAP,
        MOUSE
    }
    public TECHNIQUE _technique;
    private boolean toVibrate = false;



    // Position of the leftmost finger
    private Foint lmFingerDownPos = new Foint();
    private Foint tlFingerPos = new Foint();
    private final int INVALID_POINTER_ID = -1;
    private int leftPointerID = INVALID_POINTER_ID;
    private int upPointerIndex;
    private MotionEvent.PointerCoords startPCoords = new MotionEvent.PointerCoords();
    private MotionEvent.PointerCoords endPCoords = new MotionEvent.PointerCoords();
    private long actionStartTime; // Both for time keeping and checking if action is started
    private long pressDuration;
    private long duration;
    private float dX, dY;
    private int nExtraFingers;
    private MotionEvent.PointerCoords extra1PCoords = new MotionEvent.PointerCoords();
    private MotionEvent.PointerCoords extra2PCoords = new MotionEvent.PointerCoords();

    // Is virtually pressed?
    private boolean vPressed = false;
    private boolean actionCancelled;

    // [TEST]
    public Vibrator vibrator;

    // Is trial running on Expenvi? (for logging)
    public boolean isTrialRunning;

    // For calling the Networker (can't call directly)
    private PublishSubject<String> mssgPublisher;

    // ===============================================================================

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

    public Actioner() {
        mssgPublisher = PublishSubject.create();
        Networker.get().subscribeToMessages(mssgPublisher);
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
    public void processEvent(TouchEvent tevent) {
//        Log.d(TAG, "Action: " + Const.actionToString(tevent.getAction()));
        // Sent to Mologger for logging (if isLogging there)
        Mologger.get().logAll(tevent);

        //--- Process the TOUCH EVENT based on the gesture
        if (_technique != null) {
            switch (_technique) {
            case SWIPE:
                doSwipeLClick(tevent);
                break;
            case TAP:
                doTapLClick(tevent);
                break;
            }
        }

    }

    /**
     * Process the given MotionEvent
     * @param me MotionEvent
     */
    public void processMotion(MotionEvent me) {
        //--- Process the TOUCH EVENT based on the gesture
        if (_technique != null) {
            switch (_technique) {
            case SWIPE:
                swipeLClick(me);
                break;
            case TAP:
                tapLClick(me);
                break;
            }
        }
    }

    /**
     * SWIPE L-CLICK
     * @param me MotionEvent
     */
    public void swipeLClick(MotionEvent me) {
        int leftIndex;
        switch (me.getActionMasked()) {
            // Only one finger is on the screen
        case MotionEvent.ACTION_DOWN:
            leftIndex = 0;
            leftPointerID = me.getPointerId(leftIndex); // The only finger
            me.getPointerCoords(0, startPCoords); // Fill the coords
            break;
            // More fingers are added
        case MotionEvent.ACTION_POINTER_DOWN:
            leftIndex = findLeftPointerIndex(me); // Find the new left pointer
            leftPointerID = me.getPointerId(leftIndex); // Set ID
            me.getPointerCoords(leftIndex, startPCoords); // Fill the coords
            break;
            // Last finger is up
        case MotionEvent.ACTION_UP:
            // Check if the active finger has gone up
            upPointerIndex = me.getActionIndex();
            if (vPressed && me.getPointerId(upPointerIndex) == leftPointerID) { // Release
                Log.d(TAG, "------- Released ---------");
                vPressed = false;
                duration = System.currentTimeMillis() - actionStartTime;
                mssgPublisher.onNext(Strs.ACT_RELEASE_PRI);

                // log the release
                Mologger.get().logMeta(
                        startPCoords, endPCoords,
                        pressDuration, duration,
                        dX, dY,
                        nExtraFingers, extra1PCoords, extra2PCoords);
            }

            // Reset all the values
            leftPointerID = INVALID_POINTER_ID; // No figner on the screen
            startPCoords.clear();
            actionStartTime = 0;
            break;
            // Second, third, ... fingers are up
        case MotionEvent.ACTION_POINTER_UP:
            // Check if the active finger has gone up
            upPointerIndex = me.getActionIndex();
            if (vPressed && me.getPointerId(upPointerIndex) == leftPointerID) { // Release
                Log.d(TAG, "------- Released ---------");
                vPressed = false;
                duration = System.currentTimeMillis() - actionStartTime;
                mssgPublisher.onNext(Strs.ACT_RELEASE_PRI); // Send the action

                // log the release
                Mologger.get().logMeta(
                        startPCoords, endPCoords,
                        pressDuration, duration,
                        dX, dY,
                        nExtraFingers, extra1PCoords, extra2PCoords);
            }

            // Recalculate the left finger
            leftIndex = findLeftPointerIndex(me);
            leftPointerID = me.getPointerId(leftIndex); // Set ID
            me.getPointerCoords(leftIndex, startPCoords); // Fill the coords
            break;
            // Main process
        case MotionEvent.ACTION_MOVE:
            if (startPCoords.x != 0) { // There is coords
                // Set the start of the movement time
                if (actionStartTime == 0) actionStartTime = System.currentTimeMillis();

                MotionEvent.PointerCoords newLeftPCoords = new MotionEvent.PointerCoords();
                int activePIndex = me.findPointerIndex(leftPointerID);
                if (activePIndex > -1) {
                    me.getPointerCoords(activePIndex, newLeftPCoords);
                }
//                Log.d(TAG, "leftPointerID= " + leftPointerID);
//
//                Log.d(TAG, "activePIndex= " + activePIndex);
//                Log.d(TAG, "newLeftPCoords= " + newLeftPCoords.y);
                // Check the movement condition
                dY = newLeftPCoords.y - startPCoords.y;
//                Log.d(TAG, "dY= " + dY);
                if (dY > Config._swipeLClickDyMin) {

                    if (!vPressed) { // Only press once
                        vPressed = true; // Flag
                        Log.d(TAG, "------- Pressed ---------");
                        mssgPublisher.onNext(Strs.ACT_PRESS_PRI); // Send the action
                    }

                    pressDuration = System.currentTimeMillis() - actionStartTime; // Press dur.
                    endPCoords.copyFrom(newLeftPCoords); // Action end coords
                    dX = newLeftPCoords.x - startPCoords.x; // dX
                    nExtraFingers = me.getPointerCount() - 1;
                    if (nExtraFingers > 0) me.getPointerCoords(1, extra1PCoords);
                    if (nExtraFingers > 1) me.getPointerCoords(2, extra2PCoords);
                }
            }
            break;

        }
    }

    /**
     * TAP L-CLICK
     * @param me MotionEvent
     */
    public void tapLClick(MotionEvent me) {
        int leftIndex;
        switch (me.getActionMasked()) {
        // Only one finger is on the screen
        case MotionEvent.ACTION_DOWN:
            leftIndex = 0;
            leftPointerID = me.getPointerId(leftIndex); // The only finger
            me.getPointerCoords(0, startPCoords); // Fill the coords

            // Start the TAP action
            actionStartTime = System.currentTimeMillis();
            break;
        // More fingers are added
        case MotionEvent.ACTION_POINTER_DOWN:
            leftIndex = findLeftPointerIndex(me); // Find the new left pointer
            leftPointerID = me.getPointerId(leftIndex); // Set ID
            me.getPointerCoords(leftIndex, startPCoords); // Fill the coords

            // Start the TAP action
            actionStartTime = System.currentTimeMillis();
            break;
        // Last finger is up
        case MotionEvent.ACTION_UP:
            // Check if the active finger has gone up
            upPointerIndex = me.getActionIndex();
            if (me.getPointerId(upPointerIndex) == leftPointerID) { // TAP is done?
                duration = System.currentTimeMillis() - actionStartTime;
                double dist = Utils.distance(startPCoords, endPCoords);
                if (dist <= Config._swipeLClickDyMin && duration < Config.TAP_DUR) {
                    Log.d(TAG, "------- Click ---------");
                    mssgPublisher.onNext(Strs.ACT_CLICK);

                    // log the release
                    dX = endPCoords.x - startPCoords.x;
                    dY = endPCoords.y - startPCoords.y;
                    Mologger.get().logMeta(
                            startPCoords, endPCoords,
                            pressDuration, duration,
                            dX, dY,
                            nExtraFingers, extra1PCoords, extra2PCoords);
                }
            }

            // Reset all the values
            leftPointerID = INVALID_POINTER_ID; // No figner on the screen
            startPCoords.clear();
            actionStartTime = 0;
            break;
        // Second, third, ... fingers are up
        case MotionEvent.ACTION_POINTER_UP:
            // Check if the active finger has gone up
            upPointerIndex = me.getActionIndex();
            if (me.getPointerId(upPointerIndex) == leftPointerID) { // TAP is done?
                duration = System.currentTimeMillis() - actionStartTime;
                double dist = Utils.distance(startPCoords, endPCoords);
                if (dist <= Config._swipeLClickDyMin && duration < Config.TAP_DUR) {
                    Log.d(TAG, "------- Click ---------");
                    mssgPublisher.onNext(Strs.ACT_CLICK);

                    // log the release
                    dX = endPCoords.x - startPCoords.x;
                    dY = endPCoords.y - startPCoords.y;
                    Mologger.get().logMeta(
                            startPCoords, endPCoords,
                            pressDuration, duration,
                            dX, dY,
                            nExtraFingers, extra1PCoords, extra2PCoords);
                }
            }

            // Recalculate the left finger
            leftIndex = findLeftPointerIndex(me);
            leftPointerID = me.getPointerId(leftIndex); // Set ID
            me.getPointerCoords(leftIndex, startPCoords); // Fill the coords
            break;
        // Main process
        case MotionEvent.ACTION_MOVE:
            if (startPCoords.x != 0) { // There is coords
                int activePIndex = me.findPointerIndex(leftPointerID);
                if (activePIndex > -1) {
                    me.getPointerCoords(activePIndex, endPCoords);
                }
            }
            break;

        }
    }

    public int findLeftPointerIndex(MotionEvent me) {
        int leftPointerIndex = 0;

        for (int pindex = 1; pindex < me.getPointerCount(); pindex++) {
            if (me.getX(pindex) < me.getX(0)) // Lefter finger!
                leftPointerIndex = pindex;
        }

        return leftPointerIndex;
    }

    /**
     * Swipe down with leftmost finger to perform press-release
     * @param tevent TouchEvent
     */
    private void doSwipeLClick(TouchEvent tevent) {

        switch (tevent.getAction()) {
        case MotionEvent.ACTION_DOWN: case MotionEvent.ACTION_POINTER_DOWN:
            // Active = the only finger on the screen
//            activePointerID = tevent.getEvent().getPointerId(0);
            leftPointerID = tevent.getLeftFingerID();
            startPCoords = tevent.getLeftPointerCoords();
            Log.d(TAG, "leftPointerID= " + leftPointerID);
            Log.d(TAG, "leftPCoords= " + startPCoords.y);
            break;

        // Check for significant movement
        case MotionEvent.ACTION_MOVE:
//            Log.d(TAG, tlFingerPos.toString());
//            int nPointers = tevent.getEvent().getPointerCount();
//            for (int pi = 0; pi < nPointers; pi ++) {
//                int pindex = tevent.getEvent().findPointerIndex(pi);
//                MotionEvent.PointerCoords pcoords = new MotionEvent.PointerCoords();
//                tevent.getEvent().getPointerCoords(pi, pcoords);
//                Log.d(TAG, "P" + pi + " - index : " +  pindex +
//                        "| Axis value : " + tevent.getEvent().getAxisValue(MotionEvent.AXIS_Y, pi) +
//                        "| Coords : " + pcoords);
//            }
            if (startPCoords.x != 0) { // There is coords
                MotionEvent.PointerCoords newLeftPCoords = new MotionEvent.PointerCoords();
                int activePIndex = tevent.getEvent().findPointerIndex(leftPointerID);
                Log.d(TAG, "activePIndex= " + activePIndex);
                if (activePIndex > -1) {
                    tevent.getEvent().getPointerCoords(activePIndex, newLeftPCoords);
                }
                Log.d(TAG, "leftPointerID= " + leftPointerID);
                Log.d(TAG, "newLeftPCoords= " + newLeftPCoords.y);
                // Check the movement condition
                float dY = newLeftPCoords.y - startPCoords.y;
                Log.d(TAG, "dY= " + dY);
                if (dY > Config._swipeLClickDyMin) {
                    Log.d(TAG, "------- Pressed ---------");
                }
            }

//            if (tlFingerPos.hasCoord()) { // Only check if prev. finger down
//                float dY = tevent.getTopLeftFingerPos().y - tlFingerPos.y;
//                float dX = tevent.getTopLeftFingerPos().x - tlFingerPos.x;
////                Log.d(TAG, "TLFP Prev: " + tlFingerPos);
////                Log.d(TAG, "TLFP Next: " + tevent.getTopLeftFingerPos());
////                Log.d(TAG, "dX = " + dX + " | " + "dY = " + dY);
//                // Is it down Y? => [PRESS]
////                Log.d(TAG, "dY = " + dY);
//                if (dY > Config._swipeLClickDyMin) {
////                    Log.d(TAG, "Coords: " + tevent.getTopLeftFingerPos());
//                    if (!vPressed) {
////                        Log.d(TAG, "------- Pressed ---------");
//                        Networker.get().sendAction(Strs.ACT_PRESS_PRI);
//                        // Log
//                        Mologger.get().log(tevent +
//                                "--dX=" + dX +
//                                "--dY=" + dY);
//
//                        vPressed = true;
//                    }
//                }
//            }

            break;

        // LM finger up? => [RELEASE]
        case MotionEvent.ACTION_UP: case MotionEvent.ACTION_POINTER_UP:
            // Update the ID
            leftPointerID = tevent.getLeftFingerID();
            startPCoords = tevent.getLeftPointerCoords();
//            if (tevent.isTLFinger()) {
//                if (vPressed) {
//                    Log.d(TAG, "------- Released ---------");
//                    Networker.get().sendAction(Strs.ACT_RELEASE_PRI);
//                    // Log
//                    Mologger.get().log(tevent.toString());
//
//                    vPressed = false;
//                }
//            }

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
//            lmFingerDownPos = tevent.getLmFingerPos();
            tlFingerPos = tevent.getTopLeftFingerPos();
            if (tevent.isTLFinger()) {
//                Log.d(TAG, "------- LM Down ---------");
                actionStartTime = System.currentTimeMillis();
                actionCancelled = false;
            }
            break;

        // Check for significant movement
        case MotionEvent.ACTION_MOVE:
            Log.d(TAG, tlFingerPos.toString());
            // If moved more than a threshold, cancel the action
            if (tlFingerPos.hasCoord()) { // Only check if prev. finger down
                double mDist = tevent.getTopLeftFingerPos().dist(tlFingerPos);
                Log.d(TAG, "mDist = " + mDist);
                if (mDist >= Config._swipeLClickDyMin) {
                    actionCancelled = true;
                }
            }

            break;

        // TAP starts from UP
        case MotionEvent.ACTION_UP: case MotionEvent.ACTION_POINTER_UP:
            long time = System.currentTimeMillis();
            long dt = time - actionStartTime;
            if (!actionCancelled && dt < Config.TAP_DUR) { // TAP recognized!
                Log.d(TAG, "------- TAP! ---------");
                if (toVibrate) vibrate(100);
                Networker.get().sendAction(Strs.ACT_CLICK);
                // Log
                Mologger.get().logAll(tevent + "--dt=" + dt);
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

    /**
     * Set the technique
     * @param techStr Name of the technique
     */
    public void setTechnique(String techStr) {
        Log.d(TAG, "Technique set: " + techStr);
        switch (techStr) {
        case "SWIPE":
            _technique = TECHNIQUE.SWIPE;
            break;
        case "TAP":
            _technique = TECHNIQUE.TAP;
            break;
        case "MOUSE":
            _technique = TECHNIQUE.MOUSE;
            break;
        }

    }
}
