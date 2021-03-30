package at.aau.moose;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import at.aau.moose.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.reactivex.rxjava3.subjects.PublishSubject;

public class MainActivity extends Activity {

    private final String TAG = "MooseMainActivity";

    private PublishSubject<TouchEvent> eventPublisher;


    private int ptr1ID, ptr2ID;
    private int lind, rind;
    private float ptr0X, ptr1X, ptr1Y, ptr2X, ptr2Y;
    private float dx = 10; // Min px distance between the two fingers on the screen

    private List<MotionEvent.PointerCoords> leftMoveList = new ArrayList<>();
    private List<MotionEvent.PointerCoords> rightMoveList = new ArrayList<>();

    private long actStartTime;
    private long actEndTime;

    private static TouchState fingersState = new TouchState();

    private static DevicePolicyManager mDPM;
    private static ComponentName adminManager;
    public static boolean adminActive;
    private static boolean statusDisabled;

    private int OVERLAY_PERMISSION_CODE = 2;
    private boolean askedForOverlayPermission;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature( Window.FEATURE_NO_TITLE );
        super.onCreate(savedInstanceState);

        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminManager = new ComponentName(this, AdminManager.class);

        adminActive = mDPM.isAdminActive(adminManager);

        if (!adminActive) {
            Log.d(TAG, "Open Intent");
            // Launch the activity to have the user enable our admin.
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminManager);
            startActivityForResult(intent, 1);
        } else {
            Log.d(TAG, "Already admin");
//            disableStatusbar();
        }

//        showAdminRequest();


        if (!Settings.canDrawOverlays(this)) {
            askedForOverlayPermission = true;
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_CODE);
        } else {
            drawViewGroup();
        }



        // Hide the status bar
//        View decorView = getWindow().getDecorView();
//        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
//        decorView.setSystemUiVisibility(uiOptions);

        // Create the actSubject
        eventPublisher = PublishSubject.create();

        // Start Networker
//        Networker.get().subscribeToActions(eventPublisher);
        Actioner.get().subscribeToEvents(eventPublisher);


        // Set up tapping and storing taps
