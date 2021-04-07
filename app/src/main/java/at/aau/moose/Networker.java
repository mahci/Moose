package at.aau.moose;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Objects;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class Networker {

    private final String TAG = "Moose_Networker";

    private static Networker self; // For singleton

    private Socket socket;
    private PrintStream outChannel;
    private BufferedReader inChannel;

    private Observable<Object> receiverOberservable;
    private PublishSubject<String> statePuSu;

    /**
     * Get the singletong instance
     * @return self instance
     */
    public static Networker get() {
        if (self == null) self = new Networker();
        return self;
    }

    /**
     * Constructor
     */
    private Networker() {

    }

    /**
     * Connect to Empenvi
     */
    public void connect() {
        // Create receiverObserable (doesn't start until after subscription)
        receiverOberservable = Observable.fromAction(() -> {
            Log.d(TAG, "Receiving data...");
            // Continously read lines from server until DISCONNECT is received
            String line;
            do {
                line = inChannel.readLine();
                // Got the begin line message
                Log.d(TAG, "Server message: " + line);
                if (line != null) {
                    // Extract the parts of the message
                    String[] parts = line.split("_");
                    if (parts.length > 0) {
                        // Prefix and message
                        String prefix = parts[0];

                        // Command
                        switch (prefix) {
                        case Const.MSSG_CONFIRM:
                            break;
                        case Const.MSSG_PID:
                            if (parts.length > 1) {
                                // Get the participant's ID
                                Mologger.get().setupParticipantLog(parts[1]);
                            } else {
                                Log.d(TAG, "No participant ID received!");
                            }
                            break;
                        case Const.MSSG_BEG_EXP:
                            // Tell the MainActivity to save the initial state
                            MainActivity.beginExperiment();

                            if (parts.length > 1) {
                                // Get the experiment number
                                int expNum = Integer.parseInt(parts[1]);
                                Mologger.get().setupExperimentLog(expNum);
                            } else {
                                Log.d(TAG, "No experiment number received!");
                            }
                            break;
                        case Const.MSSG_BEG_BLK:
                            if (parts.length > 1) {
                                // Get the experiment number
                                int blkNum = Integer.parseInt(parts[1]);
                                Mologger.get().setupBlockLog(blkNum);
                            } else {
                                Log.d(TAG, "No block number received!");
                            }
                            break;
                        case Const.MSSG_END_TRL:
                            Mologger.get().finishTrialLog();
                            break;
                        case Const.MSSG_END_BLK:
                            Mologger.get().finishBlockLog();
                            break;
                        }
                    }

                }

                if (Objects.equals(line, Const.MSSG_BEG_LOG)) { // Start of logging
                    Mologger.get().setLogState(true);
                }
                if (Objects.equals(line, Const.MSSG_END_LOG)) { // End of logging
                    Mologger.get().setLogState(false);
                }

            } while(!Objects.equals(line, Const.NET_DISCONNECT));
        }).subscribeOn(Schedulers.io());

        // Connect to server (runs once)
        new NetTask().execute(Const.NET_CONNECT);
    }

    /**
     * Connections task
     */
    @SuppressLint("StaticFieldLeak")
    private class NetTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... tasks) {

            // Run the appropriate action
            if (Const.NET_CONNECT.equals(tasks[0])) {
                Log.d(TAG, "Connecting...");
                try {
                    // Open the socket
                    socket = new Socket(Const.SERVER_IP, Const.SERVER_Port);
                    Log.d(TAG, "Socket created");
                    // Create streams for I/O
                    outChannel = new PrintStream(socket.getOutputStream());
                    inChannel = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));
                    Log.d(TAG, "Channels created");
                    // Send the first message and get the reply for connection confirmation
                    outChannel.println(Const.MSSG_MOOSE);
                    Log.d(TAG, "Moose message sent");
                    String line = inChannel.readLine();
                    Log.d(TAG, line);
                    if (Objects.equals(line, Const.MSSG_CONFIRM)) { // Successful!
                        Log.d(TAG, "Connection Successful!");
                        return "SUCCESS";
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    return "FAIL";
                }
            }

            return "FAIL";
        }

        @Override
        protected void onPostExecute(String s) {
            if (Objects.equals(s, "SUCCESS")) {
                Log.d(TAG, "Start receiving...");
                // Start receiving data from server
                receiverOberservable.subscribe();
            }
        }
    }

    /**
     * Send an action string to the Expenvi
     * @param actStr String action (from Constants)
     */
    public void sendAction(String actStr) {
        if (outChannel != null) {
            outChannel.println(actStr);
            outChannel.flush();
            Log.d(TAG, actStr + " sent to server");
        } else {
            Log.d(TAG, "Channel to server not opened!");
        }
    }

}


