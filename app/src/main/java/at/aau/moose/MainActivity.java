package at.aau.moose;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import java.util.Calendar;

import io.reactivex.rxjava3.subjects.PublishSubject;

public class MainActivity extends Activity {

    private final String TAG = "Moose_Main";

    // ------------------------------------------------

    // Publisher for publishing the events to other classes
    private PublishSubject<TouchEvent> eventPublisher;

    // Code for overlay permission intent
    private static final int OVERLAY_PERMISSION_CODE = 2;

    private static TouchState fingersState = new TouchState();

    private static DevicePolicyManager mDPM;
    private static ComponentName adminManager;
    public static boolean adminActive;
    private static boolean statusDisabled;

    private boolean askedForOverlayPermission;

    private boolean initialized;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE); // For removing the status bar [ReStBa]

        super.onCreate(savedInstanceState);
        Log.d(TAG, "Initialize? " + !initialized);
        // Initialize only once
        if (!initialized) {
            init();
            // Done!
            Log.d(TAG, "Initization finished!");

        }

        // Set the content of the activity
//        setContentView(R.layout.activity_main);
    }

    /**
     * Initialize
     */
    private void init() {

        initialized = true;

        // Pass the DisplayMetrics to Const to convert values
        Const.SetPxValues(getResources().getDisplayMetrics());

        // Get the admin permission [for ReStBa]
        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminManager = new ComponentName(this, AdminManager.class);
        adminActive = mDPM.isAdminActive(adminManager);
        if (!adminActive) {
            Log.d(TAG, "[Admin] Disabled => open request intent");
            // Launch the activity to have the user enable our admin.
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminManager);
            startActivityForResult(intent, 1);
        } else {
            Log.d(TAG, "[Admin] Enabled");
        }

        // Get the overlay permission (possible only with admin)
        if (!Settings.canDrawOverlays(this)) {
            askedForOverlayPermission = true;
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_CODE);
        } else {
            drawViewGroup();
        }

        // Create the Publisher
        eventPublisher = PublishSubject.create();

        // Subscribe the classes to receive actions
        Actioner.get().subscribeToEvents(eventPublisher);
        Mologger.get().subscribeToEvents(eventPublisher);

        // Connect to the Empenvi
        Networker.get().connect();
    }

    /**
     *  Return from other intents
     * @param requestCode (int) request code
     * @param resultCode (int) restult code
     * @param data Additionaly data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_CODE) {
            askedForOverlayPermission = false;
            if (Settings.canDrawOverlays(this)) {
                drawViewGroup();
            }
        }
    }

    /**
     * Draw the custom view (to apear under the status bar)
     */
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
        params.height = (int) (Const.TAP_REGION_H);

        TouchViewGroup view = new TouchViewGroup(this);

        view.setBackgroundColor(Color.WHITE);
        assert winManager != null;
        winManager.addView(view, params);
    }

    /**
     * The experiment begins
     */
    public static void beginExperiment() {
        // Save the state at this time
        fingersState.setTime(Calendar.getInstance().getTimeInMillis());
//        Actioner.get().setInitState(fingersState);
    }

    /**
     * Return a TouchEvent with the MotionEvent
     * @param me MotionEvent
     */
    private void publishEvent(MotionEvent me) {
//        setFingers(me); // Set the state of fingers
        TouchEvent te = new TouchEvent(
                me,
                Calendar.getInstance().getTimeInMillis());
        te.setDestate(fingersState);

        // Publish!
        eventPublisher.onNext(te);
    }


    @Override
    public void onBackPressed() {
        // Intentionally empty to disable the BACK button
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

    /**
     * Custom view class
     */
    private class TouchViewGroup extends ViewGroup {

        public TouchViewGroup(Context context) {
            super(context);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {

        }

        /**
         * Intercept the touches on the view
         * @param ev MotionEvent
         * @return Always true (to pass the events to children)
         */
        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            // TODO: Only redraw if on status bar
            // Redraw the layout
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
//            recreate();
//            finish();
            startActivity(getIntent());

            return false;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {

            // Publish the event (mostly for Actioner)
            publishEvent(event);

            // Log the action
//            Mologger.get().log(event);

            return super.onTouchEvent(event);
        }
    }




}