//        View tapRegion = findViewById(R.id.tapRegion);
//        tapRegion.setOnTouchListener(touchListener);
//        detector = new GestureDetector(this, this);

        // Pass the DisplayMetrics to Const to convert values
        Const.SetPxValues(getResources().getDisplayMetrics());

        setContentView(R.layout.activity_main);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2) {
            askedForOverlayPermission = false;
            if (Settings.canDrawOverlays(this)) {
                drawViewGroup();
            }
        }
    }

    public void drawViewGroup() {
        WindowManager winManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP;
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = (int) (50 * getResources()
                .getDisplayMetrics().scaledDensity);
//        params.format = PixelFormat.TRANSPARENT;

        TouchViewGroup view = new TouchViewGroup(this);

        assert winManager != null;
        winManager.addView(view, params);
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
////        if (detector.onTouchEvent(event)){
////            return true;
////        }
////        return super.onTouchEvent(event);
//        return true;
//    }

    /**
     * The experiment begins
     */
    public static void beginExperiment() {
        // Save the state at this time
        fingersState.setTime(Calendar.getInstance().getTimeInMillis());
        Actioner.get().setInitState(fingersState);
    }

    /**
     * Handles the touch on the tap region
     */
    private final View.OnTouchListener touchListener = new View.OnTouchListener() {

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // Keep track of the fingers on the screen
//            recordState(event);
//            recordEvent(event);
            publishEvent(event);

            int maskedAction = event.getActionMasked();
            float p0x, p1x;
            switch(maskedAction) {

                case MotionEvent.ACTION_DOWN: // First touch
//                    Log.d(TAG, "ACTION_DOWN -- id = " +
//                            event.getPointerId(0));
                    Log.d(TAG, "ACTION_DOWN ---");
                    break;

                case MotionEvent.ACTION_UP:
//                    Log.d(TAG, "ACTION_UP -- id = " +
//                            event.getPointerId(0));
                    Log.d(TAG, "ACTION_UP ---");
                    break;

                case MotionEvent.ACTION_POINTER_DOWN: // Second touch -> start of gestures
                    // Gesture started
                    actStartTime = event.getEventTime(); // TODO: Check the time

                    int pointerIndex =
                            (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                                    >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
//                    final int pointerIndexDown = event.getActionIndex();
                    Log.d(TAG, "POINTER_DOWN --- ix = " + pointerIndex +
                            " | id = " + event.getPointerId(pointerIndex));
//                    for (int i = 0; i < event.getPointerCount(); i++) {
//                        Log.d(TAG, "i = " + i + " - " + "pid = " + event.getPointerId(i));
//                    }
                    // Determine the left and right indexes
                    if (event.getPointerCount() == 2) {

                        p0x = event.getX(0);
                        p1x = event.getX(1);
                        if (p0x < p1x + dx) {
                            lind = 0;
                            rind = 1;
                        } else {
                            lind = 1;
                            rind = 0;
                        }

                        // ---- Publish the secod touch (index = 1)
//                        MotionEvent.PointerCoords pCoords = new MotionEvent.PointerCoords();
//                        Constants.FINGER finger;
//                        if (lind == 1) { // Left
//                            event.getPointerCoords(lind, pCoords);
//                            finger = Constants.FINGER.LEFT;
//                        } else { // Right
//                            event.getPointerCoords(rind, pCoords);
//                            finger = Constants.FINGER.RIGHT;
//                        }
//                        TouchEvent te = new TouchEvent(
//                                Constants.ACT.PRESS, pCoords,
//                                finger, event.getEventTime());

//                        actSubject.onNext(te);

//                        Actioner.get().PointerDown(
//                                Actioner.FINGER.LEFT,
//                                pCoords, event.getEventTime());
//                        event.getPointerCoords(rind, pCoords); // fill the coords
//                        Actioner.get().PointerDown(
//                                Actioner.FINGER.RIGHT,
//                                pCoords, event.getEventTime());
                    }


                    break;

                case MotionEvent.ACTION_POINTER_UP: // Gesture is finished
                    pointerIndex =
                            (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                                    >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
//                    final int pointerIndexUp = event.getActionIndex();
                    Log.d(TAG, "POINTER_UP --- ix = " + pointerIndex +
                            " | id = " + event.getPointerId(pointerIndex));
//                    }
                    // Gesture ended
//                    actEndTime = event.getEventTime();
//                    long gesDuration = actEndTime - actStartTime;
//                    Log.d(TAG, "Duration (ms) = " + gesDuration);
//                    actStartTime = actEndTime = 0;

                    // Send the info to the Actioner for analysis
//                    Actioner.get().Action(leftMoveList, rightMoveList, gesDuration);

                    // ---- Publish the secod touch (index = 1)
//                    MotionEvent.PointerCoords pCoords = new MotionEvent.PointerCoords();
//                    Constants.FINGER finger;
//                    if (lind == 1) { // Left
//                        event.getPointerCoords(lind, pCoords);
//                        finger = Constants.FINGER.LEFT;
//                    } else { // Right
//                        event.getPointerCoords(rind, pCoords);
//                        finger = Constants.FINGER.RIGHT;
//                    }
//                    TouchEvent te = new TouchEvent(
//                            Constants.ACT.RELEASE, pCoords,
//                            finger, event.getEventTime());
//
//                    actSubject.onNext(te);

                    // Print the list of movement
//                    String out = "{";
//                    for (int e = 0; e < leftMoveList.size(); e++) {
//                        out += pointerCoordsToString(leftMoveList.get(e)) + "\n";
//                    }
//                    Log.d(TAG, "Left List: " + out);
//                    out = "{";
//                    for (int e = 0; e < rightMoveList.size(); e++) {
//                        out += pointerCoordsToString(rightMoveList.get(e)) + "\n";
//                    }
//                    Log.d(TAG, "Right List: " + out);

                    // Clear the lists for the next gesture
//                    leftMoveList.clear();
//                    rightMoveList.clear();

                    break;
                case MotionEvent.ACTION_MOVE: // Gesture
//                    if (event.getPointerCount() == 2) { // Accept only two fingers
//                        int hs = event.getHistorySize();
////                        Log.d(TAG, "History size= " + hs);
//                        for (int h = 0; h < hs; h++) { // add the history coords
//                            MotionEvent.PointerCoords lpc = new MotionEvent.PointerCoords();
//                            event.getHistoricalPointerCoords(lind, h, lpc);
//                            leftMoveList.add(lpc);
//
//                            MotionEvent.PointerCoords rpc = new MotionEvent.PointerCoords();
//                            event.getHistoricalPointerCoords(rind, h, rpc);
//                            rightMoveList.add(rpc);
//                        }
//
//                        // add the current coords
//                        MotionEvent.PointerCoords lpc = new MotionEvent.PointerCoords();
//                        event.getPointerCoords(lind, lpc);
//                        leftMoveList.add(lpc);
//
//                        MotionEvent.PointerCoords rpc = new MotionEvent.PointerCoords();
//                        event.getPointerCoords(rind, rpc);
//                        rightMoveList.add(rpc);
//
//                        break;
//
//                    }

            }

            // Publish the action
//            actSubject.onNext(Constants.ACT_CLICK);

            // Log the action
            Mologger.get().log(event);

            return true; // Necessary for accepting more pointers
        }

        /**
         * Set the fingers in fingersState
         * @param me MotionEvent
         */
        private void setFingers(MotionEvent me) {
            fingersState.reset();
            for (int i = 0; i < me.getPointerCount(); i++) {
                MotionEvent.PointerCoords poco = new MotionEvent.PointerCoords();
                me.getPointerCoords(i, poco);
                fingersState.addFingerPoCo(poco);
            }
        }

        /**
         * Stamp the current state with time (millis) and send to Actioner
         * @param me MotionEvent
         */
        private void recordState(MotionEvent me) {
            // Create the state
            fingersState.reset();
            for (int i = 0; i < me.getPointerCount(); i++) {
                MotionEvent.PointerCoords poco = new MotionEvent.PointerCoords();
                me.getPointerCoords(i, poco);
                fingersState.addFingerPoCo(poco);
            }

            // Add the state to the Actioner
            fingersState.setTime(Calendar.getInstance().getTimeInMillis());
            Actioner.get().addState(fingersState);
        }

        /**
         * Stamp the event with time (millis) and send to Actioner
         * @param me MotionEvent
         */
        private void recordEvent(MotionEvent me) {
            TouchEvent te = new TouchEvent(
                    me,
                    Calendar.getInstance().getTimeInMillis());
            Actioner.get().addEvent(te);

        }

        private void publishEvent(MotionEvent me) {
            setFingers(me); // Set the state of fingers
            TouchEvent te = new TouchEvent(
                    me,
                    Calendar.getInstance().getTimeInMillis());
            te.setDestate(fingersState);

            // Publish!
            eventPublisher.onNext(te);
        }

    };

    public void disableStatusbar() {
        mDPM.setStatusBarDisabled(adminManager, true);
//        statusDisabled = true;
//        Intent intent = getIntent();
//        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
//        finish();
//        startActivity(intent);
    }


    @Override
    public void onBackPressed() {
        // Empty to disable the BACK button
    }

    @Override
    protected void onPause() {
        super.onPause();
        ActivityManager activityManager =
                (ActivityManager) getApplicationContext()
                .getSystemService(Context.ACTIVITY_SERVICE);

        assert activityManager != null;
        activityManager.moveTaskToFront(
                getTaskId(),
                ActivityManager.MOVE_TASK_NO_USER_ACTION);


    }

    private class TouchViewGroup extends ViewGroup {

        public TouchViewGroup(Context context) {
            super(context);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {

        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            Log.d(TAG, "Intercepted...");
//            return super.onInterceptTouchEvent(ev);

//            requestWindowFeature( Window.FEATURE_NO_TITLE );
            getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN );

            recreate();
//            setContentView(R.layout.activity_main);
            return true;
        }
    }




}