package de.thm.nfcmemory.bluetooth;

import java.util.ArrayList;

import android.util.Log;

public class MonitoringThread extends Thread {
	public static final int RUNNING = 1;
	public static final int SLEEPING = 2;
	public static final int INTERRUPTED = 3;
	public static final int TERMINATED = 4;
	public static final int DEAD = 5;
	
	private static final String TAG = "MonitoringThread";
	private static final ArrayList<MonitoringThread> threadPool = new ArrayList<MonitoringThread>();
	
	private final ArrayList<ThreadStateListener> stateListeners = new ArrayList<ThreadStateListener>();
	
	private boolean running = false;
	
	public void addThreatStateListener(ThreadStateListener lis){ stateListeners.add(lis); }
	public void removeThreadStateListener(ThreadStateListener lis){ stateListeners.remove(lis); }
	
	protected void notifyThreadStateListeners(int state){
		if(state == RUNNING) threadPool.add(this);
		if(state == DEAD) threadPool.remove(this);
		for(ThreadStateListener lis : stateListeners)
			lis.stateChanged(this, state);
	}
	
	public boolean isRunning(){ return running; }
	public int countRunningMonitorThreads(){ return threadPool.size(); }
	public void terminate(){
		// Thread stoppen
		Log.v(TAG, "Terminating MonitoringThread");
		if(isRunning()){
			running = false;
			notifyThreadStateListeners(TERMINATED);
		}
	}
	
	@Override
	public void run() {
		Log.v(TAG, "started...");
		notifyThreadStateListeners(RUNNING);
		running = true;
	}

	public interface ThreadStateListener{
		public void stateChanged(MonitoringThread thread, int state);
	}
}
