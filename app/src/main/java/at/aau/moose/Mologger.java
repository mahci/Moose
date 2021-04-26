package at.aau.moose;

import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Objects;

import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

/***
 * Responsible for logging the events
 */
public class Mologger {

    private final String TAG = "Moose_Mologger";
    //--------------------------------------------

    private static Mologger self; // for singleton

    private boolean isLogging = false;

    // Naming
    private static String LOGS_DIR;
    private static String LOG_FILE_PFX  = "Log-";
    private static String PTC_PFX       = "PTC";
    private static String EXP_FILE_PFX  = "EXP";
    private static String BLK_FILE_PFX  = "BLK";

    // Values
    private String ptcDirPath = "";
    private String expDirPath = "";
    private PrintWriter blockLogFile;
    private PrintWriter allLogFile;
//    private int expNum;

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
        // Set up the log dir
        LOGS_DIR = Environment.getExternalStorageDirectory() +
                "/" + "Moose" + "/";
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
        String dir = LOGS_DIR + PTC_PFX + pid;
        if (createDir(dir)) ptcDirPath = dir + "/";
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
            if (createDir(dir)) expDirPath = dir + "/";
        } else {
            Log.d(TAG, "No particip dir found!");
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
    public int log(String lg) {
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
     * @return Success
     */
    public boolean createDir(String path) {
        File folder = new File(path);
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }

        if (success){
            Log.d(TAG, path + " created");
        } else {
            Log.d(TAG, "Problem in creating " + path);
        }

        return success;
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
}
