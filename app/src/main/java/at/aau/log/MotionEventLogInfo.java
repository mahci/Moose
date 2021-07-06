package at.aau.log;

import android.view.MotionEvent;

import at.aau.moose.Mologger;

import static at.aau.moose.Strs.SEP;

public class MotionEventLogInfo {
    public int meId;
    public MotionEvent me;

    public MotionEventLogInfo(MotionEvent me, int meId) {
        this.meId = meId;
        this.me = me;
    }

    /**
     * Get the header for the log file
     * @return String - header with the names of the vars
     */
    public static String getLogHeader() {
        return "event_id" + SEP +

                "action" + SEP +

                "flags" + SEP +
                "edge_flags" + SEP +
                "source" + SEP +

                "event_time" + SEP +
                "down_time" + SEP +

                "number_pointers" + SEP +

                "finger_1_index" + SEP +
                "finger_1_id" + SEP +
                "finger_1_orientation" + SEP +
                "finger_1_pressure" + SEP +
                "finger_1_size" + SEP +
                "finger_1_toolMajor" + SEP +
                "finger_1_toolMinor" + SEP +
                "finger_1_touchMajor" + SEP +
                "finger_1_touchMinor" + SEP +
                "finger_1_x" + SEP +
                "finger_1_y" + SEP +

                "finger_2_index" + SEP +
                "finger_2_id" + SEP +
                "finger_2_orientation" + SEP +
                "finger_2_pressure" + SEP +
                "finger_2_size" + SEP +
                "finger_2_toolMajor" + SEP +
                "finger_2_toolMinor" + SEP +
                "finger_2_touchMajor" + SEP +
                "finger_2_touchMinor" + SEP +
                "finger_2_x" + SEP +
                "finger_2_y" + SEP +

                "finger_3_index" + SEP +
                "finger_3_id" + SEP +
                "finger_3_orientation" + SEP +
                "finger_3_pressure" + SEP +
                "finger_3_size" + SEP +
                "finger_3_toolMajor" + SEP +
                "finger_3_toolMinor" + SEP +
                "finger_3_touchMajor" + SEP +
                "finger_3_touchMinor" + SEP +
                "finger_3_x" + SEP +
                "finger_3_y" + SEP +

                "finger_4_index" + SEP +
                "finger_4_id" + SEP +
                "finger_4_orientation" + SEP +
                "finger_4_pressure" + SEP +
                "finger_4_size" + SEP +
                "finger_4_toolMajor" + SEP +
                "finger_4_toolMinor" + SEP +
                "finger_4_touchMajor" + SEP +
                "finger_4_touchMinor" + SEP +
                "finger_4_x" + SEP +
                "finger_4_y" + SEP +

                "finger_5_index" + SEP +
                "finger_5_id" + SEP +
                "finger_5_orientation" + SEP +
                "finger_5_pressure" + SEP +
                "finger_5_size" + SEP +
                "finger_5_toolMajor" + SEP +
                "finger_5_toolMinor" + SEP +
                "finger_5_touchMajor" + SEP +
                "finger_5_touchMinor" + SEP +
                "finger_5_x" + SEP +
                "finger_5_y";
    }

    /**
     * Get the String of this object for logging
     * @return String - ';'-delimited
     */
    public String toLogString() {
        StringBuilder result = new StringBuilder();

        result.append(meId).append(SEP);

        result.append(me.getActionMasked()).append(SEP);

        result.append("0x").append(Integer.toHexString(me.getFlags())).append(SEP);
        result.append("0x").append(Integer.toHexString(me.getEdgeFlags())).append(SEP);
        result.append("0x").append(Integer.toHexString(me.getSource())).append(SEP);

        result.append(me.getEventTime()).append(SEP);
        result.append(me.getDownTime()).append(SEP);

        // Pointers' info (for 0 - (nPointer -1) => real values | for the rest to 5 => dummy)
        int nPointers = me.getPointerCount();
        result.append(nPointers).append(SEP);
        int pi;
        for(pi = 0; pi < nPointers; pi++) {
            result.append(pi).append(SEP); // Index
            result.append(me.getPointerId(pi)).append(SEP); // Id
            // PointerCoords
            result.append(Mologger.get().pointerCoordsToStr(me, pi)).append(SEP);
        }

        for (pi = nPointers; pi < 5; pi++) {
            result.append(-1).append(SEP); // Index = -1
            result.append(-1).append(SEP); // Id = -1
            // PointerCoords = empty
            result.append(Mologger.get().pointerCoordsToStr(new MotionEvent.PointerCoords()))
                    .append(SEP);
        }

        String resStr = result.toString();
        return resStr.substring(0, resStr.length() - 1); // Remove the last SEP
    }
    
}
