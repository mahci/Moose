package at.aau.moose;

import android.util.Log;

import java.util.Arrays;

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
}
