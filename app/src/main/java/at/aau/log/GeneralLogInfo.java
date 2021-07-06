package at.aau.log;

import at.aau.moose.Actioner;
import at.aau.moose.Strs;

import static at.aau.moose.Strs.SEP;

public class GeneralLogInfo {
    public Actioner.TECHNIQUE technique; // Ordinal
    public int phase; // Ordinal
    public int subBlockNum;
    public int trialNum;

    /**
     * Get the header for the log file
     * @return String - header with the names of the vars
     */
    public static String getLogHeader() {
        return "technique" + SEP +
                "phase" + SEP +
                "subblock_num" + SEP +
                "trial_num";
    }

    /**
     * Get the String of this object for logging
     * @return String - ';'-delimited
     */
    public String toLogString() {
        return technique.ordinal() + SEP +
                phase + SEP +
                subBlockNum + SEP +
                trialNum;
    }
}
