package at.aau.moose;

import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

/***
 * Responsible for logging the events
 */
public class Mologger {

    private final String TAG = "Moose_Mologger";
    //--------------------------------------------

    private static Mologger self; // for singleton

    private boolean isLogging = true;

    // Naming
    private static String logDir;

    private static final String PI = "P"; // Participant indicator
    private static final String SEP = ";"; // Separator

    private static String LOG_FILE_PFX  = "Log-";
    private static String PTC_PFX       = "PTC";
    private static String EXP_FILE_PFX  = "EXP";
    private static String BLK_FILE_PFX  = "BLK";

    // Values
    private String ptcDirPath = "";
    private String phaseDirPath = "";
    private String expDirPath = "";
    private PrintWriter blockLogFile;

    private String allLogFilePath;
    private String metaLogFilePath;
    private PrintWriter metaLogFile;
    private PrintWriter allLogFile;
//    private int expNum;

    public String _phase;
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
        logDir = Environment.getExternalStorageDirectory() +
                "/" + "Moose" + "/";
        createDir(logDir);
    }

    /**
     * Log the start of a participant
     * @param pid participant's id
     * @return STATUS
     */
    public STATUS logParticipant(String pid) {
        // Create the META and ALL file
        try {
            metaLogFilePath = logDir +
                    PI + pid + "_" +
                    Utils.nowDateTime() + "_" +
                    "META.txt";

            allLogFilePath = logDir +
                    PI + pid + "_" +
                    Utils.nowDateTime() + "_" +
                    "ALL.txt";

            // Open meta file and write the column headers
            metaLogFile = new PrintWriter(new FileWriter(metaLogFilePath, true));
            metaLogFile.println(metaLogHeader());
            metaLogFile.flush();

            // Open all file and write the column headers
            allLogFile = new PrintWriter(new FileWriter(allLogFilePath, true));
            allLogFile.println(allLogHeader());
            allLogFile.flush();

            Log.d(TAG, "Log files created");

            return STATUS.SUCCESS;
        } catch (IOException e) {
            Log.d(TAG, "Error in creating participant files!");
            e.printStackTrace();
            return STATUS.LOG_ERR_FILES_CREATION;
        }
    }

    /**
     * Log a MotionEvent (in ALL)
     * @param me MotionEvent
     * @return STATUS
     */
    public STATUS logAll(MotionEvent me) {
        if (!isLogging) return STATUS.LOG_DISABLED;

        try {
            if (allLogFile == null) { // Open only if not opened before
                allLogFile = new PrintWriter(new FileWriter(allLogFilePath, true));
            }
            allLogFile.println(Actioner.get()._technique.ordinal() + ";" + me);
            allLogFile.flush();

            Log.d(TAG, "Logged in ALL!");
            return STATUS.SUCCESS;

        } catch (IOException | NullPointerException e) {
            Log.d(TAG, "Error in accessing ALL log file");
            return STATUS.ERR_LOG_FILE_ACCESS;
        }
    }

    /**
     * Log a META event
     * @param actionStartPC PointerCoords of the start point
     * @param actionEndPC PointerCoords of the end point
     * @param durationPress Duration of the PRESS part of the action (if exists)
     * @param duration Action duration (long in ms)
     * @param dX Difference in X
     * @param dY Difference in Y
     * @param nExtraFingers Number of extra fingers (apart from the active one) on the screen
     * @param extra1PC PointerCoords of the 1st extra finger
     * @param extra2PC PointerCoords of the 2nd extra finger
     * @return STATUS
     */
    public STATUS logMeta(MotionEvent.PointerCoords actionStartPC,
                          MotionEvent.PointerCoords actionEndPC,
                          long durationPress,
                          long duration,
                          double dX,
                          double dY,
                          int nExtraFingers,
                          MotionEvent.PointerCoords extra1PC,
                          MotionEvent.PointerCoords extra2PC) {
        if (!isLogging) return STATUS.LOG_DISABLED;

        try {
            if (metaLogFile == null) { // Open only if not opened before
                metaLogFile = new PrintWriter(new FileWriter(metaLogFilePath, true));
            }

            // Create and write the log
            String logStr =
                    Actioner.get()._technique.ordinal() + SEP +
                    _phase + SEP +
                    _subblockNum + SEP +
                    _trialNum + SEP +
                    getPCoordStr(actionStartPC) + SEP +
                    getPCoordStr(actionEndPC) + SEP +
                    durationPress + SEP +
                    duration + SEP +
                    dX + SEP + dY + SEP +
                    nExtraFingers + SEP +
                    getPCoordStr(extra1PC) + SEP +
                    getPCoordStr(extra2PC);

            metaLogFile.println(logStr);
            metaLogFile.flush();
            metaLogFile.close();

            Log.d(TAG, "Logged in META");
            return STATUS.SUCCESS;

        } catch (IOException | NullPointerException e) {
            Log.d(TAG, "Error in accessing META log file");
            return STATUS.ERR_LOG_FILE_ACCESS;
        }
    }

    /**
     * Get the string for a MotionEvent.PointerCoord
     * @return String (semi-colon separated)
     */
    private String getPCoordStr(MotionEvent.PointerCoords inPC) {
        return
                inPC.orientation + SEP +
                inPC.pressure + SEP +
                inPC.size + SEP +
                inPC.toolMajor + SEP +
                inPC.toolMinor + SEP +
                inPC.touchMajor + SEP +
                inPC.touchMinor + SEP +
                inPC.x + SEP +
                inPC.y;

    }

    /**
     * Subscribe to a TouchEvent publisher
     * @param tePublisher TouchEventPublisher
     */
    public void subscribeToEvents(PublishSubject<TouchEvent> tePublisher) {
        tePublisher
                .observeOn(Schedulers.io())
                .subscribe(
//                        this::log
                );
    }

    /**
     * Log the event
     * @param mevent MotionEvent
     */
