package com.nicholastmosher.easycom.core.connection;

import android.hardware.usb.UsbDevice;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Nick Mosher on 10/15/15.
 * @author Nick Mosher, nicholastmosher@gmail.com, https://github.com/nicholastmosher
 */
public class UsbHostConnection extends Connection {

    private UsbDevice mUsbDevice;

    public UsbHostConnection(UsbDevice device) {
        super(device.getDeviceName());
        mUsbDevice = device;

        //FIXME Add implementation for USB Host connections. Until then throw unsupported.
        throw new UnsupportedOperationException("USB Host Connections are not yet supported.");
    }

    @Override
    public Status getStatus() {
        //FIXME Add implementation for USB Host connections. Until then throw unsupported.
        throw new UnsupportedOperationException("USB Host Connections are not yet supported.");
//        return mStatus;
    }

    @Override
    public String getConnectionType() {
        //FIXME Add implementation for USB Host connections. Until then throw unsupported.
        throw new UnsupportedOperationException("USB Host Connections are not yet supported.");
//        return TYPE_USB;
    }

    @Override
    public InputStream getInputStream() throws IllegalStateException {
        //FIXME Add implementation for USB Host connections. Until then throw unsupported.
        throw new UnsupportedOperationException("USB Host Connections are not yet supported.");
//        return new InputStream() {
//            @Override
//            public int read() throws IOException {
//                return 0;
//            }
//        };
    }

    @Override
    public OutputStream getOutputStream() throws IllegalStateException {
        //FIXME Add implementation for USB Host connections. Until then throw unsupported.
        throw new UnsupportedOperationException("USB Host Connections are not yet supported.");
//        return new OutputStream() {
//            @Override
//            public void write(int oneByte) throws IOException {
//                byte[] toSend = new byte[]{(byte) oneByte};
//
//            }
//        };
    }

    public UsbDevice getUsbDevice() {
        //FIXME Add implementation for USB Host connections. Until then throw unsupported.
        throw new UnsupportedOperationException("USB Host Connections are not yet supported.");
//        return mUsbDevice;
    }
}
