package at.aau.moose;

import android.util.Log;
import android.view.MotionEvent;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Calendar;

public class Utils {

    private static String TAG = "Moose_Utils";

    /**
     * Split the input String with "-" and return the two parts
     * If only one part, return empty for the other part
     * @param inStr Input String
     * @return String parts
     */
    public static String[] splitStr(String inStr) {
        String[] result = new String[2];
        result[0] = "";
        result[1] = "";
        String[] splString = inStr.split("-");
        Log.d(TAG, Arrays.toString(splString));
        if (splString.length > 1) {
            result[0] = splString[0];
        }

        if (splString.length == 2) {
            result[1] = splString[1];
        }

        return result;
    }

    /**
     * Return the current date and time
     * @return String
     */
    public static String nowDateTime() {
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy_hh-mm");
        return format.format(Calendar.getInstance().getTime());
    }

    /**
     * Calculate the Euclidean distance between two coords
     * @param pc1 PointerCoords 1
     * @param pc2 PointerCoords 2
     * @return Double distance
     */
    public static double distance(MotionEvent.PointerCoords pc1,
                                  MotionEvent.PointerCoords pc2) {
        return Math.sqrt(Math.pow(pc1.x - pc2.x, 2) + Math.pow(pc1.y - pc2.y, 2));
    }
}
