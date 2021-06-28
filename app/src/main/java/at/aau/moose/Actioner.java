package at.aau.moose;

import android.os.CountDownTimer;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;

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
    private int actionID = -1; // A unique id for each action in a trial
    private Foint lmFingerDownPos = new Foint();
    private Foint tlFingerPos = new Foint();
    private int leftPointerID = INVALID_POINTER_ID;
    private int upPointerIndex;
    private PointerCoords startPCoords = new PointerCoords();
    private int startMeId = -1;
    private PointerCoords endPCoords = new PointerCoords();
    private int endtMeId = -1;
    private long actionStartTime; // Both for time keeping and checking if action is started
//    private long pressDuration;
//    private long duration;
    private float dX, dY;
    private int nExtraFingers;
    private PointerCoords extra1PCoords = new PointerCoords();
    private PointerCoords extra2PCoords = new PointerCoords();

    // Keeping the coords all the time!
    private List<PointerCoords> pointerCoordsList = new ArrayList<>();

    private List<Integer> sortedIDList = new ArrayList<>();

    // Is virtually pressed?
    private boolean vPressed = false;
    private boolean actionCancelled;

    // [TEST]
    public Vibrator vibrator;

    // For calling the Networker (can't call directly)
    private PublishSubject<String> mssgPublisher;

    // State
    private STATE _state;

    // Timer for the TAP
    private CountDownTimer tapTimer;

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

    /**
     * Constructor
     */
    public Actioner() {
        mssgPublisher = PublishSubject.create();
        Networker.get().subscribeToMessages(mssgPublisher);
    }

    /**
     * Process the given MotionEvent
     * @param me MotionEvent
     * @param meId int - unique id for this event
     */
    public void processEvent(MotionEvent me, int meId) {
        //--- Process the TOUCH EVENT based on the gesture
        if (_technique != null) {
            switch (_technique) {
            case SWIPE:
                swipeLClick(me, meId);
                break;
            case TAP:
                tapLClick(me, meId);
                break;
            }
        }
    }

    /**
     * SWIPE L-CLICK
     * @param me MotionEvent
     * @param meId int - unique id for this event
     */
    public void swipeLClick(MotionEvent me, int meId) {
        int leftIndex, actionIndex;
        int duration = 0;
        float leftDY;

        switch (me.getActionMasked()) {
            // Only one finger is on the screen
        case MotionEvent.ACTION_DOWN:
            leftIndex = 0; // The only finger
            leftPointerID = me.getPointerId(leftIndex); // ID
            me.getPointerCoords(leftIndex, startPCoords); // Set the start coords
            startMeId = meId; // Set the start meID
            break;

            // More fingers are added
        case MotionEvent.ACTION_POINTER_DOWN:
            actionIndex = me.getActionIndex(); // Which pointer is down?
            // If new finger on the left
            if (isLeftMost(me, actionIndex)) {
                leftPointerID =  me.getPointerId(actionIndex); // Set ID
                me.getPointerCoords(actionIndex, startPCoords); // Update the coords
                startMeId = meId; // Set the start meID
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

                    Log.d(TAG, String.format("getY = %.2f | startY = %.2f",
                            me.getY(leftIndex), startPCoords.y));
                    Log.d(TAG, String.format("dY = %.2f | MIN = %.2f",
                            leftDY, Config.SWIPE_LCLICK_DY_MIN));
                    if (!vPressed) { // NOT pressed
                        // Set the start of the movement time
                        if (actionStartTime == 0) actionStartTime = System.currentTimeMillis();

                        // Did it move enough?
                        if (leftDY > Config.SWIPE_LCLICK_DY_MIN) { // SWIPED!
                            vPressed = true; // Flag
                            Log.d(TAG, "--------------- Pressed ---------------");
                            mssgPublisher.onNext(ACTION.PRESS_PRI.name()); // Send the action

                            actionID++; // Assign an id to this action
                        }
                    }

                } else { // leftmost ID is no longer on the screen => find the new one
                    // Recalculate the left finger (we know last active leftmost is up now)
                    leftIndex = findLeftPointerIndex(me);
                    leftPointerID = me.getPointerId(leftIndex); // Set ID
                    me.getPointerCoords(leftIndex, startPCoords); // Set the start coords
                    startMeId = meId; // Set the start meId

                    actionStartTime = 0; // Reset the time
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
                    mssgPublisher.onNext(ACTION.RELEASE_PRI.name()); // Send the action

                    // Action duration
                    duration = (int) (System.currentTimeMillis() - actionStartTime);

                    endtMeId = meId; // Set the end meID
                    // Log the action
                    Mologger.get().logMeta(
                            startMeId, startPCoords,
                            endtMeId, endPCoords,
                            dX, dY, duration);

                    // Reset some things!
                    vPressed = false;
                    actionStartTime = 0;
                    endtMeId = -1;
                }


            }
        break;
            // Last finger is up
        case MotionEvent.ACTION_UP:
            if (vPressed) { // Alreay pressed?
                Log.d(TAG, "--------------- Released ---------------");
                mssgPublisher.onNext(ACTION.RELEASE_PRI.name()); // Send the action

                // Action duration
                duration = (int) (System.currentTimeMillis() - actionStartTime);

                endtMeId = meId; // Set the end meID

                // Log the action
                Mologger.get().logMeta(
                        startMeId, startPCoords,
                        endtMeId, endPCoords,
                        dX, dY, duration);

                // Reset everything
                vPressed = false;
                actionStartTime = 0;
                leftPointerID = INVALID_POINTER_ID;
                startMeId = -1;
                endtMeId = -1;
            }

            // No need to Recalculate the left finger...
            break;

        }

    }

    /**
     * TAP L-CLICK
     * @param me MotionEvent
     * @param meId int - unique id for this event
     */
    public void tapLClick(MotionEvent me, int meId) {
        int leftIndex, actionIndex;
        float leftDY;

        switch (me.getActionMasked()) {
        // Only one finger is on the screen
        case MotionEvent.ACTION_DOWN:
            leftIndex = 0; // The only finger
            leftPointerID = me.getPointerId(leftIndex); // ID
            me.getPointerCoords(leftIndex, startPCoords); // Set the start coords
            actionStartTime = System.currentTimeMillis(); // Set the start TAP time
            startMeId = meId; // Set the start id

            // Pressed
            vPressed = true; // Flag
            mssgPublisher.onNext(ACTION.PRESS_PRI.name()); // Send the action
            Log.d(TAG, "--------------- Pressed ---------------");
            startTapTimer();

            break;

        // More fingers are added
        case MotionEvent.ACTION_POINTER_DOWN:
            actionIndex = me.getActionIndex(); // Which pointer is down?
            // If new finger on the left
            if (isLeftMost(me, actionIndex)) {
                leftPointerID =  me.getPointerId(actionIndex); // Set ID
                actionStartTime = System.currentTimeMillis(); // Set the start TAP time
                me.getPointerCoords(actionIndex, startPCoords); // Update the coords
                startMeId = meId; // Set the start id

                // Pressed
                vPressed = true; // Flag
                mssgPublisher.onNext(ACTION.PRESS_PRI.name()); // Send the action
                Log.d(TAG, "--------------- Pressed ---------------");
                startTapTimer();

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

                    Log.d(TAG, String.format("getY = %.2f | startY = %.2f",
                            me.getY(leftIndex), startPCoords.y));
                    Log.d(TAG, String.format("dY = %.2f | MIN = %.2f",
                            leftDY, Config.SWIPE_LCLICK_DY_MIN));
                    // Did it move too much?
                    if (leftDY >= Config.TAP_LCLICK_DIST_MAX) { // SWIPED!
                        vPressed = false; // Flag
                        Log.d(TAG, "--------------- Cancelled by Distance ---------------");
                        mssgPublisher.onNext(ACTION.CANCEL.name()); // Send the action

                        startMeId = -1; // Reset the start id
                    }

                }
            }

            break;

        // Second, third, ... fingers are up
        case MotionEvent.ACTION_POINTER_UP:
            // Check if the active finger has gone up
            actionIndex = me.getActionIndex();
            if (me.getPointerId(actionIndex) == leftPointerID) { // Leftmost finger is UP
                endPCoords = getPointerCoords(me, actionIndex);
                double dist = distance(startPCoords, endPCoords);
                int duration = (int) (System.currentTimeMillis() - actionStartTime);

                Log.d(TAG, "duration = " + duration +
                        " | MAX = " + Config.TAP_LCLICK_TIMEOUT);
                Log.d(TAG, String.format("distance = %.2f | MAX = %.2f",
                        dist, Config.TAP_LCLICK_DIST_MAX));

                if (vPressed) {
                    if (dist <= Config.TAP_LCLICK_DIST_MAX) {
                        Log.d(TAG, "--------------- Released ---------------");
                        mssgPublisher.onNext(Strs.ACT_RELEASE_PRI); // Publilsh the event

                        // Movement
                        dX = endPCoords.x - startPCoords.x;
                        dY = endPCoords.y - startPCoords.y;

                        actionID++; // Assign an id to this action

                        endtMeId = meId; // Set the end id
                        // Log
                        Mologger.get().logMeta(
                                startMeId, startPCoords,
                                endtMeId, endPCoords,
                                dX, dY,
                                duration);
                    }
                }

                // Reset everything
                leftPointerID = INVALID_POINTER_ID;
                vPressed = false;
                startMeId = -1;
                endtMeId = -1;
            }
            break;

        // Last finger is up
        case MotionEvent.ACTION_UP:
            // Was it a single-finger TAP?
            if (leftPointerID == 0) {
                actionIndex = me.findPointerIndex(leftPointerID);
                endPCoords = getPointerCoords(me, actionIndex);
                double dist = distance(startPCoords, endPCoords);
                int duration = (int) (System.currentTimeMillis() - actionStartTime);

                Log.d(TAG, "duration = " + duration +
                        " | MAX = " + Config.TAP_LCLICK_TIMEOUT);
                Log.d(TAG, String.format("distance = %.2f | MAX = %.2f",
                        dist, Config.TAP_LCLICK_DIST_MAX));

                if (vPressed) {
                    if (dist <= Config.TAP_LCLICK_DIST_MAX && duration <= Config.TAP_LCLICK_TIMEOUT) {
                        Log.d(TAG, "--------------- Released ---------------");
                        mssgPublisher.onNext(Strs.ACT_RELEASE_PRI); // Publilsh the event

                        // Movement
                        dX = endPCoords.x - startPCoords.x;
                        dY = endPCoords.y - startPCoords.y;

                        actionID++; // Assign an id to this action

                        endtMeId = meId; // Set the end id
                        // Log
                        Mologger.get().logMeta(
                                startMeId, startPCoords,
                                endtMeId, endPCoords,
                                dX, dY,
                                duration);

                    }
                }
            }

            // Reset everything
            leftPointerID = INVALID_POINTER_ID;
            vPressed = false;
            startMeId = -1;
            endtMeId = -1;

            break;

        }
    }

    private void startTapTimer() {
        tapTimer = new CountDownTimer(Config.TAP_LCLICK_TIMEOUT, 100) {
            @Override
            public void onTick(long millisUntilFinished) {}

            @Override
            public void onFinish() {
                if (vPressed) {
                    Log.d(TAG, "--------------- Cancelled by Time ---------------");
                    mssgPublisher.onNext(Strs.ACT_CANCEL); // Send a CANCEL message
                    vPressed = false;
                }
            }
        }.start();
    }

    /**
     * Check if a finger is leftmost
     * @param me MortionEvent
     * @param pointerIndex int
     * @return boolean
     */
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
            for (int pindex = 1; pindex < me.getPointerCount(); pindex++) {
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
     * Log the state of the pointers
     */
    private void logPointers(MotionEvent me, int leftPointerID) {
        pointerCoordsList = new ArrayList<>();

        // First, add the left finger
        int leftIndex = me.findPointerIndex(leftPointerID);
        if (leftIndex > -1) {
            PointerCoords leftCoords = getPointerCoords(me, leftIndex);
            pointerCoordsList.add(leftCoords);

            // Add other fingers
            for (int pix = 0; pix < me.getPointerCount(); pix++) {
                getPointerCoords(me, pix);
            }
        }



        // Pass the list with the time and state to the Mologger
        Mologger.get().logCoords(_state, pointerCoordsList, System.currentTimeMillis());
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
