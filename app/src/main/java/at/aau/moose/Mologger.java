package at.aau.moose;

import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/***
 * Responsible for logging the events
 */
public class Mologger {

    private final String TAG = "Mologger";

    private static Mologger self; // for singleton

    private boolean toLog = false;
    private List<MotionEvent> eventList = new ArrayList<>();

    private PrintWriter logFile;
    private String particID = "Pa1";

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
        // Create the log file
        try {
            logFile = new PrintWriter(new FileWriter(
                            Environment.getExternalStorageDirectory() + "/" +
                                    particID + ".txt"));
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Problem in creating log file!");
        }
    }

    /**
     * Log the event
     * @param mevent MotionEvent
     */
    public void log(MotionEvent mevent) {
        if (logFile != null && toLog) {
            String infoStr = "(" + mevent.getX() + "," + mevent.getY() + ")";
            infoStr += " -- " + mevent.getTouchMajor() + "," + mevent.getTouchMinor();
            Log.d(TAG, infoStr);
            logFile.println(infoStr);
        } else {
            Log.d(TAG, "No logging happened!");
        }
    }

    /**
     * Set the state of loggin
     * @param logState state of logging
     */
    public void setLogState(boolean logState) {
        Log.d(TAG, "Logging state: " + logState);
        toLog = logState;
        if (!logState) logFile.close();
    }
}
