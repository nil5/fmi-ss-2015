package de.thm.nfcmemory.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.thm.nfcmemory.NFCActivity;
import de.thm.nfcmemory.bluetooth.MonitoringThread.ThreadStateListener;
import de.thm.nfcmemory.bluetooth.listener.BluetoothConnectionStateListener;
import de.thm.nfcmemory.bluetooth.listener.BluetoothDiscoveryListener;
import de.thm.nfcmemory.bluetooth.listener.BluetoothMessageListener;
import de.thm.nfcmemory.bluetooth.listener.BluetoothPowerStateListener;
import de.thm.nfcmemory.bluetooth.listener.BluetoothVisibilityListener;

public class BluetoothActivity extends NFCActivity implements ThreadStateListener {
	public static final String CONNECTION_STATE_CHANGED = "connection_state_changed";
	public static final int STATE_NONE = 0;
	public static final int STATE_READY = 1;
	public static final int STATE_STARTING_DISCOVERY = 2;
	public static final int STATE_DISCOVERING = 3;
	public static final int STATE_SERVE = 4;
	public static final int STATE_CONNECT = 5;
	public static final int STATE_SERVE_AND_CONNECT = 6;
	public static final int STATE_CONNECTED = 7;
	public static final int STATE_CLOSING = 8;
	public static final int STATE_DONE = 9;
	public static final int STATE_ERROR = 10;
	
	protected static final BluetoothAdapter ADAPTER = BluetoothAdapter.getDefaultAdapter();
	
	private static final String TAG = "BluetoothController";
	private static final int REQUEST_ENABLE_BT = 200;
	private static final int REQUEST_DISCOVERABLE = 201;
	
	private static final ArrayList<BluetoothConnectionStateListener> BLUETOOTH_CONNECTION_STATE_LISTENERS = new ArrayList<BluetoothConnectionStateListener>();
	private static final ArrayList<BluetoothDiscoveryListener> BLUETOOTH_DISCOVERY_LISTENERS = new ArrayList<BluetoothDiscoveryListener>();
	private static final ArrayList<BluetoothMessageListener> BLUETOOTH_MESSAGE_LISTENERS = new ArrayList<BluetoothMessageListener>();
	private static final ArrayList<BluetoothPowerStateListener> BLUETOOTH_POWER_STATE_LISTENERS = new ArrayList<BluetoothPowerStateListener>();
	private static final ArrayList<BluetoothVisibilityListener> BLUETOOTH_VISIBILITY_LISTENERS = new ArrayList<BluetoothVisibilityListener>();
	
	private static final UUID MY_UUID = UUID.fromString("13090cf0-ef08-4ba4-8da1-601be709046a"); // has to be the same on remote device
	private static final Handler UI_HANDLER = new Handler(Looper.getMainLooper());
	
	private static BluetoothActivity activity;
	private static BluetoothServerThread serverThread = null;
	private static BluetoothConnectionThread connectionThread = null;
	private static BluetoothMessageThread messageThread = null;
	private static Thread discovery = null;
	private static int state = STATE_NONE;
	private static boolean bluetoothSupport;
	
	private static final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
	        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
	        	// When discovery finds a device
	            // Get the BluetoothDevice object from the Intent
	            for(BluetoothDiscoveryListener lis : BLUETOOTH_DISCOVERY_LISTENERS)
	            	lis.onDeviceFound((BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
	        }
	        
	        else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
	        	// When Bluetooth state changes
	        	final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
				switch (state) {
				case BluetoothAdapter.STATE_OFF:
					Log.d(TAG, "Bluetooth off");
					setState(STATE_NONE);
					break;
				case BluetoothAdapter.STATE_TURNING_OFF:
					Log.d(TAG, "Turning Bluetooth off...");
					break;
				case BluetoothAdapter.STATE_ON:
					Log.d(TAG, "Bluetooth on");
					setState(STATE_READY);
					break;
				case BluetoothAdapter.STATE_TURNING_ON:
					Log.d(TAG, "Turning Bluetooth on...");
					break;
				}
				for(BluetoothPowerStateListener lis : BLUETOOTH_POWER_STATE_LISTENERS)
					lis.onPowerStateChanged(state);
			}
	        