//    public void log(MotionEvent mevent) {
//        if (blockLogFile != null && toLog) {
//            String infoStr = "(" + mevent.getX() + "," + mevent.getY() + ")";
//            infoStr += " -- " + mevent.getTouchMajor() + "," + mevent.getTouchMinor();
//            Log.d(TAG, infoStr);
//            blockLogFile.println(infoStr);
//        } else {
////            Log.d(TAG, "No logging happened!");
//        }
//    }

    /**
     * Set up a participant directory for logging
     * @param pid Participant id
     */
    public void setupParticipantLog(String pid) {
//        participID = pid;

        // Create appropriate directory
        String dir = logDir + PTC_PFX + pid;
        if (createDir(dir) == STATUS.SUCCESS) ptcDirPath = dir + "/";
    }

    /**
     * Log the start of a phase
     * @param phase String
     */
    public void logPhaseStart(String phase) {
        // Create a dir for the phase
        String phaseDir = ptcDirPath + "/" +
                phase + "-" +
                Actioner.get()._technique;
        if (createDir(phaseDir) == STATUS.SUCCESS) {
            phaseDirPath = phaseDir + "/";
            if (phase != "SHOWCASE") isLogging = true;
        }
    }

    /**
     * Set the experiment numebr
     * @param desc Description of the experiment (date, ...)
     */
    public void setupExperimentLog(String desc) {
//        this.expNum = expNum;
        // Create a folder for the experiment
        if (!ptcDirPath.equals("")) {
            String dir = ptcDirPath + desc;
            if (createDir(dir) == STATUS.SUCCESS) expDirPath = dir + "/";
        } else {
            Log.d(TAG, "No particip dir found!");
        }

    }

    public void logBlockStart(int blkNum) {
        if (!phaseDirPath.isEmpty()) {
            try {
                String blkFilePath = phaseDirPath +
                        BLK_FILE_PFX + "-" + blkNum + ".txt";

                blockLogFile = new PrintWriter(new FileWriter(blkFilePath));

                String allLogFilePath = phaseDirPath +
                        BLK_FILE_PFX + "-" + blkNum + "-all.txt";

                allLogFile = new PrintWriter(new FileWriter(allLogFilePath));

                Log.d(TAG, "Log files created");
            } catch (IOException e) {
                Log.d(TAG, "Problem in creating block file!");
                e.printStackTrace();
            }
        }
    }

    /**
     * Set up a log for the Block
     * @param blkNum Block number
     */
    public void setupBlockLog(int blkNum) {
        // Create the block file
        if (!expDirPath.isEmpty()) {
            try {
                String blkFilePath = expDirPath +
                        BLK_FILE_PFX + "-" + blkNum + ".txt";

                blockLogFile = new PrintWriter(new FileWriter(blkFilePath));

                String allLogFilePath = expDirPath +
                        BLK_FILE_PFX + "-" + blkNum + "-all.txt";

                allLogFile = new PrintWriter(new FileWriter(allLogFilePath));

                Log.d(TAG, "Log files created");
            } catch (IOException e) {
                Log.d(TAG, "Problem in creating block file!");
                e.printStackTrace();
            }
        }
    }

    /**
     * Finish the current trial => add a line in file
     */
    public void finishTrialLog() {
        if (blockLogFile != null) {
            blockLogFile.println("---------------------------------");
        }
        if (allLogFile != null) {
            allLogFile.println("---------------------------------");
        }
    }

    /**
     * Finish the current block => close the block file
     */
    public void finishBlockLog() {
        blockLogFile.close();
        allLogFile.close();
    }

    /**
     * Log a TouchEvent
     * @param lg String to log
     */
    public int logAll(String lg) {
        if (!isLogging) return 1;

        if (blockLogFile !=null) {
            blockLogFile.println(lg);
//            Log.d(TAG, "Action Logged");
        } else {
            Log.d(TAG, "Can't access block log file!");
        }
        return 0;
    }



    public int logAll(TouchEvent tevent) {
        if (!isLogging) return 1;

        if (allLogFile !=null) {
            allLogFile.println(tevent);
//            Log.d(TAG, "Action Logged");
        } else {
            Log.d(TAG, "Can't access block log file!");
        }
        return 0;
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
        }

        if (result) return STATUS.SUCCESS;
        else return STATUS.LOG_ERR_DIR_CREATION;

    }

    /**
     * Set the state of loggin
     * @param logState state of logging
     */
    public void setLogState(boolean logState) {
        Log.d(TAG, "Logging state: " + logState);
        isLogging = logState;
//        if (!logState) blockLogFile.close();
    }

    /**
     * Return the header for the meta file
     * @return String
     */
    private String metaLogHeader() {
        StringBuilder headerSB = new StringBuilder()
                .append("technique;")
                .append("phase;")
                .append("subblock_num;")
                .append("trial_num;")
                .append("action_start_orientation;")
                .append("action_start_pressure;")
                .append("action_start_size;")
                .append("action_start_toolMajor;")
                .append("action_start_toolMinor;")
                .append("action_start_touchMajor;")
                .append("action_start_touchMinor;")
                .append("action_start_x;")
                .append("action_start_y;")
                .append("action_end_orientation;")
                .append("action_end_pressure;")
                .append("action_end_size;")
                .append("action_end_toolMajor;")
                .append("action_end_toolMinor;")
                .append("action_end_touchMajor;")
                .append("action_end_touchMinor;")
                .append("action_end_x;")
                .append("action_end_y;")
                .append("durationPress;")
                .append("durationRelease;")
                .append("dX;")
                .append("dY;")
                .append("num_extra_fingers_on_screen;")
                .append("extra_finger_1_orientation;")
                .append("extra_finger_1_pressure;")
                .append("extra_finger_1_size;")
                .append("extra_finger_1_toolMajor;")
                .append("extra_finger_1_toolMinor;")
                .append("extra_finger_1_touchMajor;")
                .append("extra_finger_1_touchMinor;")
                .append("extra_finger_1_x;")
                .append("extra_finger_1_y;")
                .append("extra_finger_2_orientation;")
                .append("extra_finger_2_pressure;")
                .append("extra_finger_2_size;")
                .append("extra_finger_2_toolMajor;")
                .append("extra_finger_2_toolMinor;")
                .append("extra_finger_2_touchMajor;")
                .append("extra_finger_2_touchMinor;")
                .append("extra_finger_2_x;")
                .append("extra_finger_2_y");

        return headerSB.toString();
    }

    /**
     * Return the header for the all file
     * @return String
     */
    private String allLogHeader() {
        StringBuilder headerSB = new StringBuilder()
                .append("technique;")
                .append("phase;")
                .append("subblock_num;")
                .append("trial_num;")
                .append("motion_event");
        return headerSB.toString();
    }
}
