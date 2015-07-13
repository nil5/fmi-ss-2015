package de.thm.nfcmemory.bluetooth.listener;

public interface BluetoothConnectionStateListener {
	public void onConnectionStateChanged(int oldState, int newState, int stateChangedCount);
}
