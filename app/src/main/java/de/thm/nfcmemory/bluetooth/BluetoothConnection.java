package de.thm.nfcmemory.bluetooth;

import android.util.Log;
import de.thm.nfcmemory.bluetooth.listener.BluetoothConnectionStateListener;

public abstract class BluetoothConnection implements BluetoothConnectionStateListener {
	private static final String TAG = "BluetoothConnection";
	
	private final BluetoothActivity activity;
	private final String mac;
	
	private int state = BluetoothActivity.STATE_NONE;
	
	public BluetoothConnection(final BluetoothActivity activity, final String mac){
		this.activity = activity;
		this.mac = mac;
	}
	
	public void start(){ start(true); }
	public void startInsecure(){ start(false); }
	private void start(boolean secure){
		if(state == BluetoothActivity.STATE_CONNECT || state == BluetoothActivity.STATE_CONNECTED){
			return;
		}
		
		state = BluetoothActivity.STATE_CONNECT;
		Log.v(TAG, "Connection initialized. Listener registered.");
		activity.addBluetoothConnectionStateListener(this);
		if(secure) activity.connect(mac, true);
		else activity.connect(mac, false);
	}
	
	public abstract void onConnectionEstablished();
	public abstract void onConnectionError(String msg);
	public abstract void onDisconnect();

	@Override
	public void onConnectionStateChanged(int oldState, int newState, int stateChangedCount) {
		switch(newState){			
		case BluetoothActivity.STATE_CONNECTED:
			this.state = newState;
			activity.removeBluetoothConnectionStateListener(this);
			onConnectionEstablished();
		case BluetoothActivity.STATE_CONNECT:
			break;
		case BluetoothActivity.STATE_ERROR:
			if(this.state == newState) break;
			this.state = BluetoothActivity.STATE_ERROR;
			activity.removeBluetoothConnectionStateListener(this);
			onConnectionError("Connection failed: Wrong state (" + state + ")");
			break;
		default:
			if(this.state == BluetoothActivity.STATE_CONNECTED){
				this.state = BluetoothActivity.STATE_NONE;
				activity.removeBluetoothConnectionStateListener(this);
				onDisconnect();
				break;
			}
		}
	}
}
