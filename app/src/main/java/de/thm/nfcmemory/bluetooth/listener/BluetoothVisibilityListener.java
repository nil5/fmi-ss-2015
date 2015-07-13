package de.thm.nfcmemory.bluetooth.listener;

public interface BluetoothVisibilityListener {
	public void onVisibilityChanged(short secondsVisible, int scanMode);
}
