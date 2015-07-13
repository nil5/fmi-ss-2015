package de.thm.nfcmemory.bluetooth;

import java.util.ArrayList;

public class BluetoothThread extends MonitoringThread {
	private ArrayList<ConnectionStateListener> connectionStateListener = new ArrayList<ConnectionStateListener>();
	
	public void addConnectionStateListener(ConnectionStateListener lis){ connectionStateListener.add(lis); }
	public void removeConnectionStateListener(ConnectionStateListener lis){ connectionStateListener.remove(lis); }
	protected void notifyConnectionSateListeners(int state){
		for(ConnectionStateListener lis : connectionStateListener)
			lis.connectionStateChanged(state);
	}
	
	public interface ConnectionStateListener{
		public void connectionStateChanged(int state);
	}
}
