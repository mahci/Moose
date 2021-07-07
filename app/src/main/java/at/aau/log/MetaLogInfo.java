package at.aau.log;

import android.view.MotionEvent;

import at.aau.moose.Config;
import at.aau.moose.MainActivity;
import at.aau.moose.Mologger;
import at.aau.moose.Strs;
import at.aau.moose.Utils;

import static android.view.MotionEvent.PointerCoords;
import static at.aau.moose.Strs.SEP;

public class MetaLogInfo {
    public int startMeId; // Start MotionEvent ID
    public PointerCoords startPointerCoords = new PointerCoords();
    public int endMeId; // End MotionEvent ID
    public PointerCoords endPointerCoords = new PointerCoords();
    public float dX;
    public float dY;
    public int duration;
    public int relCan; // Released (0) or cancelled (1)

    /**
     * Set the dX, dY (called after setting coords)
     */
    public void setDs() {
        dX = Config.pxToMM(endPointerCoords.x - startPointerCoords.x);
        dY = Config.pxToMM(endPointerCoords.y - startPointerCoords.y);
    }

    /**
     * Get the header for the log file
     * @return String - header with the names of the vars
     */
    public static String getLogHeader() {

        return "action_start_event_id" + SEP +

                "action_start_orientation" + SEP +
                "action_start_pressure" + SEP +
                "action_start_size" + SEP +
                "action_start_toolMajor" + SEP +
                "action_start_toolMinor" + SEP +
                "action_start_touchMajor" + SEP +
                "action_start_touchMinor" + SEP +
                "action_start_x" + SEP +
                "action_start_y" + SEP +

                "action_end_event_id" + SEP +

                "action_end_orientation" + SEP +
                "action_end_pressure" + SEP +
                "action_end_size" + SEP +
                "action_end_toolMajor" + SEP +
                "action_end_toolMinor" + SEP +
                "action_end_touchMajor" + SEP +
                "action_end_touchMinor" + SEP +
                "action_end_x" + SEP +
                "action_end_y" + SEP +

                "action_dX" + SEP +
                "action_dY" + SEP +

                "action_duration" + SEP +

                "released_cancelled";
    }

    /**
     * Get the String of this object for logging
     * @return String - ';'-delimited
     */
    public String toLogString() {
        return startMeId + SEP +
                Mologger.get().pointerCoordsToStr(startPointerCoords) + SEP +

                endMeId + SEP +
                Mologger.get().pointerCoordsToStr(endPointerCoords) + SEP +

                Utils.double3Dec(dX) + SEP +
                Utils.double3Dec(dY) + SEP +

                duration + SEP +

                relCan;
    }
}
