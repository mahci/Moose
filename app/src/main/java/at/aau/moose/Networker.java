package at.aau.moose;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Objects;

import javax.net.SocketFactory;

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

    private int timeoutCount = 0;

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
                if (line != null) {
                    // Extract the parts of the message
                    String[] parts = line.split("_");
                    if (parts.length > 0) {
                        // Prefix and message
                        String prefix = parts[0];

                        // Command
                        switch (prefix) {
                        case Config.MSSG_CONFIRM:
                            break;
                        case Config.MSSG_PID:
                            if (parts.length > 1) {
                                // Get the participant's ID
                                Mologger.get().setupParticipantLog(parts[1]);
                            } else {
                                Log.d(TAG, "No participant ID received!");
                            }
                            break;
                        case Config.MSSG_BEG_EXP:
                            // Tell the MainActivity to begin experimente
                            MainActivity.beginExperiment();

                            if (parts.length > 1) {
                                // Get the experiment number
                                int expNum = Integer.parseInt(parts[1]);
                                Mologger.get().setupExperimentLog(expNum);
                            } else {
                                Log.d(TAG, "No experiment number received!");
                            }
                            break;
                        case Config.MSSG_BEG_BLK:
                            if (parts.length > 1) {
                                // Get the experiment number
                                int blkNum = Integer.parseInt(parts[1]);
                                Mologger.get().setupBlockLog(blkNum);
                            } else {
                                Log.d(TAG, "No block number received!");
                            }
                            break;
                        case Config.MSSG_END_TRL:
                            Mologger.get().finishTrialLog();
                            break;
                        case Config.MSSG_END_BLK:
                            Mologger.get().finishBlockLog();
                            break;
                        }
                    }

                }

                if (Objects.equals(line, Config.MSSG_BEG_LOG)) { // Start of logging
                    Mologger.get().setLogState(true);
                }
                if (Objects.equals(line, Config.MSSG_END_LOG)) { // End of logging
                    Mologger.get().setLogState(false);
                }

            } while(!Objects.equals(line, Config.NET_DISCONNECT));
            System.exit(0);
        }).subscribeOn(Schedulers.io());

        // Connect to server (runs once)
        new NetTask().execute(Config.NET_CONNECT);
    }

    /**
     * Connections task
     */
    @SuppressLint("StaticFieldLeak")
    private class NetTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... tasks) {

            // Run the appropriate action
            if (Objects.equals(tasks[0], Config.NET_CONNECT)) {
                Log.d(TAG, "Connecting to Expenvi...");
                long t0 = Calendar.getInstance().getTimeInMillis();
                while (Calendar.getInstance().getTimeInMillis() - t0 < Config.TIMEOUT) {
                    try {
                        // Open the socket
                        socket = new Socket(Config.SERVER_IP, Config.SERVER_Port);
                        Log.d(TAG, "Socket opened");
                        // Create streams for I/O
                        outChannel = new PrintStream(socket.getOutputStream());
                        inChannel = new BufferedReader(
                                new InputStreamReader(socket.getInputStream()));
                        Log.d(TAG, "Channels opened");
                        // Send the first message and get the reply for connection confirmation
                        outChannel.println(Config.MSSG_MOOSE);

                        String line = inChannel.readLine();
                        if (Objects.equals(line, Config.MSSG_CONFIRM)) { // Successful!
                            Log.d(TAG, "Connection Successful!");
                            return "SUCCESS";
                        }

                        return "FAIL";

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d(TAG, "Reconnecting...");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException interruptedException) {
                            interruptedException.printStackTrace();
                        }
//                        return "FAIL";
                    }
                }

            }

            return "FAIL";
        }

        @Override
        protected void onPostExecute(String s) {
            if (Objects.equals(s, "SUCCESS")) {
                Log.d(TAG, "Start receiving data...");
                // Start receiving data from server
                receiverOberservable.subscribe();
            } else {
                Log.d(TAG, "Connection failed!");
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
            Log.d(TAG, "Out channel not available!");
        }
    }

}


