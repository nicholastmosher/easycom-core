package com.nicholastmosher.easycom.core.connection;

import android.content.ContentValues;
import android.hardware.usb.UsbDevice;

import com.nicholastmosher.easycom.R;
import com.nicholastmosher.easycom.core.connection.intents.ConnectionIntent;

import java.io.IOException;
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
    }

    @Override
    public Status getStatus() {
        return mStatus;
    }

    @Override
    public String getConnectionType() {
        return ConnectionIntent.CONNECTION_TYPE_USB;
    }

    @Override
    public InputStream getInputStream() throws IllegalStateException {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                return 0;
            }
        };
    }

    @Override
    public OutputStream getOutputStream() throws IllegalStateException {
        return new OutputStream() {
            @Override
            public void write(int oneByte) throws IOException {
                byte[] toSend = new byte[]{(byte) oneByte};

            }
        };
    }

    @Override
    public int getImageResourceId() {
        return R.drawable.ic_usb_black_48dp;
    }

    @Override
    public ContentValues getContentValues() {
        return null;
    }

    public UsbDevice getUsbDevice() {
        return mUsbDevice;
    }
}
