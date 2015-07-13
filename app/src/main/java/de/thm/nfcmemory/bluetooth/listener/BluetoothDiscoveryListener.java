package de.thm.nfcmemory.bluetooth.listener;

import android.bluetooth.BluetoothDevice;

public interface BluetoothDiscoveryListener {
	public void onDiscoveryStart();
	public void onDeviceFound(BluetoothDevice device);
	public void onDiscoveryFinished();
}