	        else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
				Log.v(TAG, "Discovery started.");
				setState(STATE_DISCOVERING);
				for(int i = 0; i < BLUETOOTH_DISCOVERY_LISTENERS.size(); i++)
	            	BLUETOOTH_DISCOVERY_LISTENERS.get(i).onDiscoveryStart();
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
				Log.v(TAG, "Discovery finished.");
				if(state == STATE_DISCOVERING) setState(STATE_READY);
				for(int i = 0; i < BLUETOOTH_DISCOVERY_LISTENERS.size(); i++)
	            	BLUETOOTH_DISCOVERY_LISTENERS.get(i).onDiscoveryFinished();
			}
	        
			else if(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)){
				int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.SCAN_MODE_NONE);
				int previousScanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, BluetoothAdapter.SCAN_MODE_NONE);
				Log.v(TAG, "Scan mode changed from " + previousScanMode + " to " + scanMode);
				
				for(BluetoothVisibilityListener lis : BLUETOOTH_VISIBILITY_LISTENERS){
					switch(scanMode){
					case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
						lis.onVisibilityChanged(Short.MAX_VALUE, scanMode);
						break;
					case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
					case BluetoothAdapter.SCAN_MODE_NONE:
						lis.onVisibilityChanged((short) 0, scanMode);
					}
				}
			}
	    }
	};
	
	private static final Handler messageHandler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			Log.v(TAG, "Incoming message");
			for(int i = 0; i < BLUETOOTH_MESSAGE_LISTENERS.size(); i++)
				BLUETOOTH_MESSAGE_LISTENERS.get(i).onMessageRecieved(msg);
			return true;
	}});	
	
	
	
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		activity = this;
		
		// Testen ob Bluetooth vorhanden
		if(ADAPTER != null){
			bluetoothSupport = true;
			if(bluetoothEnabled()) setState(STATE_READY);
		} else {
			bluetoothSupport = false;
			Log.e(TAG, "Bluetooth error: Bluetooth is not supported by this device. You will not be able to use any Bluetooth features.");
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
		registerReceiver(receiver, filter);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(receiver);
	}
	
	@Override
	protected void onActivityResult(final int arg0, final int arg1, final Intent arg2) {
		super.onActivityResult(arg0, arg1, arg2);
		switch(arg0){
		case REQUEST_ENABLE_BT:
			switch(arg1){
			case RESULT_OK:
				Log.d(TAG, "Bluetooth activated by user.");
				break;
			case RESULT_CANCELED:
				Log.d(TAG, "Bluetooth activation denied by user.");
				break;
			}
			break;
		case REQUEST_DISCOVERABLE:
			Log.d(TAG, "Discoverable for " + arg1 + "s.");
			if(discovery != null) discovery.interrupt();
			
			discovery = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						for(short i = (short) arg1; i >= 0; i--){
							if(state == STATE_NONE) break;
							final short s = i;
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									for(BluetoothVisibilityListener lis : BLUETOOTH_VISIBILITY_LISTENERS)
										lis.onVisibilityChanged(s, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
								}
							});
							Thread.sleep(1000);
						}
						Log.v(TAG, "Device is no longer discoverable.");
					} catch (InterruptedException e) { }
					finally{ discovery = null; }
				}
			});
			discovery.start();
		}
	}

	public void enable(boolean askUser){
		if(!bluetoothSupport) return;
		
		if(bluetoothEnabled()){
			Log.v(TAG, "Bluetooth is already enabled.");
			return;
		}
		
		if(askUser){
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		} else ADAPTER.enable();
	}
	
	public void disable(){
		if(!bluetoothSupport) return;
		
		if(!bluetoothEnabled()){
			Log.v(TAG, "Bluetooth is not enabled.");
			return;
		}
		
		ADAPTER.disable();
	}
	
	public void makeDiscoverable(){
		if(!bluetoothSupport) return;
		
		Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE);
	}
	
	public void discover(){
		if(state == STATE_READY){
			setState(STATE_STARTING_DISCOVERY);
			ADAPTER.startDiscovery();
		} else Log.w(TAG, "Discovery will only perform when connection state is " + STATE_READY);
	}
	
	public void cancelDiscovery(){
		if(!bluetoothSupport) return;
		
		if(ADAPTER.isDiscovering()){
			Log.v(TAG, "Cancel discovery.");
			ADAPTER.cancelDiscovery();
			setState(STATE_READY);
		}
	}
	
	public boolean bluetoothSupport(){ return bluetoothSupport; }
	
	public boolean bluetoothEnabled(){
		if(!bluetoothSupport) return false;
		
		if(!ADAPTER.isEnabled()){
			Log.w(TAG, "Bluetooth is not enabled. Bluetooth needs to be enabled to use most of the app functions.");
			return false;
		}
		
		return true;
	}
	
	public List<BluetoothDevice> getPairedDevices(){
		if(!bluetoothSupport || !bluetoothEnabled()) return null;
		return new ArrayList<BluetoothDevice>(ADAPTER.getBondedDevices());
	}
	
	public boolean serve(String name, boolean secure){
		switch(state){
		case STATE_STARTING_DISCOVERY:
		case STATE_DISCOVERING:
			cancelDiscovery();
		case STATE_READY:
		case STATE_CONNECT:
			break;
		default:
			Log.v(TAG, "Current connection state " + state + " does not allow to start new server.");
			return false;
		}
		
		Log.v(TAG, "Trying to create BluetoothServerThread.");
		
		BluetoothServerThread serverThread = BluetoothServerThread.create(name, secure);
		try{
			serverThread.addThreatStateListener(this);
			serverThread.start();
			BluetoothActivity.serverThread = serverThread;
			if(connectionThread != null) setState(STATE_SERVE_AND_CONNECT);
			else setState(STATE_SERVE);
			return true;
		} catch (NullPointerException e){
			return false;
		}
	}
	
	public boolean connect(String mac, boolean secure){
		switch(state){
		case STATE_STARTING_DISCOVERY:
		case STATE_DISCOVERING:
			cancelDiscovery();
		case STATE_READY:
		case STATE_SERVE:
			break;
		default:
			Log.v(TAG, "Current connection state " + state + " does not allow to start new connection request.");
			return false;
		}
		
		// check if address valid
		if(!BluetoothAdapter.checkBluetoothAddress(mac)){
			Log.e(TAG, "Argument error (connect): Address '" + mac + "' is not valid.");
			return false;
		}
		
		Log.v(TAG, "Trying to create BluetoothConnectionThread.");
		
		BluetoothConnectionThread connectionThread = BluetoothConnectionThread.create(ADAPTER.getRemoteDevice(mac), secure);
		try{
			connectionThread.addThreatStateListener(this);
			connectionThread.start();
			BluetoothActivity.connectionThread = connectionThread;
			if(serverThread != null) setState(STATE_SERVE_AND_CONNECT);
			else setState(STATE_CONNECT);
			return true;
		} catch(NullPointerException e){
			return false;
		}
	}
	
	public void disconnect(){
		if(state != STATE_CONNECTED) return;
		
		messageThread.close();
	}
	
	public void terminateConnectionThread(){ terminateBluetoothThread(connectionThread); }
	public void terminateServerThread(){ terminateBluetoothThread(serverThread); }
	private void terminateBluetoothThread(BluetoothThread thread){
		try{ thread.terminate(); }
		catch (NullPointerException e){
			Log.e(TAG, "Exception was thrown while trying terminate BluetoothThread: " + e.getMessage());
		}
	}
	
	protected static void setBluetoothMessageThread(BluetoothMessageThread thread){
		Log.v(TAG, "Setting BluetoothMessageThread.");
		if(messageThread == null){
			if(thread != null){
				messageThread = thread;
				messageThread.addThreatStateListener(activity);
				setState(STATE_CONNECTED);
			}
		}
	}
	
	public boolean send(byte b){ return send(Byte.toString(b)); }
	public boolean send(String s){
		if(!bluetoothSupport || !bluetoothEnabled()) return false;
		
		if(state != STATE_CONNECTED) return false;
		
		// Nachricht senden
		Log.v(TAG, "Trying to send message: '" + s + "'");
		try{
			return messageThread.send(s.getBytes());
		} catch(NullPointerException e){
			// BluetoothMessageThread ist nicht initialisiert oder String ist null
			Log.e(TAG, "Exception was thrown while trying to send Message: " + e.getMessage());
			return false;
		}
	}
	
	public void addBluetoothConnectionStateListener(BluetoothConnectionStateListener lis){
		if(!BLUETOOTH_CONNECTION_STATE_LISTENERS.contains(lis))
			BLUETOOTH_CONNECTION_STATE_LISTENERS.add(lis);
	}
	public void removeBluetoothConnectionStateListener(BluetoothConnectionStateListener lis){ BLUETOOTH_CONNECTION_STATE_LISTENERS.remove(lis); }
	public void addBluetoothDiscoveryListener(BluetoothDiscoveryListener lis){
		if(!BLUETOOTH_DISCOVERY_LISTENERS.contains(lis))
			BLUETOOTH_DISCOVERY_LISTENERS.add(lis);
	}
	public void removeBluetoothDiscoveryListener(BluetoothDiscoveryListener lis){ BLUETOOTH_DISCOVERY_LISTENERS.remove(lis); }
	public void addBluetoothMessageListener(BluetoothMessageListener lis){
		if(!BLUETOOTH_MESSAGE_LISTENERS.contains(lis))
			BLUETOOTH_MESSAGE_LISTENERS.add(lis);
	}
	public void removeBluetoothMessageListener(BluetoothMessageListener lis){ BLUETOOTH_MESSAGE_LISTENERS.remove(lis); }
	public void addBluetoothPowerStateListener(BluetoothPowerStateListener lis){
		if(!BLUETOOTH_POWER_STATE_LISTENERS.contains(lis))
			BLUETOOTH_POWER_STATE_LISTENERS.add(lis);
	}
	public void removeBluetoothPowerStateListener(BluetoothPowerStateListener lis){ BLUETOOTH_POWER_STATE_LISTENERS.remove(lis); }
	public void addBluetoothVisibilityListener(BluetoothVisibilityListener lis){
		if(!BLUETOOTH_VISIBILITY_LISTENERS.contains(lis))
			BLUETOOTH_VISIBILITY_LISTENERS.add(lis);
	}
	public void removeBluetoothVisibilityListener(BluetoothVisibilityListener lis){ BLUETOOTH_VISIBILITY_LISTENERS.remove(lis); }
	
	public BroadcastReceiver getBluetoothReciever(){ return receiver; }
	
	private static int stateChangeCount = 0;
	public int getState(){ return state; }
	private static void setState(final int newState){
		stateChangeCount++;
		final int oldState = BluetoothActivity.state;
		Log.v(TAG, "Setting state from " + oldState + " to " + newState + " (" + stateChangeCount + ")");
		BluetoothActivity.state = newState;
		
		runOnUi(new Runnable() {
			@Override
			public void run() {
				Log.v(TAG, "Informing " + BLUETOOTH_CONNECTION_STATE_LISTENERS.size() + " listeners about connection state change no. " + stateChangeCount+ ".");
				for(BluetoothConnectionStateListener lis : BLUETOOTH_CONNECTION_STATE_LISTENERS)
					lis.onConnectionStateChanged(oldState, newState, stateChangeCount);
			}
		});
	}

	@Override
	public void stateChanged(MonitoringThread thread, final int state) {
		switch(state){
		case MonitoringThread.RUNNING:
			Log.v(TAG, "ThreadState (" + thread.toString() + "): RUNNING");
			break;
		case MonitoringThread.SLEEPING:
			Log.v(TAG, "ThreadState (" + thread.toString() + "): SLEEPING");
			break;
		case MonitoringThread.TERMINATED:
			Log.v(TAG, "ThreadState (" + thread.toString() + "): TERMINATED");
			break;
		case MonitoringThread.INTERRUPTED:
			Log.v(TAG, "ThreadState (" + thread.toString() + "): INTERRUPTED");
			break;
		case MonitoringThread.DEAD:
			Log.v(TAG, "ThreadState (" + thread.toString() + "): DEAD");
			thread.removeThreadStateListener(this);
			if(thread == serverThread){
				Log.v(TAG, "BluetoothConnectionThread is dead.");
				if(connectionThread != null) setState(STATE_CONNECT);
				else if(messageThread == null) setState(STATE_READY);
				serverThread = null;
			}
			if(thread == connectionThread){
				Log.v(TAG, "BluetoothConnectionThread is dead.");
				if(serverThread != null) setState(STATE_SERVE);
				else if(messageThread == null) setState(STATE_READY);
				connectionThread = null;
			}
			if(thread == messageThread){
				Log.v(TAG, "BluetoothMessageThread is dead.");
				setState(STATE_READY);
				messageThread = null;
			}
			break;
		}
	}
	
	public static void runOnUi(Runnable runnable){ UI_HANDLER.post(runnable); }










	private static class BluetoothServerThread extends BluetoothThread {
		private static final String TAG = "BluetoothServer";
		
		private static BluetoothServerThread currentThread = null;
		
		private final BluetoothServerSocket serverSocket;
		
		private BluetoothServerThread(String name, boolean secure){
			Log.v(TAG, "Initialize BluetoothServerThread.");
			
			// BluetoothServerSocket erstellen
			Log.v(TAG, "Trying to create BluetoothServerSocket.");
			BluetoothServerSocket tmp = null;
			try {
				if(secure) tmp = ADAPTER.listenUsingRfcommWithServiceRecord(name, MY_UUID);
				else tmp = ADAPTER.listenUsingInsecureRfcommWithServiceRecord(name, MY_UUID);
				Log.v(TAG, "BluetoothServerSocket created successfully.");
			} catch (IOException e) {
				Log.e(TAG, "Exception was thrown while trying to create BluetoothServerSocket: " + e.getMessage());
			}
			serverSocket = tmp;
			
			// Bei Erfolg: Thread als aktiv kennzeichnen
			if(serverSocket != null){
				currentThread = this;
				Log.v(TAG, "BluetoothServerThread locked: " + toString());
			} else setState(STATE_ERROR);
		}
		
		public static BluetoothServerThread create(String name, boolean secure){
			// Nur erstellbar, wenn keine anderer Thread aktiv
			if(currentThread != null){
				Log.e(TAG, "Can't initialize BluetoothServerThread. Only one Server can be opened at the same time. Lock caused by: " + currentThread.toString());
				return null;
			} else return new BluetoothServerThread(name, secure);
		}
		
		public boolean close(){
			Log.v(TAG, "Trying to close BluetoothServerSocket");
			try {
				// BluetoothServerSocket schliessen
				serverSocket.close();
				Log.v(TAG, "BluetoothServerSocket closed successfully");
				
				// Thread als nicht mehr aktiv kennzeichnen
				if(this == currentThread){
					currentThread = null;
					Log.v(TAG, "Freed BluetoothServerThread lock. A new BluetoothServerThread can now be opened.");
				}
				return true;
			} catch (IOException e) {
				// Fehler beim schliessen des BluetoothServerSockets
				Log.e(TAG, "Exception was thrown while trying to close BluetoothServerSocket: " + e.getMessage());
				return false;
			} catch (NullPointerException e) {
				// BluetoothServerSocket ist nicht initialisiert
				Log.e(TAG, "Exception was thrown while trying to close BluetoothServerSocket: " + e.getMessage());
				return false;
			}
		}
		
		public static BluetoothServerThread getCurrentThread(){ return currentThread; }
	
		@Override
		public void run() {
			// MonitoringThread
			super.run();
			Log.v(TAG, "BluetoothServerThread running.");
			
			// Auf Verbindungsgeraet warten (maximal 30 Sekunden)
			BluetoothSocket socket = null;
			Log.v(TAG, "Waiting for client connection...");
			while (isRunning()) {
	            try {
	                socket = serverSocket.accept(30000);
	            } catch (IOException e) {
	            	// Fehler beim Verbindungsaufbau (z.B. geschlossener BluetoothServerSocket): Thread stoppen
	            	Log.e(TAG, "Exception was thrown while trying to connect to client: " + e.getMessage());
	            	socket = null;
	            	setState(STATE_ERROR);
	                terminate();
	            } catch (NullPointerException e){
	            	// BluetoothServerSocket ist nicht initialisiert: Thread stoppen
	            	Log.e(TAG, "Exception was thrown while trying to connect to client: " + e.getMessage());
	            	socket = null;
	            	setState(STATE_ERROR);
	            	terminate();
	            }
	            
	            // Wenn Socket erfolgreich erstellt...
	            if (socket != null) {
	            	BluetoothDevice remote = socket.getRemoteDevice();
	            	Log.v(TAG, "Connection to client '" + remote.getName() + "' (" + remote.getAddress() + " established. Starting BluetoothMessageThread.");
	
	            	// Starten und speichern eines BluetoothMessageThreads
	                new BluetoothMessageThread(socket).start();
	                
	                // Thread stoppen
	                terminate();
	            }
			}
			
			// BluetoothServerSocket schliessen
			close();
			
			// Listener benachrichtigen
			notifyThreadStateListeners(DEAD);
		}
	}
	
	
	
	
	
	
	
	
	
	
	private static class BluetoothConnectionThread extends BluetoothThread {
		private static final String TAG = "BluetoothConnector";
		
		private static BluetoothConnectionThread currentThread = null;
		
		private final BluetoothSocket socket;
		
		private boolean connected;
		
		private BluetoothConnectionThread(BluetoothDevice device, boolean secure){
			Log.v(TAG, "Initializing BluetoothConnectionThread.");
			
			// BluetoothSocket erstellen
			Log.v(TAG, "Trying to create BluetoothSocket.");
			BluetoothSocket tmp = null;
			try {
				if(secure) tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
				else tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
				Log.v(TAG, "BluetoothSocket created successfully.");
			} catch (IOException e) {
				Log.e(TAG, "Exception was thrown while trying to create BluetoothSocket: " + e.getMessage());
			}
			socket = tmp;
			
			// Bei Erfolg: Thread als aktiv kennzeichnen
			if(socket != null){
				currentThread = this;
				Log.v(TAG, "BluetoothConnectionThread locked: " + toString());
			} else setState(STATE_ERROR);
		}
		
		public static BluetoothConnectionThread create(BluetoothDevice device, boolean secure){
			if(currentThread != null){
				Log.e(TAG, "Can't initialize BluetoothConnectionThread. Only one connection request run at the same time. Lock caused by: " + currentThread.toString());
				return null;
			} else return new BluetoothConnectionThread(device, secure);
		}
		
		public static BluetoothConnectionThread getCurrentThread(){ return currentThread; }
		
		public boolean close(){
			Log.v(TAG, "Trying to close BluetoothSocket");
			
			try {
				// BluetoothSocket schliessen
				if(!connected){
					socket.close();
					Log.v(TAG, "BluetoothSocket closed successfully");
				} else Log.v(TAG, "BlutoothSocket will be used and closed by MessageThread.");
				
				// Thread als nicht mehr aktiv kennzeichnen
				if(this == currentThread){
					currentThread = null;
					Log.v(TAG, "Freed BluetoothConnectionThread lock. A new BluetoothConnectionThread can now be opened.");
				}
				return true;
			} catch (IOException e) {
				// Fehler beim Schliessen des BluetoothSockets
				Log.e(TAG, "Exception was thrown while trying to close BluetoothSocket: " + e.getMessage());
				return false;
			} catch (NullPointerException e) {
				// BluetoothSocket ist nicht initialisiert
				Log.e(TAG, "Exception was thrown while trying to close BluetoothSocket: " + e.getMessage());
				return false;
			}
		}
		
		public void run() {
			// MonitoringThread
			super.run();
			Log.v(TAG, "BluetoothConnectionThread running.");
			
			Log.v(TAG, "Trying to connect to server...");
			connected = false;
			try{
				// connect to remote device
				socket.connect();
				connected = true;
			} catch (IOException e){
				// Fehler beim Verbindungsaufbau (z.B. geschlossener BluetoothServerSocket): Thread stoppen
				Log.e(TAG, "Exception was thrown while trying to connect to server: " + e.getMessage());
				setState(STATE_ERROR);
	        	terminate();
			} catch (NullPointerException e){
	        	// BluetoothSocket oder BluetoothMessageThread ist nicht initialisiert: Thread stoppen
	        	Log.e(TAG, "Exception was thrown while trying to connect to server: " + e.getMessage());
	        	setState(STATE_ERROR);
	        	terminate();
	        }
			
			// Wenn Socket erfolgreich erstellt...
	        if (connected) {
	        	BluetoothDevice remote = socket.getRemoteDevice();
	        	Log.v(TAG, "Connection to server '" + remote.getName() + "' (" + remote.getAddress() + " established. Starting BluetoothMessageThread.");

	        	// Starten und speichern eines BluetoothMessageThreads
	            new BluetoothMessageThread(socket).start();
	            
	            // Thread stoppen
	            terminate();
	        } else {
	        	if(serverThread != null) setState(STATE_SERVE);
	        	else setState(STATE_READY);
	        }
	        
	        close();
	        
	        // Listener benachrichtigen
	     	notifyThreadStateListeners(DEAD);
		}
	}
	
	
	
	
	
	
	
	
	
	
	public static class BluetoothMessageThread extends MonitoringThread {
		private static final String TAG = "BluetoothMessenger";
		
		private final BluetoothSocket socket;
		private final InputStream in;
	    private final OutputStream out;
		
		public BluetoothMessageThread(BluetoothSocket socket){
			Log.v(TAG, "Initialize BluetoothMessageThread.");
			this.socket = socket;
			
			BluetoothActivity.setBluetoothMessageThread(this);
			
			// Temporaere Variablen um sicherzugehen, dass finale Variablen initialisiert werden
	    	InputStream tmpIn = null;
	    	OutputStream tmpOut = null;
	    	Log.v(TAG, "Trying to get Input- and OutputStream.");
	    	
	    	// Versuchen In- und OutputStream zu bekommen
	    	try{
	    		tmpIn = socket.getInputStream();
	    		tmpOut = socket.getOutputStream();
	    		Log.v(TAG, "Input- and OutputStream successfully initialized.");
	    	} catch (IOException e){
	    		Log.e(TAG, "Exception was thrown while trying to get Input- and OutputStream: " + e.getMessage());
	    	}
	    	
	    	// finale Variablen initialisieren
	    	in = tmpIn;
	    	out = tmpOut;
		}
		
		public boolean send(byte[] bytes){
			// Nachricht senden
	        try {
	            out.write(bytes);
	            Log.v(TAG, "Message successfully sent.");
	            return true;
	        } catch (IOException e) {
	        	// Fehler beim Schreiben des OutputStreams
	        	Log.e(TAG, "Exception was thrown while trying to send Message: " + e.getMessage());
	        	return false;
	        } catch (NullPointerException e){
	        	// OutputStream ist nicht korrekt initialisiert
	        	Log.e(TAG, "Exception was thrown while trying to send Message: " + e.getMessage());
	        	return false;
	        }
		}
		
		public boolean close(){
			Log.v(TAG, "Trying to close BluetoothSocket");
			try {
				// In- und OutputStream sowie ServerSocket schliessen
				in.close();
				out.close();
				socket.close();
				Log.v(TAG, "BluetoothSocket successfully closed");
				return true;
			} catch (IOException e) {
				// Fehler beim Schliessen einer Komponenten
				Log.e(TAG, "Exception was thrown while trying to close BluetoothSocket: " + e.getMessage());
				return false;
			} catch (NullPointerException e){
				// Mindestens eine Komponente ist nicht initialisiert
				Log.e(TAG, "Exception was thrown while trying to close BluetoothSocket: " + e.getMessage());
				return false;
			}
		}

		@Override
		public void run() {
			// MonitoringThread
			super.run();
			Log.v(TAG, "BluetoothMessageThread running.");
			
			byte[] buffer = new byte[1024];
			int bytes;
			
			// Auf Nachrichten von Verbindungsgeraet warten
			Log.v(TAG, "Waiting for messages...");
			while(isRunning()){
				try{
					// InputStream lesen
					bytes = in.read(buffer);
					Log.v(TAG, "Message recieved. Informing Handler.");
					
					final String what = new String(buffer, 0, bytes);
					
					// Message aus InputStream erstellen und an Handler weiterleiten
					messageHandler.obtainMessage(1, what).sendToTarget();
					Log.v(TAG, "Handler successfully informed");
				} catch (IOException e){
					// Fehler beim Lesen des InputStreams
					Log.e(TAG, "Exception was thrown while trying to read Message: " + e.getMessage());
					setState(STATE_ERROR);
					terminate();
				} catch (NullPointerException e){
					// InputStream oder Handler ist nicht korrekt initialisiert
					Log.e(TAG, "Exception was thrown while trying to read Message: " + e.getMessage());
					setState(STATE_ERROR);
					terminate();
				}
			}
			
			// Komponenten schliessen
			close();
			
			// Listener benachrichtigen
			notifyThreadStateListeners(DEAD);
		}
		
		
	}
}