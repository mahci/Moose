package at.aau.moose;

import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;

public class Config {

    private static final String TAG = "Moose_Config";
    //=======================================================

    // Server
    public static final String SERVER_IP = "192.168.2.1";
//    public static final String SERVER_IP = "192.168.178.34";
    public static final int SERVER_Port = 8000;
//    public static final int TIMEOUT = 2 * 60 * 1000; // 2 min

    // Thresholds ------------------------------------
    public static final int SWIPE_LCLICK_DY_MIN_MM = 3; // mm
    public static final int TAP_LCLICK_DIST_MAX_MM = 1; // mm
    public static float SWIPE_LCLICK_DY_MIN; // px
    public static float TAP_LCLICK_DIST_MAX; // px

    public static final int TAP_LCLICK_TIMEOUT = 200; // ms

    public static final int PALM_AREA_Y = 1080; // px (from the top)

    // -----------------------------------------------

    // Sizes  ----------------------------------------
//    public static final int TAP_REGION_H_MM = 100; // mm
//    public static float _tapRegionH; // px
    // -----------------------------------------------

    /**
     * Set the pixel equivalent of mm values
     * @param dm DisplayMetrics of the current device
     */
    public static void SetPxValues(DisplayMetrics dm) {
        float multip = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_MM, 1, dm);

        SWIPE_LCLICK_DY_MIN = SWIPE_LCLICK_DY_MIN_MM * multip;
        TAP_LCLICK_DIST_MAX = TAP_LCLICK_DIST_MAX_MM * multip;

//        _tapRegionH = TAP_REGION_H_MM * multip;

        Log.d(TAG, "Constants ============");
        Log.d(TAG, "Min dY = " + SWIPE_LCLICK_DY_MIN + " px");
        Log.d(TAG, "======================");
    }

    /**
     * Given an action int, returns a string description
     * @param action MotionEvent action
     * @return String description of the input action
     */
    public static String actionToString(int action) {
        switch (action) {
            case MotionEvent.ACTION_DOWN: return "ACTION_DOWN";
            case MotionEvent.ACTION_MOVE: return "ACTION_MOVE";
            case MotionEvent.ACTION_POINTER_DOWN: return "ACTION_POINTER_DOWN";
            case MotionEvent.ACTION_UP: return "ACTION_UP";
            case MotionEvent.ACTION_POINTER_UP: return "ACTION_POINTER_UP";
            case MotionEvent.ACTION_OUTSIDE: return "ACTION_OUTSIDE";
            case MotionEvent.ACTION_CANCEL: return "ACTION_CANCEL";
        }
        return "";
    }

    /**
     * Get the String for a PointerCoords object
     * @param pc PointerCoords
     * @return String of pc
     */
    public static String pointerCoordsToString(MotionEvent.PointerCoords pc) {
//        return "coord= (" + pc.x + "," + pc.y + ")" + " - " +
//                "orientation= " + pc.orientation + " - " +
//                "pressure= " + pc.pressure + " - " +
//                "size= " + pc.size + " - " +
//                "touchMajor= " + pc.touchMajor + " - " +
//                "touchMinor= " + pc.touchMinor;

        return "coord= (" + pc.x + "," + pc.y + ")";
    }

}
