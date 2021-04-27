package at.aau.moose;

import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;

public class Config {

    private static final String TAG = "Moose_Config";
    //=======================================================

    // Server
//    public static final String SERVER_IP = "192.168.2.1";
    public static final String SERVER_IP = "192.168.178.34";
    public static final int SERVER_Port = 8000;
    public static final int TIMEOUT = 2 * 60 * 1000; // 2 min

    // Messages
    public static final String MSSG_MOOSE       = "MOOSE";
    public static final String MSSG_CONFIRM     = "CONFIRM";
    public static final String MSSG_PID         = "PID";
    public static final String MSSG_BEG_EXP     = "BEGEXP";
    public static final String MSSG_END_EXP     = "ENDEXP";
    public static final String MSSG_BEG_BLK     = "BEGBLK";
    public static final String MSSG_END_BLK     = "ENDBLK";
    public static final String MSSG_END_TRL     = "ENDTRL";
    public static final String MSSG_END_LOG     = "ENDLOG";
    public static final String MSSG_BEG_LOG     = "BEGLOG";
    public static final String MSSG_ACK         = "ACK";

    // Actions
    public static final String ACT_CLICK        = "CLICK";
    public static final String ACT_PRESS_PRI    = "PRESS_PRI";
    public static final String ACT_RELEASE_PRI  = "RELEASE_PRI";
    public static final String ACT_PRESS_SEC    = "PRESS_SEC";
    public static final String ACT_RELEASE_SEC  = "RELEASE_SEC";

    // Network
    public static final String NET_CONNECT      = "CONNECT";
    public static final String NET_DISCONNECT   = "DISCONNECT";

    // Thresholds ------------------------------------
    public static final int SWIPE_LCLICK_DY_MIN_MM = 5; // mm
    public static final int SWIPE_LCLICK_DX_MAX_MM = 3; // mm
    public static float _swipeLClickDyMin; // px
    public static float _swipeLClickDxMax; // px

    public static final int TAP_DUR = 300; // ms

    // -----------------------------------------------

    // Sizes  ----------------------------------------
    public static final int TAP_REGION_H_MM = 70; // mm
    public static float _tapRegionH; // px
    // -----------------------------------------------

    /**
     * Set the pixel equivalent of mm values
     * @param dm DisplayMetrics of the current device
     */
    public static void SetPxValues(DisplayMetrics dm) {
        float multip = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_MM, 1, dm);

        _swipeLClickDyMin = SWIPE_LCLICK_DY_MIN_MM * multip;
        _swipeLClickDxMax = SWIPE_LCLICK_DX_MAX_MM * multip;

        _tapRegionH = TAP_REGION_H_MM * multip;

        Log.d(TAG, "Constants ============");
        Log.d(TAG, "Min dY = " + _swipeLClickDyMin + " px");
        Log.d(TAG, "Max dX = " + _swipeLClickDxMax + " px");
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
