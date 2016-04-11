package com.nicholastmosher.easycom.core.connection;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Nick Mosher on 4/23/15.
 * The ConnectionService handles all heavy networking operations with Connections
 * such as connecting, disconnecting, sending, and receiving data.  The
 * ConnectionService multiplexes tasks with all types of Connections, as well as
 * multiple instances of the same type of Connection (e.g. a TcpIpConnection and
 * a BluetoothConnection, or multiple BluetoothConnections, or any combination).
 * <p/>
 * All data flowing into and out of the ConnectionService is passed through custom
 * intents, with data Extras assigned via keys located in ConnectionIntent.  Most
 * typical operations such as Connecting, Disconnecting, Sending, and Receiving
 * have custom intents representing them (ConnectIntent, DisconnectIntent,
 * SendIntent, and ReceiveIntent, respectively).
 * @author Nick Mosher, nicholastmosher@gmail.com, https://github.com/nicholastmosher
 */
public class ConnectionService extends Service {

    /**
     * Allows for one task per connection to be active.  For example, a DisconnectTask
     * issued while a ConnectTask is executing on a connection will need to first
     * interrupt the ConnectTask before it can be active.  If the active Task finishes,
     * it removes itself from this Map.
     */
    private final Map<Connection, AsyncTask<Connection, Void, Boolean>> mTasks = new HashMap<>();

    private static final String TAG = "ConnectionService";

    private static boolean launched = false;

    /**
     * Instantiated on the first onCreate of this service. Entities
     * can then reference the created instance by calling getInstance();
     */
    private static ConnectionService SINGLETON;

    private UsbManager mUsbManager;

    /**
     * Launches the ConnectionService if it is not already active.
     * @param context The context to launch the Service from.
     */
    public static void launch(Context context) {
        if(!launched) {
            if(context == null) {
                Log.w(TAG, "Context is null.");
                return;
            }
            context.startService(new Intent(context, ConnectionService.class));
        }
    }

