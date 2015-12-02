package com.jikexueyuan.bluetoothchat;

import android.bluetooth.BluetoothDevice;

/**
 * Created by plter on 11/27/15.
 */
public class BluetoothDeviceWrapper {
    private final BluetoothDevice device;

    public BluetoothDeviceWrapper(BluetoothDevice device) {
        this.device = device;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    @Override
    public boolean equals(Object o) {
        if (o!=null){
            assert o instanceof BluetoothDeviceWrapper;

            return ((BluetoothDeviceWrapper) o).getDevice().getAddress().equals(getDevice().getAddress());
        }else {
            return false;
        }
    }

    @Override
    public String toString() {
        return String.format("%s\n%s",getDevice().getName(),getDevice().getAddress());
    }
}
