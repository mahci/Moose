package at.aau.moose;

import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;

public class Const {

    private static final String TAG = "Moose_Const";

    // Server
    public static final String SERVER_IP = "143.205.114.167";
    public static final int SERVER_Port = 5000;

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

    // Actions
    public static final String ACT_CLICK        = "CLICK";
    public static final String ACT_PRESS_PRI    = "PRESS_PRI";
    public static final String ACT_RELEASE_PRI  = "RELEASE_PRI";
    public static final String ACT_PRESS_SEC    = "PRESS_SEC";
    public static final String ACT_RELEASE_SEC  = "RELEASE_SEC";

    // Network
    public static final String NET_CONNECT      = "CONNECT";
    public static final String NET_DISCONNECT   = "DISCONNECT";

    // Touches
    public enum FINGER {
        LEFT,
        RIGHT
    }

    public enum ACT {
        PRESS,
        RELEASE,
        CANCEL
    }

    // Thresholds ------------------------------------
    public static final int PRESS_DY_MIN_MM = 6; // mm
    public static final int PRESS_DX_MAX_MM = 5; // mm
    public static float PRESS_DY_MIN_PX; // px
    public static float PRESS_DX_MAX_PX; // px

    public static final int TAP_DUR = 300; // ms

    // -----------------------------------------------

    // Sizes  ----------------------------------------
    public static final int TAP_REGION_H_MM = 70; // mm
    public static float TAP_REGION_H; // px
    // -----------------------------------------------

    /**
     * Set the pixel equivalent of mm values
     * @param dm DisplayMetrics of the current device
     */
    public static void SetPxValues(DisplayMetrics dm) {
        float multip = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_MM, 1, dm);

        PRESS_DY_MIN_PX = PRESS_DY_MIN_MM * multip;
        PRESS_DX_MAX_PX = PRESS_DX_MAX_MM * multip;

        TAP_REGION_H = TAP_REGION_H_MM * multip;

        Log.d(TAG, "Constants ============");
        Log.d(TAG, "Min dY = " + PRESS_DY_MIN_PX + " px");
        Log.d(TAG, "Max dX = " + PRESS_DX_MAX_PX + " px");
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
