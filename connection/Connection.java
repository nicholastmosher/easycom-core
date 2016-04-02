package com.nicholastmosher.easycom.core.connection;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Nick Mosher on 3/3/2015.
 * @author Nick Mosher
 * nicholastmosher@gmail.com
 * https://github.com/nicholastmosher
 */
public abstract class Connection {

    public static final String TYPE_BLUETOOTH = "connection_type_bluetooth";
    public static final String TYPE_TCPIP = "connection_type_tcpip";
    public static final String TYPE_USB = "connection_type_usb";

    public enum Status {
        Connected,
        Disconnected,
        Connecting,
        ConnectFailed,
        ConnectCanceled,
        MetadataChanged
    }

    /**
     * Connection Adapter for use with the Gson json library for correctly
     * destructing and constructing Connections.
     */
    private static ConnectionTypeAdapter mTypeAdapter =
            new ConnectionTypeAdapter();

    /**
     * The name of this Connection.
     */
    protected String mName;

    /**
     * A unique identifier for this Connection, used as a reliable key
     * for storing and retrieving data from static Maps.
     */
    protected final UUID mUUID;

    /**
     * Keeps track of the status of the connectivity.
     */
    protected Status mStatus = Status.Disconnected;

    /**
     * Constructs a Connection using a given name.  Addresses or
     * connection information are managed by subclasses.
     * @param name The name of the Connection.
     */
    public Connection(String name) {
        if (name == null) {
            System.out.println("Connection has no name!");
            mName = "";
        } else {
            mName = name;
        }
        mUUID = UUID.randomUUID();
    }

    /**
     * Sets the name of this connection.
     * @param name The new name of this connection.
     */
    public void setName(String name) {
        if (name != null) {
            mName = name;
            notifyMetadataChanged();
        } else {
            new NullPointerException("Name is nulL!").printStackTrace();
        }
    }

    /**
     * Returns the name of this connection.
     * @return The name of this connection.
     */
    public String getName() {
        return this.mName;
    }

    /**
     * Returns the unique identifier of this Connection.
     * @return The unique identifier of this Connection.
     */
    public String getUUID() {
        return mUUID.toString();
    }

    /**
     * Send connect request to ConnectionService to open a Connection
     * using this object's data.
     */
    public void connect() {
        if (!(getStatus().equals(Status.Connected))) {

            ConnectionService.getInstance().connect(this);

            //Indicate that this connection's status is now "connecting".
            mStatus = Status.Connecting;
        }
    }

    /**
     * Send disconnect request to ConnectionService to close a Connection
     * using this object's data.
     */
    public void disconnect() {
        if (getStatus().equals(Status.Connected)) {
            ConnectionService.getInstance().disconnect(this);
        }
    }

    /**
     * Sends an intent to ConnectionService with data that should be sent over
     * this connection.
     * @param data    The data to send.
     */
    public void send(byte[] data) {
        ConnectionService.getInstance().send(this, data);
    }

    /**
     * Tells what the status of this connection is.
     * Statuses include:
     * Connected
     * Connecting
     * Disconnected
     * Connect Failed
     * Connect Canceled
     * @return Status of connection.
     */
    public abstract Status getStatus();

    /**
     * Convenience method for use with intent extra "CONNECTION_TYPE".
     * @return The string "connection type" as defined by ConnectionIntent.
     */
    public abstract String getConnectionType();

    /**
     * Returns an InputStream that reads from this Connection's remote source.
     * @return An InputStream that reads from this Connection's remote source.
     * @throws IllegalStateException If this Connection is not connected.
     */
    public abstract InputStream getInputStream() throws IllegalStateException;

    /**
     * Returns an OutputStream that writes to this Connection's remote
     * destination.
     * @return An OutputStream that writes to this Connection's remote
     * destination.
     * @throws IllegalStateException If this Connection is not connected.
     */
    public abstract OutputStream getOutputStream() throws IllegalStateException;

