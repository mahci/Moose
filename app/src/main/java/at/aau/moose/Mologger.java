package at.aau.moose;

import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

import static android.view.MotionEvent.PointerCoords;

/***
 * Responsible for logging the events
 */
public class Mologger {

    private final String TAG = "Moose_Mologger";
    //--------------------------------------------

    private static Mologger self; // for singleton

    public boolean isLogging = true;

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

    private String metaLogFilePath;
    private String coordsLogFilePath;
    private String allLogFilePath;

    private PrintWriter metaLogFile;
    private PrintWriter coordsLogFile;
    private PrintWriter allLogFile;
//    private int expNum;

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
        logDir = Environment.getExternalStorageDirectory() +
                "/" + "Moose" + "/";
        createDir(logDir);
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

//            Log.d(TAG, "Logged in ALL!");
            return STATUS.SUCCESS;

        } catch (IOException | NullPointerException e) {
            Log.d(TAG, "Error in accessing ALL log file");
            return STATUS.ERR_LOG_FILE_ACCESS;
        }
    }

    /**
     * Log a META
     * @param actionStartPC PointerCoords of the start point
     * @param actionEndPC PointerCoords of the end point
     * @param duration Action duration (in ms)
     * @param dX Difference in X
     * @param dY Difference in Y
     * @return STATUS
     */
    public STATUS logMeta(int  actionID,
                          PointerCoords actionStartPC,
                          PointerCoords actionEndPC,
                          float dX,
                          float dY,
                          int duration) {

        if (!isLogging) return STATUS.LOG_DISABLED;

        try {
            if (metaLogFile == null) { // Open only if not opened before
                metaLogFile = new PrintWriter(new FileWriter(metaLogFilePath, true));
            }

            // Create and write the log
            String logStr = Actioner.get()._technique.ordinal() + SEP +
                    _phase + SEP +
                    _subblockNum + SEP +
                    _trialNum + SEP +
                    actionID + SEP +
                    getPCoordStr(actionStartPC) + SEP +
                    getPCoordStr(actionEndPC) + SEP +
                    Utils.double3Dec(dX) + SEP +
                    Utils.double3Dec(dY) + SEP +
                    duration + SEP;

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
     * Log to the COORDS file
     * @param state STATE
     * @param coordsList List of PointerCoords
     * @param time long
     * @return STATUS
     */
    public STATUS logCoords(STATE state, List<PointerCoords> coordsList, long time) {
        if (!isLogging) return STATUS.LOG_DISABLED;

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
                logSB.append(getPCoordStr(coordsList.get(i))).append(SEP);
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
        StringBuilder headerSB = new StringBuilder()
                .append("technique;")
                .append("phase;")
                .append("subblock_num;")
                .append("trial_num;")
                .append("motion_event");
        return headerSB.toString();
    }
}
