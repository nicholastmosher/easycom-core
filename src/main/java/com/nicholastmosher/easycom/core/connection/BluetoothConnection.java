package com.nicholastmosher.easycom.core.connection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Nick Mosher on 4/16/15.
 * @author Nick Mosher
 * nicholastmosher@gmail.com,
 * https://github.com/nicholastmosher
 */
public class BluetoothConnection extends Connection {

    /**
     * UUID used for connecting to Serial boards.
     */
    public static final UUID BLUETOOTH_SERIAL_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /**
     * When a Bluetooth Device is discovered, sometimes it will have a name
     * associated with it.  We want to store that, but it may not be the
     * BluetoothConnection's name.
     */
    private String mDeviceName;

    /**
     * The MAC Address of the remote bluetooth device for this Connection.
     */
    private String mAddress;

    /**
     * The Bluetooth Socket used to communicate to the remote device.
     */
    private BluetoothSocket mBluetoothSocket;

    /**
     * Constructs a BluetoothConnection from a name and bluetooth MAC address.
     * @param name    The name of this BluetoothConnection.
     * @param address The MAC Address of the remote device to connect to.
     */
    public BluetoothConnection(String name, String address) {
        super(name);
        if (address == null) {
            throw new NullPointerException("Bluetooth address is null!");
        }
        mAddress = address;
    }

    /**
     * Tells whether this BluetoothConnection is actively connected.
     * @return True if connected, false otherwise.
     */
    @Override
    public Status getStatus() {

        //If we know we're trying to connect to something.
        if (mStatus.equals(Status.Connecting)) return Status.Connecting;

        //If not in the process of connecting, verify active connections.
        if (mBluetoothSocket != null) {
            if (!mBluetoothSocket.isConnected()) {
                try {
                    //Closing a socket really "should" never throw an error.
                    mBluetoothSocket.close();
                } catch (IOException e) {
                    System.out.println("Bluetooth socket not connected; error closing socket!");
                    e.printStackTrace();
                }
            }
            return mBluetoothSocket.isConnected() ?
                    Status.Connected : Status.Disconnected;
        }
        return Status.Disconnected;
    }

    /**
     * Convenience method for use with intent extra "CONNECTION_TYPE".
     * @return The string "connection type" as defined by ConnectionIntent.
     */
    @Override
    public String getConnectionType() {
        return TYPE_BLUETOOTH;
    }

    /**
     * Retrieves the Input Stream if this Connection is connected and
     * the Input Stream is not null.
     * @return The InputStream from the remote bluetooth device.
     * @throws java.lang.IllegalStateException If not connected.
     */
    @Override
    public InputStream getInputStream() throws IllegalStateException {
        if (getStatus().equals(Status.Connected)) {
            try {
                return mBluetoothSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            throw new IllegalStateException("Connection is not active!");
        }
        return null;
    }

    /**
     * Retrieves the Output Stream if this Connection is connected and
     * the Output Stream is not null.
     * @return The OutputStream to the remote bluetooth device.
     * @throws java.lang.IllegalStateException If not connected.
     */
    @Override
    public OutputStream getOutputStream() throws IllegalStateException {
        if (getStatus().equals(Status.Connected)) {
            try {
                return mBluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            throw new IllegalStateException("Connection is not active!");
        }
        return null;
    }

    /**
     * Sets the bluetooth address of this Connection.
     * @param address The MAC bluetooth address of the remote device.
     */
    public void setAddress(String address) {
        if (address != null) {
            if (BluetoothAdapter.checkBluetoothAddress(address)) {
                mAddress = address;
            } else {
                new IllegalArgumentException("Bluetooth address is not valid!")
                        .printStackTrace();
            }
        } else {
            new NullPointerException("Bluetooth address is null!")
                    .printStackTrace();
        }
    }

    /**
     * Returns the name of the Bluetooth Device as it was discovered.
     * @return The name of the Bluetooth Device as it was discovered.
     */
    public String getDeviceName() {
        return mDeviceName;
    }

    /**
     * Gets the Bluetooth MAC Address of the remote device of this
     * BluetoothConnection.
     * @return A Bluetooth MAC Address.
     */
    public String getAddress() {
        return mAddress;
    }

    /**
     * Assigns the BluetoothSocket for this BluetoothConnection.
     * @param socket New BluetoothSocket.
     */
    void setBluetoothSocket(BluetoothSocket socket) {
        if (socket == null) {
            System.out.println("Socket is null!");
            return;
        }
        mBluetoothSocket = socket;
    }

    /**
     * Gets this BluetoothConnection's BluetoothSocket if it exists.
     * @return This BluetoothSocket.
     * @throws java.lang.NullPointerException If BluetoothSocket is null.
     */
    public BluetoothSocket getBluetoothSocket() throws NullPointerException {
        if (mBluetoothSocket != null) {
            return mBluetoothSocket;
        } else {
            throw new NullPointerException("Bluetooth Socket is null!");
        }
    }

    /**
     * Returns a String representation of this connection.
     * @return A String representation of this connection.
     */
    @Override
    public String toString() {
        return mName + ", " + mAddress;
    }
}