    /**
     * Hashing a connection object will tell if the two objects contain
     * the exact content data, but the same connection - if any
     * member values are changed - will hash differently.  This method
     * is here to compare two connection and determine whether they represent
     * the same connection regardless of the status of the member data.
     * This is determined by comparing the UUIDs of each connection.
     * @param c The connection to compare to this object.
     * @return True if connections are the same, false otherwise.
     */
    public boolean isVersionOf(Connection c) {
        return c.getUUID().equals(this.getUUID());
    }

    /*
     * A listener setup for notifying listening parties about a change in this
     * Connection's metadata (such as name or address change).
     */
    public interface OnMetadataChangedListener {
        void onMetadataChanged(Connection connection);
    }
    private Set<OnMetadataChangedListener> mOnMetadataChangedListeners = new HashSet<>();
    public void addOnMetadataChangedListener(OnMetadataChangedListener listener) {
        mOnMetadataChangedListeners.add(listener);
    }
    private void notifyMetadataChanged() {
        for(OnMetadataChangedListener listener : mOnMetadataChangedListeners) {
            listener.onMetadataChanged(this);
        }
    }

    /*
     * A listener setup for notifying listening parties about a successfully
     * established connection.
     */
    public interface OnConnectListener {
        void onConnect(Connection connection);
    }
    private Set<OnConnectListener> mOnConnectListeners = new HashSet<>();
    public void addOnConnectListener(OnConnectListener listener) {
        mOnConnectListeners.add(listener);
    }
    public void notifyConnect() {
        for(OnConnectListener listener : mOnConnectListeners) {
            listener.onConnect(this);
        }
    }

    /*
     * A listener setup for notifying listening parties about a successfully
     * established connection.
     */
    public interface OnDisconnectListener {
        void onDisconnect(Connection connection);
    }
    private Set<OnDisconnectListener> mOnDisconnectListeners = new HashSet<>();
    public void addOnDisconnectListener(OnDisconnectListener listener) {
        mOnDisconnectListeners.add(listener);
    }
    public void notifyDisconnect() {
        for(OnDisconnectListener listener : mOnDisconnectListeners) {
            listener.onDisconnect(this);
        }
    }

    /*
     * A listener setup for notifying listening parties that some data has been
     * received over this connection.
     */
    public interface OnDataReceivedListener {
        void onDataReceived(Connection connection, byte[] data);
    }
    private Set<OnDataReceivedListener> mOnDataReceivedListeners = new HashSet<>();
    public void addOnDataReceivedListener(OnDataReceivedListener listener) {
        mOnDataReceivedListeners.add(listener);
    }
    public void notifyDataReceived(byte[] data) {
        for(OnDataReceivedListener listener : mOnDataReceivedListeners) {
            listener.onDataReceived(this, data);
        }
    }

    /**
     * Returns a TypeAdapter for Gson to use for Connections.
     * @return A TypeAdapter for Gson to use for Connections.
     */
    public static ConnectionTypeAdapter getTypeAdapter() {
        return mTypeAdapter;
    }

