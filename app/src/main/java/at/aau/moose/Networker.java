package at.aau.moose;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Calendar;
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
        // Create receiverObserable (starts after connection is established)
        receiverOberservable = Observable.fromAction(() -> {
            Log.d(TAG, "Receiving data...");
            // Continously read lines from server until DISCONNECT is received
            try {
                String line;
                while (true) {
                    line = inChannel.readLine();
                    if (line != null) {
                        Log.d(TAG, "Received: " + line);
                        // Command must be in the format mssg_param
                        processInput(line);
//                        String[] parts = line.split("_");
//                        if (parts.length > 0) {
//                            // Message and param
//                            String mssg = parts[0];
//                            String param = parts[1];
//
//                            // Command
//                            switch (mssg) {
//                            case Config.MSSG_PID:
//                                // Participant's ID
//                                Mologger.get().setupParticipantLog(param);
//                                break;
//
//                            case Config.MSSG_BEG_EXP:
//                                // Tell the MainActivity to begin experimente
//                                MainActivity.beginExperiment();
//
//                                // Experiment description
//                                Mologger.get().setupExperimentLog(param);
//                                Mologger.get().setLogState(true);
//                                break;
//
//                            case Config.MSSG_BEG_BLK:
//                                // Get the experiment number
//                                int blkNum = Integer.parseInt(param);
//                                Mologger.get().setupBlockLog(blkNum);
//                                break;
//
//                            case Config.MSSG_END_TRL:
//                                Mologger.get().finishTrialLog();
//                                break;
//
//                            case Config.MSSG_END_BLK:
//                                Mologger.get().finishBlockLog();
//                                break;
//
//                            case Config.MSSG_END_EXP:
//                                Mologger.get().setLogState(false);
//                                break;
//
//                            case Config.MSSG_BEG_LOG:
//                                Actioner.get().isTrialRunning = true;
//                                break;
//
//                            case Config.MSSG_END_LOG:
//                                Actioner.get().isTrialRunning = false;
//                                break;
//
//                            case Config.NET_DISCONNECT:
//                                connect();
//                                break;
//                            }
//
//                        } else {
//                            Log.d(TAG, "Command not in the right format");
//                        }

                    } else break;
                }
                connect();
            } catch (Exception e) {
                // Try to reconnect
                connect();
            }

//            System.exit(0);
        }).subscribeOn(Schedulers.io());
    }

    /**
     * Connect to Empenvi
     */
    public void connect() {
        // Connect to server (runs once)
        new ConnectTask().execute();
    }

    private void processInput(String inStr) {
        Log.d(TAG, "Process: " + inStr);
        // Command must be in the format mssg_param
        String[] parts = inStr.split("_");
        if (parts.length > 0) {
            // Message and param
            String mssg = parts[0];
            String param = parts[1];

            // Command
            switch (mssg) {
            case Config.MSSG_PID:
                // Participant's ID
                Mologger.get().setupParticipantLog(param);
                break;

            case Config.MSSG_BEG_EXP:
                // Tell the MainActivity to begin experimente
                MainActivity.beginExperiment();

                // Experiment description
                Mologger.get().setupExperimentLog(param);
                Mologger.get().setLogState(true);
                break;

            case Config.MSSG_BEG_BLK:
                // Get the experiment number
                int blkNum = Integer.parseInt(param);
                Mologger.get().setupBlockLog(blkNum);
                break;

            case Config.MSSG_END_TRL:
                Mologger.get().finishTrialLog();
                break;

            case Config.MSSG_END_BLK:
                Mologger.get().finishBlockLog();
                break;

            case Config.MSSG_END_EXP:
                Mologger.get().setLogState(false);
                break;

            case Config.MSSG_BEG_LOG:
                Actioner.get().isTrialRunning = true;
                break;

            case Config.MSSG_END_LOG:
                Actioner.get().isTrialRunning = false;
                break;

            case Config.NET_DISCONNECT:
                connect();
                break;
            }

        } else {
            Log.d(TAG, "Command not in the right format");
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

    /**
     * Connections task
     */
    @SuppressLint("StaticFieldLeak")
    private class ConnectTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... tasks) {

            Log.d(TAG, "Connecting to Expenvi...");
            long t0 = now();

            while (true) {
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
                    Log.d(TAG, e.toString());
                    Log.d(TAG, "Reconnecting...");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }
//                        return "FAIL";
                }
            }

//            return "FAIL";
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
     * Return the time in ms
     * @return Time (ms)
     */
    private long now() {
        return Calendar.getInstance().getTimeInMillis();
    }

}



