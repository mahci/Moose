package at.aau.moose;

import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import static android.view.MotionEvent.PointerCoords;

/***
 * Responsible for logging the events
 */
public class Mologger {

    private final String TAG = "Moose_Mologger";
    //--------------------------------------------

    private static Mologger self; // for singleton

    // Logging is active or not
    public boolean toLog = true;

    // Naming
    private static String logDir; // Top directory for all the logs

    private static final String PI = "P"; // Participant indicator
    private static final String SEP = ";"; // Separator

    // Values
    private String metaLogFilePath;
    private String coordsLogFilePath;
    private String allLogFilePath;

    private PrintWriter metaLogFile;
    private PrintWriter coordsLogFile;
    private PrintWriter allLogFile;

    private int _pid = -1; // Participant ID (for keeping logged)
    public int _phase;
    public int _subblockNum;
    public int _trialNum;

    // ===============================================================================

    /**
     * Get the instance
     * @return single instance
     */
    public static Mologger get() {
        if (self == null) {
            self = new Mologger();
        }

        return self;
    }

    /**
     * Constructor
     */
    public Mologger() {
        // Create the log dir (if not existed)
        logDir = Environment.getExternalStorageDirectory() + "/Moose/";
        createDir(logDir).output(this.getClass().getName());
    }

    /**
     * Log the start of a participant
     * @param pid participant's id
     * @return STATUS
     */
    public STATUS loginParticipant(String pid) {
        try {
            metaLogFilePath = logDir +
                    PI + pid + "_" +
                    Utils.nowDateTime() + "_" +
                    "META.txt";

            coordsLogFilePath = logDir +
                    PI + pid + "_" +
                    Utils.nowDateTime() + "_" +
                    "COORDS.txt";

            allLogFilePath = logDir +
                    PI + pid + "_" +
                    Utils.nowDateTime() + "_" +
                    "ALL.txt";

            // Open meta file and write the column headers
            metaLogFile = new PrintWriter(new FileWriter(metaLogFilePath, true));
            metaLogFile.println(metaLogHeader());
            metaLogFile.flush();

            // Open meta file and write the column headers
            coordsLogFile = new PrintWriter(new FileWriter(coordsLogFilePath, true));
            coordsLogFile.println(coordsHeader());
            coordsLogFile.flush();

            // Open all file and write the column headers
            allLogFile = new PrintWriter(new FileWriter(allLogFilePath, true));
            allLogFile.println(allLogHeader());
            allLogFile.flush();

            Log.d(TAG, "Log files created");

            return STATUS.SUCCESS;
        } catch (IOException e) {
            Log.d(TAG, "Error in creating participant files!");
            e.printStackTrace();
            return STATUS.ERR_FILES_CREATION;
        }
    }

    /**
     * Log a MotionEvent (in ALL)
     * @param me MotionEvent
     * @param meID int - unique id of the MotionEvent
     * @return STATUS
     */
    public STATUS logAll(MotionEvent me, int meID) {
        if (!toLog) return STATUS.LOG_DISABLED;

        try {
            if (allLogFile == null) { // Open only if not opened before
                allLogFile = new PrintWriter(new FileWriter(allLogFilePath, true));
            }

            String logStr = Actioner.get()._technique.ordinal() + SEP +
                    _phase + SEP +
                    _subblockNum + SEP +
                    _trialNum + SEP +
                    meID + SEP +
                    motionEventToStr(me);

            allLogFile.println(logStr);
            allLogFile.flush();

//            Log.d(TAG, "Logged in ALL!");
            return STATUS.SUCCESS;

        } catch (IOException | NullPointerException e) {
            Log.d(TAG, "Error in accessing ALL log file");
            return STATUS.ERR_LOG_FILE_ACCESS;
        }
    }