    /**
     * Special class implementing GSON's TypeAdapter.  This is used to tell
     * GSON exactly how to serialize and deserialize Connections.
     */
    private static final class ConnectionTypeAdapter
            extends TypeAdapter<Connection> {

        /**
         * Name key of all Connections.
         */
        public static final String CONNECTION_NAME = "name";
        /**
         * Key of Connection Implementation.
         */
        public static final String CONNECTION_IMP = "imp";
        /**
         * Key of Connection Universal Identifier.
         */
        public static final String CONNECTION_UUID = "uuid";

        //Implementations of Connection
        /**
         * Implementation key of Bluetooth Connections.
         */
        public static final String IMP_BLUETOOTH = "impBt";
        /**
         * Implementation key of TCPIP Connections.
         */
        public static final String IMP_TCPIP = "impTcp";

        //BluetoothConnection specific data
        /**
         * Key to store BluetoothConnection address.
         */
        public static final String BLUETOOTH_ADDRESS = "btAddr";

        //TcpIpConnection specific data
        /**
         * Key to store TcpIp remote Ip.
         */
        public static final String TCPIP_IP = "tcpIp";
        /**
         * Key to store TcpIp remote Port.
         */
        public static final String TCPIP_PORT = "tcpPort";

        /**
         * Takes a Connection and writes it to the JsonWriter as a JSON object.
         * @param writer     The JsonWriter to write the objects into.
         * @param connection The Connection data to convert into JSON.
         * @throws IOException
         */
        @Override
        public void write(JsonWriter writer, Connection connection) throws IOException {

            if (connection == null) {
                throw new NullPointerException("Connection is null!");
            }

            //Begin this Connection
            writer.beginObject();
            writer.name(CONNECTION_NAME).value(connection.getName()); //Write connection name
            writer.name(CONNECTION_UUID).value(connection.getUUID()); //Write connection uuid

            //Write all properties specific to BluetoothConnections.
            if (connection instanceof BluetoothConnection) {
                BluetoothConnection btConnection = (BluetoothConnection) connection;
                writer.name(CONNECTION_IMP).value(IMP_BLUETOOTH); //Write connection implementation
                writer.name(BLUETOOTH_ADDRESS).value(btConnection.getAddress()); //Write BluetoothConnection address

                //Write all properties specific to TcpIpConnections.
            } else if (connection instanceof TcpIpConnection) {
                TcpIpConnection tcpIpConnection = (TcpIpConnection) connection;
                writer.name(CONNECTION_IMP).value(IMP_TCPIP);
                writer.name(TCPIP_IP).value(tcpIpConnection.getServerIp());
                writer.name(TCPIP_PORT).value(tcpIpConnection.getServerPort());
            }

            //End this Connection
            writer.endObject();
        }

        /**
         * Parses data from a JsonReader back into a List of Connections.
         * @param reader The source of a JSON string to convert.
         * @return A Connection parsed from the reader.
         * @throws IOException
         */
        @Override
        public Connection read(JsonReader reader) throws IOException {

            //Create local variables as a cache to build a Connection
            String connectionName = null;
            String imp = null;
            String btAddress = null;
            String tcpIp = null;
            int tcpPort = -1;

            //Begin this Connection
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case CONNECTION_NAME: //Read Connection name
                        connectionName = reader.nextString();
                        break;
                    case CONNECTION_IMP: //Read Connection implementation
                        imp = reader.nextString();
                        break;
                    case BLUETOOTH_ADDRESS: //Read BluetoothConnection address
                        btAddress = reader.nextString();
                        break;
                    case TCPIP_IP:
                        tcpIp = reader.nextString();
                        break;
                    case TCPIP_PORT:
                        tcpPort = reader.nextInt();
                        break;
                    default:
                }
            }
            //End this Connection
            reader.endObject();

            //Parse Connection data into object
            Connection connection = null;
            if (connectionName == null) {
                throw new NullPointerException("Connection name is null!");
            }
            if (imp == null) {
                throw new NullPointerException("Implementation is null!");
            }

            //If this Connection is a BluetoothConnection
            if (imp.equals(IMP_BLUETOOTH)) {
                if (btAddress == null) {
                    throw new NullPointerException("Bluetooth Address is null!");
                }
                connection = new BluetoothConnection(connectionName, btAddress);

                //If this Connection is a TcpIpConnection
            } else if (imp.equals(IMP_TCPIP)) {
                if (tcpIp == null) {
                    throw new NullPointerException("TcpIp IP is null!");
                }
                if (tcpPort == -1) {
                    throw new NullPointerException("Port is null!");
                }
                connection = new TcpIpConnection(connectionName, tcpIp, tcpPort);
            }

            //If the connection is null, something is wrong.
            if (connection == null) {
                throw new NullPointerException("Connection is null!");
            }

            //If we've passed all checks, return the connection.
            return connection;
        }
    }
}
