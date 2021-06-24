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
                        processInput(line);
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
     * Subscribe to get the messages (to send to the desktop)
     * @param mssgPublisher Publisher of String messages
     */
    public void subscribeToMessages(PublishSubject<String> mssgPublisher) {
        mssgPublisher
                .observeOn(Schedulers.io())
                .subscribe(actionStr -> {
                    sendAction(actionStr);
                });
    }

    /**
     * Connect to Empenvi
     */
    public void connect() {
        // Connect to server (runs once)
        new ConnectTask().execute();
    }

    /**
     * Process the input command
     * @param inStr Input String
     */
    private void processInput(String inStr) {
        Log.d(TAG, "Process: " + inStr);
        // Command must be in the format <mssg-param>
        // Message and param
        String mssg = Utils.splitStr(inStr)[0];
        String param = Utils.splitStr(inStr)[1];

        // Command
        switch (mssg) {
        case Strs.MSSG_PID: // Start of a participant
            Mologger.get().loginParticipant(param);
            break;
        case Strs.MSSG_TECHNIQUE: // Set the technique
            Actioner.get().setTechnique(param);
            break;
        case Strs.MSSG_BEG_PHS: // Start of the phase
            int phase = Integer.parseInt(param);
            Mologger.get()._phase = phase;
            // Don't log during the Showcase
            if (phase == 0) Mologger.get().isLogging = false;
            else Mologger.get().isLogging  = true;
            break;
        case Strs.MSSG_SUBBLOCK: // Subblock number
            Mologger.get()._subblockNum = Integer.parseInt(param);
            break;
        case Strs.MSSG_TRIAL: // Trial number
            Mologger.get()._trialNum = Integer.parseInt(param);
            break;
        case Strs.MSSG_END_EXP:
            Mologger.get().isLogging = false;
            break;
        case Strs.NET_DISCONNECT:
            connect();
            break;
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
     * Connections task (Background)
     */
    @SuppressLint("StaticFieldLeak")
    private class ConnectTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... tasks) {

            Log.d(TAG, "Connecting to Expenvi...");

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
                    outChannel.println(Strs.MSSG_MOOSE);
                    String line = inChannel.readLine();
                    Log.d(TAG, "Line: " + line);
                    if (Objects.equals(line, Strs.MSSG_CONFIRM)) { // Confirmation
                        Log.d(TAG, "Connection Successful!");
                        return "SUCCESS";
                    }

                    return "FAIL";

                } catch (IOException e) {
//                    e.printStackTrace();
                    Log.d(TAG, e.toString());
                    Log.d(TAG, "Reconnecting...");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }
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
                // Try to reconnect
                connect();
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