    /**
     * Log a META
     * @param startMeId int - meId of the start event
     * @param actionStartPC PointerCoords of the start point
     * @param endMeId int - meId of the end event
     * @param actionEndPC PointerCoords of the end point
     * @param duration Action duration (in ms)
     * @param dX Difference in X
     * @param dY Difference in Y
     * @return STATUS
     */
    public STATUS logMeta(int  startMeId,
                          PointerCoords actionStartPC,
                          int endMeId,
                          PointerCoords actionEndPC,
                          float dX,
                          float dY,
                          int duration) {

        if (!toLog) return STATUS.LOG_DISABLED;

        try {
            if (metaLogFile == null) { // Open only if not opened before
                metaLogFile = new PrintWriter(new FileWriter(metaLogFilePath, true));
            }

            // Create and write the log
            String logStr = Actioner.get()._technique.ordinal() + SEP +
                    _phase + SEP +
                    _subblockNum + SEP +
                    _trialNum + SEP +
                    startMeId + SEP +
                    pointerCoordsToStr(actionStartPC) + SEP +
                    endMeId + SEP +
                    pointerCoordsToStr(actionEndPC) + SEP +
                    Utils.double3Dec(dX) + SEP +
                    Utils.double3Dec(dY) + SEP +
                    duration;

            metaLogFile.println(logStr);
            metaLogFile.flush();
//            metaLogFile.close();

            Log.d(TAG, "Logged in META");
            return STATUS.SUCCESS;

        } catch (IOException | NullPointerException e) {
            Log.d(TAG, "Error in accessing META log file");
            return STATUS.ERR_LOG_FILE_ACCESS;
        }
    }

    /**
     * Log to the COORDS file
     * @param state STATE
     * @param coordsList List of PointerCoords
     * @param time long
     * @return STATUS
     */
    public STATUS logCoords(STATE state, List<PointerCoords> coordsList, long time) {
        if (!toLog) return STATUS.LOG_DISABLED;

        try {
            if (coordsLogFile == null) { // Open only if not opened before
                coordsLogFile = new PrintWriter(new FileWriter(coordsLogFilePath, true));
            }

            // Add the infor and all the available coords to a StringBuilder
            StringBuilder logSB = new StringBuilder();
            logSB.append(Actioner.get()._technique.ordinal()).append(SEP)
                            .append(_phase).append(SEP)
                            .append(_subblockNum).append(SEP)
                            .append(_trialNum).append(SEP)
                            .append(state.ordinal()).append(SEP)
                            .append(coordsList.size()).append(SEP);
            for(int i = 0; i < coordsList.size(); i++) {
                logSB.append(pointerCoordsToStr(coordsList.get(i))).append(SEP);
            }
            logSB.deleteCharAt(logSB.length() - 1);

            // Write to the file
            metaLogFile.println(logSB.toString());
            metaLogFile.flush();

            Log.d(TAG, "Logged in COORDS");
            return STATUS.SUCCESS;

        } catch (IOException | NullPointerException e) {
            Log.d(TAG, "Error in accessing META log file");
            return STATUS.ERR_LOG_FILE_ACCESS;
        }
    }

    public STATUS finishLogs() {
        try {
            if (metaLogFile != null) {
                metaLogFile.close();
                metaLogFile = null;
            }
            if (allLogFile != null) {
                allLogFile.close();
                allLogFile = null;
            }

            return STATUS.SUCCESS;

        } catch (NullPointerException e) {
            Log.d(TAG, "Error in accessing META log file");
            return STATUS.ERR_LOG_FILE_ACCESS;
        }
    }

    /**
     * Get the string for a MotionEvent.PointerCoord
     * @return String (semi-colon separated)
     */
    private String pointerCoordsToStr(PointerCoords inPC) {
        return
                Utils.double3Dec(inPC.orientation) + SEP +
                Utils.double3Dec(inPC.pressure) + SEP +
                Utils.double3Dec(inPC.size) + SEP +
                Utils.double3Dec(inPC.toolMajor) + SEP +
                Utils.double3Dec(inPC.toolMinor) + SEP +
                Utils.double3Dec(inPC.touchMajor) + SEP +
                Utils.double3Dec(inPC.touchMinor) + SEP +
                Utils.double3Dec(inPC.x) + SEP +
                Utils.double3Dec(inPC.y);

    }

    /**
     * Truly GET the PointerCoords!
     * @param me MotionEvent
     * @param pointerIndex int pointer index
     * @return String
     */
    public String pointerCoordsToStr(MotionEvent me, int pointerIndex) {
        PointerCoords result = new PointerCoords();
        me.getPointerCoords(pointerIndex, result);
        return pointerCoordsToStr(result);
    }

