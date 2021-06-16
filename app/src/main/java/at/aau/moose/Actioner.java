package at.aau.moose;

import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;

import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

import static android.view.MotionEvent.INVALID_POINTER_ID;
import  android.view.MotionEvent.PointerCoords;

import java.util.ArrayList;
import java.util.List;

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

    MotionEvent.PointerCoords newLeftPCoords = new MotionEvent.PointerCoords();

    // Position of the leftmost finger
    private Foint lmFingerDownPos = new Foint();
    private Foint tlFingerPos = new Foint();
    private int leftPointerID = INVALID_POINTER_ID;
    private int upPointerIndex;
    private PointerCoords startPCoords = new PointerCoords();
    private PointerCoords endPCoords = new PointerCoords();
    private long actionStartTime; // Both for time keeping and checking if action is started
//    private long pressDuration;
//    private long duration;
    private float dX, dY;
    private int nExtraFingers;
    private PointerCoords extra1PCoords = new PointerCoords();
    private PointerCoords extra2PCoords = new PointerCoords();

    private List<Integer> sortedIDList = new ArrayList<>();

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
    public void processEvent(MotionEvent me) {
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
        int leftIndex, actionIndex;
        long actionDuration;
        float leftDY;

        switch (me.getActionMasked()) {
            // Only one finger is on the screen
        case MotionEvent.ACTION_DOWN:
            leftIndex = 0; // The only finger
            leftPointerID = me.getPointerId(leftIndex); // ID
            me.getPointerCoords(leftIndex, startPCoords); // Fill the coords
            break;
            // More fingers are added
        case MotionEvent.ACTION_POINTER_DOWN:
            actionIndex = me.getActionIndex(); // Which pointer is down?
            // If new finger on the left
            if (isLeftMost(me, actionIndex)) {
                leftPointerID =  me.getPointerId(actionIndex); // Set ID
                me.getPointerCoords(actionIndex, startPCoords); // Update the coords
            }
            break;
            // Movement...
        case MotionEvent.ACTION_MOVE:
            if (leftPointerID != INVALID_POINTER_ID) { // We have a leftmost finger
//                printPointers(me); // TEST
                leftIndex = me.findPointerIndex(leftPointerID);
                if (leftIndex != -1) { // ID found

                    // Always calculated the amount of movement of the leftmost finger
                    leftDY = me.getY(leftIndex) - startPCoords.y;

                    if (!vPressed) { // NOT pressed
                        // Set the start of the movement time
                        if (actionStartTime == 0) actionStartTime = System.currentTimeMillis();

                        // Did it move enough?
                        if (leftDY > Config.SWIPE_LCLICK_DY_MIN) { // SWIPED!
                            vPressed = true; // Flag
                            Log.d(TAG, "--------------- Pressed ---------------");
                            mssgPublisher.onNext(Strs.ACT_PRESS_PRI); // Send the action
                        }
                    }

                } else { // leftmost ID is no longer on the screen => find the new one
                    // Recalculate the left finger (we know last active leftmost is up now)
                    leftIndex = findLeftPointerIndex(me);
                    leftPointerID = me.getPointerId(leftIndex); // Set ID
                    me.getPointerCoords(leftIndex, startPCoords); // Fill the coords
                }
            }

            break;
        // Ond of the pointers is up
        case MotionEvent.ACTION_POINTER_UP:
            // Check if the active finger has gone up
            actionIndex = me.getActionIndex();
            if (me.getPointerId(actionIndex) == leftPointerID) { // Leftmost finger is UP

                if (vPressed) { // Alreay pressed
                    Log.d(TAG, "--------------- Released ---------------");
                    mssgPublisher.onNext(Strs.ACT_RELEASE_PRI); // Send the action

                    // Action duration
                    actionDuration = System.currentTimeMillis() - actionStartTime;

                    // Reset everything
                    vPressed = false;
                    actionStartTime = 0;

                    // TODO log the release
//                    Mologger.get().logMeta(
//                            startPCoords, endPCoords,
//                            pressDuration, duration,
//                            dX, dY,
//                            nExtraFingers, extra1PCoords, extra2PCoords);
                }


            }
        break;
            // Last finger is up
        case MotionEvent.ACTION_UP:
            if (vPressed) { // Alreay pressed?
                Log.d(TAG, "--------------- Released ---------------");
                mssgPublisher.onNext(Strs.ACT_RELEASE_PRI); // Send the action

                // Action duration
                actionDuration = System.currentTimeMillis() - actionStartTime;

                // Reset everything
                vPressed = false;
                actionStartTime = 0;
                leftPointerID = INVALID_POINTER_ID;

                // TODO log the release
//                    Mologger.get().logMeta(
//                            startPCoords, endPCoords,
//                            pressDuration, duration,
//                            dX, dY,
//                            nExtraFingers, extra1PCoords, extra2PCoords);
            }

            // No need to Recalculate the left finger...
            break;

        }

    }

    /**
     * TAP L-CLICK
     * @param me MotionEvent
     */
    public void tapLClick(MotionEvent me) {
        int leftIndex, actionIndex;
        switch (me.getActionMasked()) {
        // Only one finger is on the screen
        case MotionEvent.ACTION_DOWN:
            leftIndex = 0; // The only finger
            leftPointerID = me.getPointerId(leftIndex); // ID
            me.getPointerCoords(leftIndex, startPCoords); // Fill the coords
            actionStartTime = System.currentTimeMillis(); // Set the start TAP time
            break;
        // More fingers are added
        case MotionEvent.ACTION_POINTER_DOWN:
            actionIndex = me.getActionIndex(); // Which pointer is down?
            // If new finger on the left
            if (isLeftMost(me, actionIndex)) {
                leftPointerID =  me.getPointerId(actionIndex); // Set ID
                actionStartTime = System.currentTimeMillis(); // Set the start TAP time
                me.getPointerCoords(actionIndex, startPCoords); // Update the coords
            }
            break;
        // Second, third, ... fingers are up
        case MotionEvent.ACTION_POINTER_UP:
            // Check if the active finger has gone up
            actionIndex = me.getActionIndex();
            if (me.getPointerId(actionIndex) == leftPointerID) { // Leftmost finger is UP
                endPCoords = getPointerCoords(me, actionIndex);
                double dist = distance(startPCoords, endPCoords);
                long duration = System.currentTimeMillis() - actionStartTime;

                Log.d(TAG, "duration = " + duration +
                        " | max = " + Config.TAP_LCLICK_DUR_MAX);
                Log.d(TAG, String.format("distance = %.2f | MAX = %.2f",
                        dist, Config.TAP_LCLICK_DIST_MAX));

                if (dist <= Config.TAP_LCLICK_DIST_MAX && duration < Config.TAP_LCLICK_DUR_MAX) {
                    Log.d(TAG, "--------------- Click ---------------");
                    mssgPublisher.onNext(Strs.ACT_CLICK); // Publilsh the event

                    // log the release
                    dX = endPCoords.x - startPCoords.x;
                    dY = endPCoords.y - startPCoords.y;
//                    Mologger.get().logMeta(
//                            startPCoords, endPCoords,
//                            pressDuration, duration,
//                            dX, dY,
//                            nExtraFingers, extra1PCoords, extra2PCoords);
                }

                // Reset the leftmost ID
                leftPointerID = INVALID_POINTER_ID;
            }
            break;
        // Last finger is up
        case MotionEvent.ACTION_UP:
            // Was it a single-finger TAP?
            if (leftPointerID == 0) {
                actionIndex = me.findPointerIndex(leftPointerID);
                endPCoords = getPointerCoords(me, actionIndex);
                double dist = distance(startPCoords, endPCoords);
                long duration = System.currentTimeMillis() - actionStartTime;

                Log.d(TAG, "duration = " + duration +
                        " | max = " + Config.TAP_LCLICK_DUR_MAX);
                Log.d(TAG, String.format("distance = %.2f | MAX = %.2f",
                        dist, Config.TAP_LCLICK_DIST_MAX));

                if (dist <= Config.TAP_LCLICK_DIST_MAX && duration <= Config.TAP_LCLICK_DUR_MAX) {
                    Log.d(TAG, "------- Click ---------");
                    mssgPublisher.onNext(Strs.ACT_CLICK); // Publilsh the event

                    // log the release
                    dX = endPCoords.x - startPCoords.x;
                    dY = endPCoords.y - startPCoords.y;
//                    Mologger.get().logMeta(
//                            startPCoords, endPCoords,
//                            pressDuration, duration,
//                            dX, dY,
//                            nExtraFingers, extra1PCoords, extra2PCoords);

                }
            }

            // Reset the leftmost ID
            leftPointerID = INVALID_POINTER_ID;

            break;

        }
    }

    public boolean isLeftMost(MotionEvent me, int pointerIndex) {
        boolean result = true;
        for (int pix = 0; pix < me.getPointerCount(); pix++) {
            if (me.getX(pix) < me.getX(pointerIndex)) result = false;
        }

        return result;
    }

    /**
     * Find the index of the leftmost pointer
     * @param me MotionEvent
     * @return Index of the leftmost pointer
     */
    public int findLeftPointerIndex(MotionEvent me) {
        if (me.getPointerCount() == 0) return -1;
        else {
            int leftIndex = 0;
            Log.d(TAG, "pointerCount = " + me.getPointerCount());
            for (int pindex = 1; pindex < me.getPointerCount(); pindex++) {
                Log.d(TAG, "pindex = " + pindex);
                if (me.getX(pindex) < me.getX(leftIndex)) leftIndex = pindex;
            }

            return leftIndex;
        }
    }

    /**
     * Find the ID of the leftmost pointer
     * @param me MotionEvent
     * @return ID of the leftmost pointer (INVALID_POINTER_ID if no pointer on the screen)
     */
    public int findLeftPointID(MotionEvent me) {
        int leftIndex = findLeftPointerIndex(me);
        if (leftIndex == -1) return INVALID_POINTER_ID;
        else return me.getPointerId(leftIndex);
    }

    /**
     * Add a pointer ID to the list + sort the list based on the x
     * @param pointerID int ID
     */
    private void addIDToList(int pointerID) {

    }
    /**
     * Print the indexes and IDs of all the pointers
     * @param me MotionEvent
     */
    private void printPointers(MotionEvent me) {
        for (int pix = 0; pix < me.getPointerCount(); pix++) {
            Log.d(TAG, "Index = " + pix + " | " + "ID = " + me.getPointerId(pix));
        }
        Log.d(TAG, "------------------------------");
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
                if (dY > Config.SWIPE_LCLICK_DY_MIN) {
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
                if (mDist >= Config.SWIPE_LCLICK_DY_MIN) {
                    actionCancelled = true;
                }
            }

            break;

        // TAP starts from UP
        case MotionEvent.ACTION_UP: case MotionEvent.ACTION_POINTER_UP:
            long time = System.currentTimeMillis();
            long dt = time - actionStartTime;
            if (!actionCancelled && dt < Config.TAP_LCLICK_DUR_MAX) { // TAP recognized!
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

    // ===============================================================================
    //region [Tools]

    /**
     * Calculate the Euclidean distance between two coords
     * @param pc1 PointerCoords 1
     * @param pc2 PointerCoords 2
     * @return Double distance
     */
    public double distance(MotionEvent.PointerCoords pc1,
                                  MotionEvent.PointerCoords pc2) {
        return Math.sqrt(Math.pow(pc1.x - pc2.x, 2) + Math.pow(pc1.y - pc2.y, 2));
    }

    /**
     * Truly GET the PointerCoords!
     * @param me MotionEvent
     * @param pointerIndex int pointer index
     * @return PointerCoords
     */
    public PointerCoords getPointerCoords(MotionEvent me, int pointerIndex) {
        PointerCoords result = new PointerCoords();
        me.getPointerCoords(pointerIndex, result);
        return result;
    }

    //endregion
}
