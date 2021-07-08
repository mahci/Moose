package at.aau.moose;

import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import at.aau.log.GeneralLogInfo;
import at.aau.log.MetaLogInfo;
import at.aau.log.MotionEventLogInfo;

import static android.view.MotionEvent.PointerCoords;
import static at.aau.moose.Strs.SEP;

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
    private String metaLogFilePath = "";
    private String allLogFilePath = "";

    private PrintWriter metaLogFile;
    private PrintWriter allLogFile;

//    private String expLogId;

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
     * @param expLogId String - experiment Id for the files
     * @return STATUS
     */
    public STATUS syncExperiment(String expLogId) {
        try {
            metaLogFilePath = logDir + expLogId + "_" + "META.txt";
            allLogFilePath = logDir + expLogId + "_" + "ALL.txt";

            if (new File(metaLogFilePath).exists()) { // File already exists => open to write
                Log.d(TAG, "META file existed");
                metaLogFile = new PrintWriter(new FileWriter(metaLogFilePath, true));
            } else { // First time creating the file
                metaLogFile = new PrintWriter(new FileWriter(metaLogFilePath, true));
                metaLogFile.println(
                        GeneralLogInfo.getLogHeader() + SEP +
                                MetaLogInfo.getLogHeader());
                metaLogFile.flush();
            }

            if (new File(allLogFilePath).exists()) { // File already exists => open to write
                Log.d(TAG, "ALL file existed");
                allLogFile = new PrintWriter(new FileWriter(allLogFilePath, true));
            } else { // First time creating the file
                allLogFile = new PrintWriter(new FileWriter(allLogFilePath, true));
                allLogFile.println(
                        GeneralLogInfo.getLogHeader() + SEP +
                                MotionEventLogInfo.getLogHeader());
                allLogFile.flush();
            }

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
     * @param mMotionEventLogInfo MotionEventLogInfo
     * @return STATUS
     */
    public STATUS logAll(MotionEventLogInfo mMotionEventLogInfo) {
        if (!toLog) return STATUS.LOG_DISABLED;

        try {
            if (allLogFile == null) { // Open only if not opened before
                allLogFile = new PrintWriter(new FileWriter(allLogFilePath, true));
            }

            allLogFile.println(
                    Actioner.get().mGenLogInfo.toLogString() + SEP +
                    mMotionEventLogInfo.toLogString());
            allLogFile.flush();

//            Log.d(TAG, "Logged in ALL!");
            return STATUS.SUCCESS;

        } catch (IOException | NullPointerException e) {
            Log.d(TAG, "Error in accessing ALL log file");
            Log.d(TAG, e.toString());
            return STATUS.ERR_LOG_FILE_ACCESS;
        }
    }

    /**
     * Log a META
     * @param mMetaLogInfo MetaLogInfo - all the required info
     * @return STATUS
     */
    public STATUS logMeta(MetaLogInfo mMetaLogInfo) {

        if (!toLog) return STATUS.LOG_DISABLED;

        try {
            if (metaLogFile == null) { // Open only if not opened before
                metaLogFile = new PrintWriter(new FileWriter(metaLogFilePath, true));
            }

            metaLogFile.println(
                    Actioner.get().mGenLogInfo.toLogString() +
                    Strs.SEP +
                    mMetaLogInfo.toLogString());
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
     * Close all the logs
     * @return STATUS
     */
    public STATUS closeLogs() {
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
    public String pointerCoordsToStr(PointerCoords inPC) {
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
}