    /**
     * Get the String of a MotionEvent
     * @param me MotionEvent
     * @return String
     */
    public String motionEventToStr(MotionEvent me) {
        StringBuilder result = new StringBuilder();

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
        for(pi = 0; pi < me.getPointerCount(); pi++) {
            result.append(pi).append(SEP); // Index
            result.append(me.getPointerId(pi)).append(SEP); // Id
            result.append(pointerCoordsToStr(me, pi)).append(SEP); // PointerCoords
        }

        for (pi = nPointers; pi < 5; pi++) {
            result.append(-1).append(SEP); // Index = -1
            result.append(-1).append(SEP); // Id = -1
            result.append(pointerCoordsToStr(new PointerCoords())).append(SEP); // PointerCoords = empty
        }

        return result.toString();

    }

    /**
     * Create a dir if not existed
     * @param path Dir path
     * @return STATUS
     */
    public STATUS createDir(String path) {
        File folder = new File(path);

        boolean result = true;
        if (!folder.exists()) {
            result = folder.mkdirs();
        } else {
            return STATUS.DIR_EXISTS;
        }

        if (result) return STATUS.SUCCESS;
        else return STATUS.ERR_DIR_CREATION;

    }

    /**
     * Return the header for the meta file
     * @return String
     */
    private String metaLogHeader() {
        return "technique;" +
                "phase;" +
                "subblock_num;" +
                "trial_num;" +
                "action_id" +
                "action_start_orientation;" +
                "action_start_pressure;" +
                "action_start_size;" +
                "action_start_toolMajor;" +
                "action_start_toolMinor;" +
                "action_start_touchMajor;" +
                "action_start_touchMinor;" +
                "action_start_x;" +
                "action_start_y;" +
                "action_end_orientation;" +
                "action_end_pressure;" +
                "action_end_size;" +
                "action_end_toolMajor;" +
                "action_end_toolMinor;" +
                "action_end_touchMajor;" +
                "action_end_touchMinor;" +
                "action_end_x;" +
                "action_end_y;" +
                "dX;" +
                "dY;" +
                "duration;";
    }

    private String coordsHeader() {
        return "technique;" + "phase;" + 
                "subblock_num;" + "trial_num;" +
                "state;" +
                "num_fingers;" +
                "time;" +
                "finger_1_orientation;" +
                "finger_1_pressure;" +
                "finger_1_size;" +
                "finger_1_toolMajor;" +
                "finger_1_toolMinor;" +
                "finger_1_touchMajor;" +
                "finger_1_touchMinor;" +
                "finger_1_x;" +
                "finger_1_y;" +
                "finger_2_orientation;" +
                "finger_2_pressure;" +
                "finger_2_size;" +
                "finger_2_toolMajor;" +
                "finger_2_toolMinor;" +
                "finger_2_touchMajor;" +
                "finger_2_touchMinor;" +
                "finger_2_x;" +
                "finger_2_y" +
                "finger_3_orientation;" +
                "finger_3_pressure;" +
                "finger_3_size;" +
                "finger_3_toolMajor;" +
                "finger_3_toolMinor;" +
                "finger_3_touchMajor;" +
                "finger_3_touchMinor;" +
                "finger_3_x;" +
                "finger_3_y" +
                "finger_4_orientation;" +
                "finger_4_pressure;" +
                "finger_4_size;" +
                "finger_4_toolMajor;" +
                "finger_4_toolMinor;" +
                "finger_4_touchMajor;" +
                "finger_4_touchMinor;" +
                "finger_4_x;" +
                "finger_4_y" +
                "finger_5_orientation;" +
                "finger_5_pressure;" +
                "finger_5_size;" +
                "finger_5_toolMajor;" +
                "finger_5_toolMinor;" +
                "finger_5_touchMajor;" +
                "finger_5_touchMinor;" +
                "finger_5_x;" +
                "finger_5_y";
    }
    
    /**
     * Return the header for the all file
     * @return String
     */
    private String allLogHeader() {
        return "technique" + SEP +
                "phase" + SEP +
                "subblock_num" + SEP +
                "trial_num" + SEP +

                "event_id" + SEP +

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
                "finger_3_y" +

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
                "finger_4_y" +

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
}
