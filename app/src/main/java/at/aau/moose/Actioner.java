package at.aau.moose;

import android.os.CountDownTimer;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;

import at.aau.log.GeneralLogInfo;
import at.aau.log.MetaLogInfo;
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

    private int leftPointerID = INVALID_POINTER_ID; // Id of the left finger
    private long actionStartTime; // Both for time keeping and checking if action is started

    // Is virtually pressed?
    private boolean vPressed = false;

    // For calling the Networker (can't call directly)
    private PublishSubject<String> mssgPublisher;

    // State
    private STATE _state;

    // Timer for the TAP
    private CountDownTimer tapTimer;

    // Log info
    public GeneralLogInfo mGenLogInfo = new GeneralLogInfo();
    private MetaLogInfo mMetaLogInfo = new MetaLogInfo();

    // [TEST]
    public Vibrator vibrator;

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
        if (mGenLogInfo.technique != null) {
            //--- Process the TOUCH EVENT based on the gesture
            switch (mGenLogInfo.technique) {
            case SWIPE: // SWIPE
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
        float leftDY;

        switch (me.getActionMasked()) {

            // Only one finger is on the screen
        case MotionEvent.ACTION_DOWN:
            leftIndex = 0; // The only finger
            leftPointerID = me.getPointerId(leftIndex); // ID

            // Set the start coords
            mMetaLogInfo.startPointerCoords = getPointerCoords(me, leftIndex);
            mMetaLogInfo.startMeId = meId; // Set the start meID

            break;

            // More fingers are added
        case MotionEvent.ACTION_POINTER_DOWN:
            actionIndex = me.getActionIndex(); // Which pointer is down?

            // If new finger on the left
            if (isLeftMost(me, actionIndex)) {
                leftPointerID =  me.getPointerId(actionIndex); // Set ID

                // Update the start coords
                mMetaLogInfo.startPointerCoords = getPointerCoords(me, actionIndex);
                mMetaLogInfo.startMeId = meId; // Set the start meID
            }

            break;

            // Movement...
        case MotionEvent.ACTION_MOVE:
            if (leftPointerID != INVALID_POINTER_ID) { // We have a leftmost finger
//                printPointers(me); // TEST
                leftIndex = me.findPointerIndex(leftPointerID);
                if (leftIndex != -1) { // ID found

                    // Always calculated the amount of movement of the leftmost finger
                    leftDY = me.getY(leftIndex) - mMetaLogInfo.startPointerCoords.y;

                    Log.d(TAG, String.format("getY = %.2f | startY = %.2f",
                            me.getY(leftIndex), mMetaLogInfo.startPointerCoords.y));
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
                        }
                    }

                } else { // leftmost ID is no longer on the screen => find the next one
                    // Recalculate the left finger (we know last active leftmost is up now)
                    leftIndex = findLeftPointerIndex(me);
                    leftPointerID = me.getPointerId(leftIndex); // Set ID

                    // Set the start coords
                    mMetaLogInfo.startPointerCoords = getPointerCoords(me, leftIndex);
                    mMetaLogInfo.startMeId = meId; // Set the start meId

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

                    // End attribs
                    mMetaLogInfo.duration = (int) (System.currentTimeMillis() - actionStartTime);
                    mMetaLogInfo.endMeId = meId;
                    mMetaLogInfo.endPointerCoords = getPointerCoords(me, actionIndex);
                    mMetaLogInfo.setDs(); // Set dX and dY based on
                    mMetaLogInfo.relCan = 0;

                    // Log the action
                    Mologger.get().logMeta(mMetaLogInfo);

                    // Reset some things!
                    vPressed = false;
                    actionStartTime = 0;
                    mMetaLogInfo.endMeId = -1;
                }


            }

        break;

            // Last finger is up
        case MotionEvent.ACTION_UP:
            if (vPressed) { // Alreay pressed?
                Log.d(TAG, "--------------- Released ---------------");
                mssgPublisher.onNext(ACTION.RELEASE_PRI.name()); // Send the action

                // End attribs
                mMetaLogInfo.duration = (int) (System.currentTimeMillis() - actionStartTime);
                mMetaLogInfo.endMeId = meId;
                mMetaLogInfo.endPointerCoords = getPointerCoords(me, 0);
                mMetaLogInfo.setDs(); // Set dX and dY based on
                mMetaLogInfo.relCan = 0;

                // Log the action
                Mologger.get().logMeta(mMetaLogInfo);

                // Reset everything
                vPressed = false;
                actionStartTime = 0;
                leftPointerID = INVALID_POINTER_ID;
                mMetaLogInfo.startMeId = -1;
                mMetaLogInfo.endMeId = -1;
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
            // Set the start coords
            mMetaLogInfo.startPointerCoords = getPointerCoords(me, leftIndex);
            actionStartTime = System.currentTimeMillis(); // Set the start TAP time

            mMetaLogInfo.startMeId = meId; // Set the start id

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
                // Update the start coords
                me.getPointerCoords(actionIndex, mMetaLogInfo.startPointerCoords);
                mMetaLogInfo.startMeId = meId; // Set the start id

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
                    leftDY = me.getY(leftIndex) - mMetaLogInfo.startPointerCoords.y;

                    Log.d(TAG, String.format("getY = %.2f | startY = %.2f",
                            me.getY(leftIndex), mMetaLogInfo.startPointerCoords.y));
                    Log.d(TAG, String.format("dY = %.2f | MIN = %.2f",
                            leftDY, Config.SWIPE_LCLICK_DY_MIN));
                    // Did it move too much?
                    if (leftDY >= Config.TAP_LCLICK_DIST_MAX) { // SWIPED!
                        vPressed = false; // Flag
                        Log.d(TAG, "--------------- Cancelled by Distance ---------------");
                        mssgPublisher.onNext(ACTION.CANCEL.name()); // Send the action

                        // Log if not already logged
                        if (mMetaLogInfo.startMeId != -1) {
                            // End attribs
                            mMetaLogInfo.duration = (int)
                                    (System.currentTimeMillis() - actionStartTime);
                            mMetaLogInfo.endMeId = meId;
                            mMetaLogInfo.endPointerCoords = getPointerCoords(me, 0);
                            mMetaLogInfo.setDs(); // Set dX and dY based on
                            mMetaLogInfo.relCan = 1; // Cancelled
                            Log.d(TAG, "start id= " + mMetaLogInfo.startMeId);
                            Log.d(TAG, "end id= " + mMetaLogInfo.endMeId);

                            // Log the action
                            Mologger.get().logMeta(mMetaLogInfo);

                            mMetaLogInfo.startMeId = -1; // Reset the start id
                            mMetaLogInfo.endMeId = -1; // Reset the end id

                        }

                    }

                }
            }

            break;

        // Second, third, ... fingers are up
        case MotionEvent.ACTION_POINTER_UP:
            // Check if the active finger has gone up
            actionIndex = me.getActionIndex();
            if (me.getPointerId(actionIndex) == leftPointerID) { // Leftmost finger is UP

                mMetaLogInfo.endPointerCoords = getPointerCoords(me, actionIndex);
                double dist = distance(
                        mMetaLogInfo.startPointerCoords,
                        mMetaLogInfo.endPointerCoords);
                int duration = (int) (System.currentTimeMillis() - actionStartTime);

                Log.d(TAG, "duration = " + duration +
                        " | MAX = " + Config.TAP_LCLICK_TIMEOUT);
                Log.d(TAG, String.format("distance = %.2f | MAX = %.2f",
                        dist, Config.TAP_LCLICK_DIST_MAX));

                if (vPressed) {

                    if (dist <= Config.TAP_LCLICK_DIST_MAX) {
                        Log.d(TAG, "--------------- Released ---------------");
                        mssgPublisher.onNext(Strs.ACT_RELEASE_PRI); // Publilsh the event

                        // Save log info
                        mMetaLogInfo.duration = duration;
                        mMetaLogInfo.endMeId = meId;
                        mMetaLogInfo.setDs();
                        mMetaLogInfo.relCan = 0;

                        // Log
                        Mologger.get().logMeta(mMetaLogInfo);
                    }
                }

                // Reset everything
                leftPointerID = INVALID_POINTER_ID;
                vPressed = false;
                mMetaLogInfo.startMeId = -1;
                mMetaLogInfo.endMeId = -1;
            }
            break;

        // Last finger is up
        case MotionEvent.ACTION_UP:
            // Was it a single-finger TAP?
            if (leftPointerID == 0) {
                actionIndex = me.findPointerIndex(leftPointerID);
                mMetaLogInfo.endPointerCoords = getPointerCoords(me, actionIndex);
                double dist = distance(
                        mMetaLogInfo.startPointerCoords,
                        mMetaLogInfo.endPointerCoords);
                int duration = (int) (System.currentTimeMillis() - actionStartTime);

                Log.d(TAG, "duration = " + duration +
                        " | MAX = " + Config.TAP_LCLICK_TIMEOUT);
                Log.d(TAG, String.format("distance = %.2f | MAX = %.2f",
                        dist, Config.TAP_LCLICK_DIST_MAX));

                if (vPressed) {
                    if (dist <= Config.TAP_LCLICK_DIST_MAX &&
                            duration <= Config.TAP_LCLICK_TIMEOUT) {
                        Log.d(TAG, "--------------- Released ---------------");
                        mssgPublisher.onNext(Strs.ACT_RELEASE_PRI); // Publilsh the event

                        // Save log info
                        mMetaLogInfo.duration = duration;
                        mMetaLogInfo.setDs();
                        mMetaLogInfo.endMeId = meId;

                        // Log
                        Mologger.get().logMeta(mMetaLogInfo);

                    }
                }
            }

            // Reset everything
            leftPointerID = INVALID_POINTER_ID;
            vPressed = false;
            mMetaLogInfo.startMeId = -1;
            mMetaLogInfo.endMeId = -1;

            break;

        }
    }

    /**
     * Timer for the tap duration
     */
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

                    // Save log info
                    mMetaLogInfo.duration = Config.TAP_LCLICK_TIMEOUT;
                    mMetaLogInfo.endMeId = -1;
                    mMetaLogInfo.setDs();
                    mMetaLogInfo.relCan = 1; // Cancelled
                    Log.d(TAG, "start id= " + mMetaLogInfo.startMeId);
                    Log.d(TAG, "end id= " + mMetaLogInfo.endMeId);

                    // Log
                    Mologger.get().logMeta(mMetaLogInfo);
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
            mGenLogInfo.technique = TECHNIQUE.SWIPE;
            break;
        case "TAP":
            mGenLogInfo.technique = TECHNIQUE.TAP;
            break;
        case "MOUSE":
            mGenLogInfo.technique = TECHNIQUE.MOUSE;
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
