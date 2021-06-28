package at.aau.moose;

import android.util.Log;

public enum STATUS {
    LOG_DISABLED,
    ERR_DIR_CREATION,
    ERR_FILES_CREATION,
    ERR_LOG_FILE_ACCESS,
    DIR_EXISTS,
    SUCCESS;

    private String TAG = "[[STATUS]] ";

    public void output(String source) {
        switch (this) {
        case ERR_DIR_CREATION:
            Log.d(TAG, source + ">> Problem in creating directory!");
            break;
        case DIR_EXISTS:
            Log.d(TAG, source + ">> Directory exists.");
            break;
        }
    }

}
