# Easycom Core

Easycom core is an Android library built to simplify the process of
establishing a remote connection to an external device. Currently supported are
Bluetooth and TCP/IP serial connections. The library offloads all concerns of
Activity/Service communication, multithreading, and allows connection types to
be switched out without changing application behavior.

## Installing Easycom Core

To use easycom core in your app, add the following to the `build.gradle` file
in your app's module.

```Gradle
repositories {
	maven {
		url 'https://dl.bintray.com/nicholastmosher/maven'
	}
}

dependencies {
	compile 'com.nicholastmosher.easycom.core:easycom-core:0.0.1'
}
```

If you already see `repositories` and/or `maven`, just add the url inside those
blocks, same for `dependencies`.

## Using Easycom Core

At the center of the easycom core library are the Connections. A Connection
represents a single relationship between your Android and the remote device
which you're connecting to. The `Connection` class is abstract, so while all
functionality is provided by subclasses such as `BluetoothConnection` and
`TcpIpConnection`, you can manage and manipulate Connections polymorphicly.

Connection have four basic operations:

* Connecting
* Sending Data
* Receiving Data
* Disconnecting

### Launch the ConnectionService

In Android, Services allow tasks to be run without being attached to a single
Activity. Easycom core uses a service to manage all of the connections so that
they persist even while the user navigates between Activities. This service must
be launched before any attempt to initiate or transfer over connections. To
launch the `ConnectionService`, place the following code into your main Activity's
`onCreate` moethod:

```Java
ConnectionService.launch(getApplicationContext());
```

### Constructing Connections

#### Bluetooth

To construct a `BluetoothConnection`, do the following:

```Java
Connection myConnection = new BluetoothConnection("Device Name", "XX:XX:XX:XX:XX:XX");
```

Where `XX:XX:XX:XX:XX:XX` is the MAC address of the bluetooth device you want
to connect to. Note that this library does not take care of discovering
bluetooth devices, currently you must do that yourself.

#### TCP/IP

To construct a `TcpIpConnection`, do the following:

```Java
Connection myConnection = new TcpIpConnection("Device Name", "device.address", XXXXX);
```

Where `device.address` is the hostname or ip address of the remote device, and
`XXXXX` is the port.

In each case, `Device Name` does not have any impact on the connection process,
but allows for easy organization or labeling for displaying them in the UI.

### Connecting

The Connection object only represents how to connect to a device, but constructing it
doesn't launch the connection. To initiate a connection, do the following:

```Java
myConnection.connect();
```

### Sending Data

Once a connection is established, we can send data in byte array format using the following:

```Java
myConnection.send("Hello, World!".getBytes());
```

### Receiving Data

Receiving data from a connection is often the most difficult part about transferring data.
Luckily, easycom core has an event-based system in which each Connection has a background
thread that waits until it receives data, then notifies "listeners" that have registered
with the connection. To get notified about incoming data, register a listener with the
connection:

```Java
myConnection.addOnDataReceivedListener(new Connection.OnDataReceivedListener() {
	@Override
	public void onDataReceived(Connection connection, byte[] data) {
		System.out.println("I just received this data: " + new String(data));
	}
});
```

### Disconnecting

To disconnect a connection, just execute the following:

```Java
myConnection.disconnect();
```

## Additional Listeners

In the receiving example above we added an `OnDataReceivedListener` to the connection,
which told the connection to notify us whenever we received incoming data. There are
several other listeners that can be registered with each connection to notify about
updates such as successful connection, disconnection, etc.

### OnMetadataChangedListener

An `OnMetadataChangedListener` will trigger whenever a metadata change such as renaming
the connection or re-assigning an address occurs.

```Java
myConnection.addOnMetadataChangedListener(new Connection.OnMetadataChangedListener() {
	@Override
	public void onMetadataChanged(Connection connection) {
		//Triggers when any metadata of the connection changes.
	}
})
```

### OnConnectListener

An `OnConnectListener` will trigger whenever this connection successfully completes
connecting to its target.

```Java
myConnection.addOnConnectListener(new Connection.OnConnectListener() {
	@Override
	public void onConnect(Connection connection) {
		//Triggers when this connection finishes connecting to target.
	}
})
```

### OnDisconnectListener

An `OnDisconnectListener` will trigger when this connection disconnects, either from
losing communitation with the target device or from a user-initiated disconnect.

```Java
myConnection.addOnDisconnectListener(new Connection.OnDisconnectListener() {
	@Override
	public void onDisconnect(Connection connection) {
		//Triggers when this connection disconnects from its target.
	}
})
```

### OnDataReceivedListener

As seen before, an `OnDataReceivedListener` will trigger whenever incoming data arrives
over a connection.

```Java
myConnection.addOnDataReceivedListener(new Connection.OnDataReceivedListener() {
	@Override
	public void onDataReceived(Connection connection, byte[] data) {
		//Triggers when data arrives over connection.
		System.out.println("Received data: " + new String(data));
	}
}
```

# Disclaimer

Though I would love for people to download and use this library, I cannot be
held responsible for any damages that may result from the use of it. I do
however welcome any feedback or contributions that may help to make it a more
stable and useful piece of software to be used by others. This library is
licensed under GPL 3.0.
