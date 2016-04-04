package com.nicholastmosher.easycom.core.connection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Nick Mosher on 9/24/15.
 * A class for handling Connection data, including
 * checking for version duplicates and handling observable interactions.
 *
 * @author Nick Mosher, nicholastmosher@gmail.com, https://github.com/nicholastmosher
 */
public class ConnectionModel extends HashMap<UUID, Connection> {

    private static ConnectionModel SINGLETON;

    /**
     * A convenience method to provide singleton access to a ConnectionModel
     * while still allowing full object-oriented instance freedom.
     * @return A singleton instance of the ConnectionModel.
     */
    public static ConnectionModel getModel() {
        if(SINGLETON == null) {
            SINGLETON = new ConnectionModel();
        }
        return SINGLETON;
    }

    /**
     * Adds the given device to this model.  If no version of the
     * device already exists, it is added as a new entry.  If a
     * previous version of the device does exist, replace it.
     *
     * @param newConnection The device to add or update.
     * @return The version of the connection that was replaced.
     */
    @Override
    public Connection put(UUID key, Connection newConnection) {

        //Iterate over all existing connections to check for duplicate versions.
        for(Connection connection : values()) {

            //If the connection is a version of an existing one, replace it.
            if(newConnection.isVersionOf(connection)) {

                super.put(key, newConnection);
                notifyModelUpdated();
                return connection;
            }
        }

        //If the new connection was not already in the model, add it now.
        super.put(key, newConnection);
        notifyModelUpdated();
        return null;
    }

    /**
     * Returns a connection from this model with the given UUID key.
     * @param key The UUID of the connection in String form.
     * @return The connection with the given UUID.
     */
    public Connection get(String key) {
        return super.get(UUID.fromString(key));
    }

    /**
     * Checks if the object given matches a version of a Device
     * in this list.  If so, it removes it from the model.
     *
     * @param key The object to check against this list.
     * @return The connection previously associated with the key, or null if there was none.
     */
    public Connection remove(Object key) {
        if(!(key instanceof Connection)) {
            return null;
        }
        Connection connection = (Connection) key;
        for(Connection c : values()) {
            if(connection.isVersionOf(c)) {
                Connection removed = super.remove(connection);
                if(removed != null) notifyModelUpdated();
                return removed;
            }
        }
        return null;
    }

    public interface OnModelUpdatedListener { void onModelUpdated(ConnectionModel model); }
    private Set<OnModelUpdatedListener> mOnModelUpdatedListeners = new HashSet<>();
    public void addOnModelUpdatedListener(OnModelUpdatedListener listener) {
        mOnModelUpdatedListeners.add(listener);
    }
    private void notifyModelUpdated() {
        for(OnModelUpdatedListener listener : mOnModelUpdatedListeners)
            listener.onModelUpdated(this);
    }

    /**
     * Returns the String description of each connection.
     *
     * @return The String description of each connection.
     */
    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();
        for(Entry<UUID, Connection> entry : entrySet()) {
            string.append(entry.getKey().toString()).append(" : ").append(entry.getValue().toString()).append("\n");
        }
        return string.toString();
    }
}
