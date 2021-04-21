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

    private boolean toLog = false;

    // Naming
    private static String LOGS_DIR;
    private static String LOG_FILE_PFX  = "Log-";
    private static String PTC_PFX       = "PTC";
    private static String EXP_FILE_PFX  = "EXP";
    private static String BLK_FILE_PFX  = "BLK";

    // Values
    private String participID;
    private String participLogPath;
    private PrintWriter blockLogFile;
    private int expNum;

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
        participID = pid;

        // Create appropriate directory
        String ptcDirPath = LOGS_DIR + PTC_PFX + participID;
        File folder = new File(ptcDirPath);
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }

        if (success){
            participLogPath = ptcDirPath + "/";
            Log.d(TAG, "Participant folder created");
        }
        else Log.d(TAG, "Problem in creating participant folder");
    }

    /**
     * Set the experiment numebr
     * @param expNum Experiment number
     */
    public void setupExperimentLog(int expNum) {
        this.expNum = expNum;
        Log.d(TAG, "Experiment set = " + expNum);
    }

    /**
     * Set up a log for the Block
     * @param blkNum Block number
     */
    public void setupBlockLog(int blkNum) {
        // Create the block file
        if (!participLogPath.isEmpty()) {
            try {
                String blkFilePath = participLogPath +
                        BLK_FILE_PFX + "-" + blkNum + ".txt";

                blockLogFile = new PrintWriter(new FileWriter(blkFilePath));
                Log.d(TAG, "Block log file created");
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
            blockLogFile.println("---------------------");
        }
    }

    /**
     * Finish the current block => close the block file
     */
    public void finishBlockLog() {
        blockLogFile.close();
    }

    /**
     * Log a TouchEvent
     * @param tevent TouchEvent
     */
    public void log(TouchEvent tevent) {
        if (blockLogFile !=null) {
            blockLogFile.println(tevent);
//            Log.d(TAG, "Action Logged");
        } else {
            Log.d(TAG, "Can't access block log file!");
        }
    }

    /**
     * Set the state of loggin
     * @param logState state of logging
     */
    public void setLogState(boolean logState) {
        Log.d(TAG, "Logging state: " + logState);
        toLog = logState;
        if (!logState) blockLogFile.close();
    }
}
