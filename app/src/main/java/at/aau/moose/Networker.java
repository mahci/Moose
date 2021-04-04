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

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class Networker {

    private final String TAG = "Networker";

    private static Networker self; // For singleton

    private Socket socket;
    private PrintStream outChannel;
    private BufferedReader inChannel;

    private final @NonNull Observable<Object> receiverOberservable;
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

        // Create receiverObserable (doesn't start until after subscription)
        receiverOberservable = Observable.fromAction(new Action() {

            @Override
            public void run() throws Throwable {
                Log.d(TAG, "Receiving data from server...");
                // Continously read lines from server until DISCONNECT is received
                String line;
                do {
                    line = inChannel.readLine();
                    // Got the begin line message
                    Log.d(TAG, "Server Message: " + line);
                    if (Objects.equals(line, Const.MSSG_BEG_EXP)) {
                        // Tell the MainActivity to save the initial state
                        MainActivity.beginExperiment();
                    }
                    else if (Objects.equals(line, Const.MSSG_BEG_LOG)) { // Start of logging
                        Mologger.get().setLogState(true);
                    }
                    else if (Objects.equals(line, Const.MSSG_END_LOG)) { // End of logging
                        Mologger.get().setLogState(false);
                    }

                } while(!Objects.equals(line, Const.NET_DISCONNECT));
            }
        }).subscribeOn(Schedulers.io());

        // Connect to server (runs once)
        new NetTask().execute(Const.NET_CONNECT);

    }

    /**
     * Subscribe to actions PublishingSubject
     * @param actSubject PublishingSubject
     */
//    public void subscribeToActions(PublishSubject<TouchEvent> actSubject) {
//        actSubject
//                .observeOn(Schedulers.io())
//                .subscribe(touchEvent -> {
//                    // Send the respective message
//                    String mssg = eventToMessage(touchEvent);
//                    if (outChannel != null) {
//                        outChannel.println(mssg);
//                        outChannel.flush();
//                        Log.d(TAG, mssg + " sent to server");
//                    } else {
//                        Log.d(TAG, "Channel to server not opened!");
//                    }
//
//                });
//    }

    /**
     * Get the logging Subject
     * @return PublishSubject<Boolean> logSubject
     */
//    public PublishSubject<Boolean> getLogSubject() { return logSubject;}

    /**
     * Connections task
     */
    @SuppressLint("StaticFieldLeak")
    private class NetTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... tasks) {

            // Run the appropriate action
            switch(tasks[0]) {
                case Const.NET_CONNECT:

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

                    break;
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
     * Send an action string to the DSKMoose
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

    /**
     * Get the message from a TouchEvent
     * @param te TouchEvent
     * @return Message for server
     */
    private String eventToMessage(TouchEvent te) {
//        if (te.getFinger() == Constants.FINGER.LEFT) { // Primary tap
//            if (te.getActType() == Constants.ACT.PRESS) {
//                return Constants.ACT_PRESS_PRI;
//            } else {
//                return Constants.ACT_RELEASE_PRI;
//            }
//        } else { // Secondary tap
//            if (te.getActType() == Constants.ACT.PRESS) {
//                return Constants.ACT_PRESS_SEC;
//            } else {
//                return Constants.ACT_RELEASE_SEC;
//            }
//        }
        return "";
    }


}


