package de.thm.nfcmemory.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.util.Log;
import de.thm.nfcmemory.bluetooth.listener.BluetoothDiscoveryListener;

public abstract class BluetoothDiscovery implements BluetoothDiscoveryListener{
	
	private static final String TAG = "Discovery";
	private BluetoothActivity activity;
	private int state = BluetoothActivity.STATE_NONE;
	
	public abstract void handleDevice(BluetoothDevice device);
	public abstract void discoveryStateChanged(int state);
	
	public BluetoothDiscovery(final BluetoothActivity activity){
		this.activity = activity;
	}
	
	public void start(){
		if(state == BluetoothActivity.STATE_STARTING_DISCOVERY || state == BluetoothActivity.STATE_DISCOVERING){
			return;
		}
		
		state = BluetoothActivity.STATE_STARTING_DISCOVERY;
		Log.v(TAG, "Discovery initialized. Listener registered.");
		activity.addBluetoothDiscoveryListener(this);
		activity.discover();
	}
	
	public void cancel(){
		if(state == BluetoothActivity.STATE_DISCOVERING)
			activity.cancelDiscovery();
	}

	@Override
	public void onDeviceFound(BluetoothDevice device) {
		handleDevice(device);
	}

	@Override
	public void onDiscoveryStart() {
		state = BluetoothActivity.STATE_DISCOVERING;
		discoveryStateChanged(state);
	}

	@Override
	public void onDiscoveryFinished() {
		state = BluetoothActivity.STATE_DONE;
		discoveryStateChanged(state);
		activity.removeBluetoothDiscoveryListener(this);
	}
}