    /**
     * @return The active instance of this service.
     */
    public static ConnectionService getInstance() {
        return SINGLETON;
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        SINGLETON = this;
        launched = true;
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        /*
         * This receiver listens for an Android system broadcast
         * notifying about a newly attached USB device.  When a valid device
         * is attached, create a new UsbConnection and pass it wherever it's
         * needed.
         */
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                System.out.println("USB Device Attached!");
                System.out.println("USB Name: " + usbDevice.getDeviceName());
                System.out.println("USB Class: " + usbDevice.getDeviceClass());
                System.out.println("USB ID: " + usbDevice.getDeviceId());
                System.out.println("USB Protocol: " + usbDevice.getDeviceProtocol());
                System.out.println("USB Subclass: " + usbDevice.getDeviceSubclass());
            }
        }, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));

        /*
         * This receiver listens for an Android system broadcast
         * notifying that a USB device has been detached.  When this happens,
         * we need to close any open connections we may have had with that
         * device.
         */
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Detach here
            }
        }, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));

        return Service.START_STICKY;
    }

    public void onDestroy() {
        launched = false;
    }

    /**
     * Queues the given connection to be connected.
     * @param connection The connection to connect.
     */
    public void connect(Connection connection) {

        if(connection == null) {
            Log.w(TAG, "Connection is null.");
            return;
        }

        switch(connection.getConnectionType()) {
            case Connection.TYPE_BLUETOOTH:
                setTask(connection, new ConnectBluetoothTask());
                break;
            case Connection.TYPE_TCPIP:
                setTask(connection, new ConnectTcpIpTask());
                break;
            case Connection.TYPE_USB:
                Log.i(TAG, "Usb not implemented yet.");
                break;
            default:
                Log.w(TAG, "Connection " + connection.toString() + " is not a valid type.");
        }
    }

    /**
     * Queues the given connection to be disconnected.
     * @param connection The connection to be disconnected.
     */
    public void disconnect(Connection connection) {

        if(connection == null) {
            Log.w(TAG, "Connection is null.");
            return;
        }

        switch(connection.getConnectionType()) {
            case Connection.TYPE_BLUETOOTH:
                setTask(connection, new DisconnectBluetoothTask());
                break;
            case Connection.TYPE_TCPIP:
                setTask(connection, new DisconnectTcpIpTask());
                break;
            case Connection.TYPE_USB:
                Log.i(TAG, "Usb not implemented yet.");
                break;
            default:
                Log.w(TAG, "Connection " + connection.toString() + " is not a valid type.");
        }
    }

    /**
     * Queues data to be sent over the given connection. The queues are
     * asynchronous but guaranteed to be ordered.
     * @param connection The connection to send data over.
     * @param data The data to send.
     */
    public void send(Connection connection, byte[] data) {

        if(connection == null) {
            Log.w(TAG, "Connection is null.");
            return;
        }

        switch(connection.getConnectionType()) {
            case Connection.TYPE_BLUETOOTH:
                TransferManager.getManager(connection).postSendTask(new SendTask(connection, data));
                break;
            case Connection.TYPE_TCPIP:
                TransferManager.getManager(connection).postSendTask(new SendTask(connection, data));
                break;
            case Connection.TYPE_USB:

                break;
            default:
                Log.w(TAG, "Connection " + connection.toString() + " is not a valid type.");
        }
    }

    /**
     * Sets and executes a new active task for the given connection, interrupting any existing
     * ones in the process.
     * @param connection The connection to set this task for.
     * @param task       The task to assign to the connection.
     */
    private void setTask(Connection connection, AsyncTask<Connection, Void, Boolean> task) {

        //Null safety check connection and task.
        if(connection == null) {
            Log.w(TAG, "Connection is null.");
            return;
        }
        if(task == null) {
            Log.w(TAG, "Task is null.");
            return;
        }

        synchronized(mTasks) {
            //If the connection already has a task running, cancel it and remove it.
            if (mTasks.containsKey(connection)) {
                AsyncTask<Connection, Void, Boolean> asyncTask = mTasks.get(connection);
                //Null safety check the existing task.
                if (asyncTask != null) {
                    asyncTask.cancel(true);
                }

                //Remove the connection/task entry from the map.
                mTasks.remove(connection);
            }

            //Add and launch the new task for the connection.
            mTasks.put(connection, task);
            task.execute(connection);
        }
    }

    /**
     * Opens an asynchronous task that does not run on the UI thread
     * to handle opening BluetoothConnections.
     * Usage: new ConnectBluetoothTask(myBluetoothConnection).execute();
     */
    private class ConnectBluetoothTask extends AsyncTask<Connection, Void, Boolean> {

        private BluetoothConnection mConnection;
        private BluetoothAdapter mBluetoothAdapter;
        private BluetoothSocket mBluetoothSocket;
        private int retryCount;

        private ConnectBluetoothTask(int retry) {
            retryCount = retry;
        }

        public ConnectBluetoothTask() {
            this(0);
        }

        /**
         * This method runs on a separate, non-UI thread.  Heavy lifting goes here.
         * @return True if connecting succeeded, false if it failed.
         */
        protected Boolean doInBackground(Connection... params) {

            //Perform connection null and type safety checks.
            Connection temp = params[0];
            if(temp == null) {
                new NullPointerException("Connection is null!").printStackTrace();
                return false;
            }
            if(!(temp instanceof BluetoothConnection)) {
                new IllegalArgumentException("Connection is not a BluetoothConnection!").printStackTrace();
                return false;
            }
            mConnection = (BluetoothConnection) temp;

            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            //Check if BT is enabled
            if(!mBluetoothAdapter.isEnabled()) {
                new IllegalStateException("Cannot connect, Bluetooth is disabled!").printStackTrace();
            }
            System.out.println("BluetoothAdapter Enabled...");

            //Define a BluetoothDevice with the address from our Connection.
            String address = mConnection.getAddress();
            BluetoothDevice device;
            if(address != null && BluetoothAdapter.checkBluetoothAddress(address)) {
                device = mBluetoothAdapter.getRemoteDevice(address);
                System.out.println("Bluetooth Device parsed from BluetoothAdapter...");
            } else {
                new IllegalStateException("Error connecting to bluetooth! Problem with address.").printStackTrace();
                return false;
            }

            //Try to retrieve a BluetoothSocket from the BluetoothDevice.
            try {
                mBluetoothSocket = device.createRfcommSocketToServiceRecord(BluetoothConnection.BLUETOOTH_SERIAL_UUID);
                System.out.println("BluetoothSocket retrieved from Bluetooth Device...");
            } catch(IOException e) {
                e.printStackTrace();
                return false;
            }

            //Shouldn't need to be discovering at this point.
            mBluetoothAdapter.cancelDiscovery();

            //Attempt to connect to the bluetooth device and receive a BluetoothSocket
            try {
                mBluetoothSocket.connect();
                mConnection.setBluetoothSocket(mBluetoothSocket);
                mConnection.notifyConnect();
                System.out.println("BluetoothSocket connected, success!");
            } catch(IOException e) {
                e.printStackTrace();
                try {
                    mBluetoothSocket.close();
                } catch(IOException e2) {
                    e2.printStackTrace();
                }
                return false;
            }

            //If we've made it this far, must have been a success.
            return true;
        }

        /**
         * Result method that runs on the UI thread.  Background thread reports
         * to this thread when it's finished.
         * @param success Whether the background thread succeeded or failed.
         */
        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if(success) {
                System.out.println("Connected success");

                //Create a TransferManager to handle actual data to/from the connection.
                new TransferManager(mConnection);

                //Notify connection that it's connected.
                mConnection.notifyConnect();
            } else {
                System.out.println("Connected failed");
                if(mBluetoothSocket.isConnected()) {
                    System.out.println("WARNING: ConnectBluetoothTask reported error, but is connected.");
                    mConnection.notifyConnect();
                } else {
                    if(retryCount < 3) {
                        retryCount++;
                        System.out.println("Error connecting! Retrying... (retry " + retryCount + ").");
                        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        mBluetoothSocket = null;
                        setTask(mConnection, new ConnectBluetoothTask(retryCount));
                    } else {
                        retryCount = 0;
                        System.out.println("Error connecting, Aborting!");
                        mConnection.notifyConnect();
                    }
                }
            }
        }
    }

    /**
     * Uses an asynchronous task not on the UI thread to close BluetoothConnections.
     * Usage: new DisconnectBluetoothTask(myBluetoothConnection).execute();
     */
    private class DisconnectBluetoothTask extends AsyncTask<Connection, Void, Boolean> {

        private BluetoothConnection mConnection;

        @Override
        protected Boolean doInBackground(Connection... params) {

            //Perform connection null and type safety checks.
            Connection temp = params[0];
            if(temp == null) {
                new NullPointerException("Connection is null!").printStackTrace();
                return false;
            }
            if(!(temp instanceof BluetoothConnection)) {
                new IllegalArgumentException("Connection is not a BluetoothConnection!").printStackTrace();
                return false;
            }
            mConnection = (BluetoothConnection) temp;

            if(mConnection.getStatus().equals(Connection.Status.Connected)) {
                try {
                    mConnection.getBluetoothSocket().close();
                    return true;
                } catch(IOException e) {
                    e.printStackTrace();
                    new IllegalStateException("Error closing BT socket at disconnect!").printStackTrace();
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if(!mConnection.getStatus().equals(Connection.Status.Connected)) {

                //Find the TransferManager for this connection and close it.
                TransferManager manager = TransferManager.getManager(mConnection);
                if(manager != null) {
                    manager.close();
                }

                //Notify connection of disconnect.
                mConnection.notifyDisconnect();
            }
        }
    }

    /**
     * Uses an asynchronous task not on the UI thread to open a TCPIP connection.
     * Usage: new ConnectTcpIpTask(myTcpIpConnection).execute();
     * FIXME Redo the retry system. Shit's scary
     */
    private class ConnectTcpIpTask extends AsyncTask<Connection, Void, Boolean> {

        private TcpIpConnection mConnection;
        private Socket mSocket;
        private int retryCount;

        private ConnectTcpIpTask(int retry) {
            retryCount = retry;
        }

        public ConnectTcpIpTask() {
            this(0);
        }

        @Override
        protected Boolean doInBackground(Connection... params) {

            //Perform connection null and type safety checks.
            Connection temp = params[0];
            if(temp == null) {
                new NullPointerException("Connection is null!").printStackTrace();
                return false;
            }
            if(!(temp instanceof TcpIpConnection)) {
                new NullPointerException("Connection is not a TCP/IP Connection!").printStackTrace();
                return false;
            }
            mConnection = (TcpIpConnection) temp;

            try {
                System.out.println("Connecting to " + mConnection.getServerIp() + ":" + mConnection.getServerPort());
                mSocket = new Socket(mConnection.getServerIp(), mConnection.getServerPort());
                mConnection.setSocket(mSocket);
            } catch(IOException ioe) {
                ioe.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if(success) {
                System.out.println("Connected success");

                //Create a TransferManager to handle actual data to/from the connection.
                new TransferManager(mConnection);

                //Notify connection that it's connected.
                mConnection.notifyConnect();
            } else if(mSocket != null) {
                System.out.println("Connected failed");
                if(mSocket.isConnected()) {
                    System.out.println("WARNING: ConnectBluetoothTask reported error, but is connected.");
                    mConnection.notifyConnect();
                } else {
                    if(retryCount < 3) {
                        retryCount++;
                        System.out.println("Error connecting! Retrying... (retry " + retryCount + ").");
                        mSocket = null;
                        setTask(mConnection, new ConnectTcpIpTask());
                    } else {
                        retryCount = 0;
                        System.out.println("Error connecting, Aborting!");
                        mConnection.notifyConnect();
                    }
                }
            } else {
                System.err.println("Error, TcpIp socket is null");
                mConnection.notifyConnect();
            }
        }
    }

    /**
     * Uses an asynchronous task not on the UI thread to close a TcpIpConnection.
     * Usage: new DisconnectTcpIpTask(myTcpIpConnection).execute();
     */
    private class DisconnectTcpIpTask extends AsyncTask<Connection, Void, Boolean> {

        private TcpIpConnection mConnection;

        @Override
        protected Boolean doInBackground(Connection... params) {

            //Perform connection null and type safety checks.
            Connection temp = params[0];
            if(temp == null) {
                new NullPointerException("Connection is null!").printStackTrace();
                return false;
            }
            if(!(temp instanceof TcpIpConnection)) {
                new IllegalArgumentException("Connection is not a TCP/IP Connection").printStackTrace();
                return false;
            }
            mConnection = (TcpIpConnection) temp;

            if(mConnection.getStatus().equals(Connection.Status.Connected)) {
                try {
                    mConnection.getSocket().close();
                    return true;
                } catch(IOException ioe) {
                    ioe.printStackTrace();
                    throw new IllegalStateException("Error closing socket at disconnect!");
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean param) {
            super.onPostExecute(param);
            if(!mConnection.getStatus().equals(Connection.Status.Connected)) {

                //Find the TransferManager for this connection and close it.
                TransferManager manager = TransferManager.getManager(mConnection);
                if(manager != null) {
                    manager.close();
                }

                //Notify connection of disconnect.
                mConnection.notifyDisconnect();
            }
        }
    }

    /**
     * Uses an asynchronous task not on the UI thread to open a UsbHostConnection.
     * Usage: new ConnectUsbTask(myUsbHostConnection).execute();
     */
    private class ConnectUsbTask extends AsyncTask<Connection, Void, Boolean> {

        private UsbHostConnection mConnection;

        @Override
        protected Boolean doInBackground(Connection... params) {

            //Perform connection null and type safety checks.
            Connection temp = params[0];
            if(temp == null) {
                new NullPointerException("Connection is null!").printStackTrace();
                return false;
            }
            if(!(temp instanceof UsbHostConnection)) {
                new IllegalArgumentException("Connection is not a UsbHostConnection!").printStackTrace();
                return false;
            }
            mConnection = (UsbHostConnection) temp;

            UsbDevice usbDevice = mConnection.getUsbDevice();
            UsbInterface usbInterface = usbDevice.getInterface(0);
            UsbDeviceConnection usbDeviceConnection = mUsbManager.openDevice(usbDevice);

            //Force-claim the interface for this usb connection.  Will release at disconnect.
            usbDeviceConnection.claimInterface(usbInterface, true);

            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
        }
    }

    /**
     * Uses an asynchronous task not on the UI thread to close a UsbHostConnection.
     * Usage: new DisconnectUsbTask(myUsbHostConnection).execute();
     */
    private class DisconnectUsbTask extends AsyncTask<Connection, Void, Boolean> {

        private UsbHostConnection mConnection;

        @Override
        protected Boolean doInBackground(Connection... params) {

            //Perform connection null and type safety checks.
            Connection temp = params[0];
            if(temp == null) {
                new NullPointerException("Connection is null!").printStackTrace();
                return false;
            }
            if(!(temp instanceof UsbHostConnection)) {
                new IllegalArgumentException("Connection is not a UsbHostConnection!").printStackTrace();
                return false;
            }
            mConnection = (UsbHostConnection) temp;

            UsbDevice usbDevice = mConnection.getUsbDevice();
            UsbInterface usbInterface = usbDevice.getInterface(0);
            UsbDeviceConnection usbDeviceConnection = mUsbManager.openDevice(usbDevice);

            //Releases our claim on this usb connection.
            usbDeviceConnection.releaseInterface(usbInterface);

            return true;
        }
    }

    /**
     * A new TransferManager is created every time a new Connection is connected.
     * The TransferManager is responsible for asynchronously watching the connection
     * for incoming data and sending data in a nonblocking, sequence-safe manner.
     */
    private static class TransferManager {

        public static final String HANDLER_NAME = "Send Thread";

        private static final Map<Connection, TransferManager> MANAGERS = new HashMap<>();

        private ReceiveTask mReceiver;
        private HandlerThread mSendThread;
        private Handler mSendHandler;

        /**
         * Creates a TransferManager with a new ReceiveThread based on the
         * given connection.
         * @param connection The connection to manage.
         */
        public TransferManager(Connection connection) {
            this(connection, null);
        }

        /**
         * Creates a TransferManager with an existing ReceiveThread based on
         * the given connection.
         * @param connection The connection to manage.
         * @param receiver   The existing receiver to use.
         */
        public TransferManager(Connection connection, ReceiveTask receiver) {
            if(connection == null) {
                throw new NullPointerException("Connection is null!");
            }

            //Register this TransferManager with this connection.
            MANAGERS.put(connection, this);

            //Initialize receiver safely.
            if(receiver == null) {
                mReceiver = new ReceiveTask(connection);
            } else {
                mReceiver = receiver;
            }
            openReceiver();

            //Initialize send thread.
            mSendThread = new HandlerThread(HANDLER_NAME);
            mSendThread.start();
            mSendHandler = new Handler(mSendThread.getLooper());
        }

        /**
         * Returns the TransferManager for the given connection.
         * @param connection The connection to retrieve the manager of.
         * @return The TransferManager.
         */
        public static TransferManager getManager(Connection connection) {
            return MANAGERS.get(connection);
        }

        /**
         * Launches the ReceiverThread associated with this Connection and starts
         * listening for incoming data.
         */
        public void openReceiver() {
            mReceiver.execute();
        }

        /**
         * Posts a new SendTask to the handler queue to be asynchronously
         * but sequentially sent.
         * @param sendTask The SendTask to execute.
         */
        public void postSendTask(SendTask sendTask) {
            System.out.println("POSTING SEND TASK");
            if(sendTask == null) {
                new NullPointerException("SendTask is null!").printStackTrace();
                return;
            }
            mSendHandler.post(sendTask);
        }

        /**
         * Closes the TransferManager by interrupting the ReceiveThread and all
         * SendThreads.
         */
        public void close() {
            mReceiver.cancel(true);
            mSendThread.interrupt();
        }
    }

    /**
     * Uses an asynchronous task not on the UI thread to send data over a Connection.
     * Usage: new SendTask(myConnection, myData).execute();
     */
    private class SendTask implements Runnable {

        private Connection mConnection;
        private byte[] mData;
        private boolean mError = false;

        public SendTask(Connection connection, byte[] data) {

            //If the connection or data is null, set a flag so we skip everything in run().
            if(connection == null) {
                new NullPointerException("Connection is null!").printStackTrace();
                mError = true;
            } else if(data == null) {
                new NullPointerException("Data is null!").printStackTrace();
                mError = true;
            }
            mConnection = connection;
            mData = data;
        }

        public void run() {
            //If there was an error indicated in the constructor, don't try anything.
            if(mError) {
                return;
            }

            if(mConnection.getStatus().equals(Connection.Status.Connected)) {
                try {
                    mConnection.getOutputStream().write(mData);
                } catch(IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Connection is not connected!");
            }
        }
    }

    /**
     * A ReceiveTask is spawned when a new Connection is successfully established.
     * This task is responsible for monitoring incoming data over the given
     * connection and notifying that connection's listeners when data is received.
     * This functionality is implemented using an AsyncTask rather than a Thread
     * for a specific reason. When we receive data and it's time to notify all of
     * the listeners, those listeners may want to make UI updates. However, all
     * threads other than the main UI thread are forbidden to touch UI objects, so
     * the AsyncTask passes received values to methods in the UI thread which then
     * calls all of the listeners.
     */
    private static class ReceiveTask extends AsyncTask<Void, byte[], Void> {

        private Connection mConnection;
        private boolean isRunning;

        /**
         * Create a new ReceiveThread that watches the given connection.
         * @param connection The connection to receive data from.
         */
        public ReceiveTask(Connection connection) {
            if(connection == null) {
                throw new NullPointerException("Connection is null!");
            }
            mConnection = connection;
            isRunning = true;
        }

        /**
         * Wait for data to be received from the given connection.
         * @param params Filler for AsyncTask generic requirement.
         * @return No value. Filler for AsyncTask generic requirement.
         */
        @Override
        protected Void doInBackground(Void... params) {
            Log.v(TAG, "Begin receive task for connection " + mConnection.getName() + ".");
            InputStream input = null;
            while((mConnection.getStatus().equals(Connection.Status.Connected)) && isRunning) {

                try {
                    if(input == null) {
                        input= mConnection.getInputStream();
                        if(input == null) {
                            throw new NullPointerException("InputStream is null.");
                        }
                    }

                    while(input.available() > 0) {
                        int in = input.read();
                        if(in == -1) {
                            throw new IOException("Input stream returned -1.");
                        }
                        publishProgress(new byte[]{(byte) in});
                    }

                } catch(IOException e) {
                    //Happens if the bufferedReader's stream is closed.
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(50);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                    isRunning = false;
                }
            }
            return null;
        }

        /**
         * When we receive data, we must notify the listeners from a scope
         * within the UI thread.
         * @param values The data received from this connection.
         */
        @Override
        protected void onProgressUpdate(byte[]... values) {
            super.onProgressUpdate(values);
            mConnection.notifyDataReceived(values[0]);
        }
    }
}