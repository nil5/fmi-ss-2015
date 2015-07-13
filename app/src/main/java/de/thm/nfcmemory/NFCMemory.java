package de.thm.nfcmemory;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

public class NFCMemory extends Application {
	private static final String TAG = "NFCMemory";
	private static final boolean D = true;
	
	public static final String SHARED_PREFS = "de.thm.nfcmemory.shared.preferences";
	
	private static NFCMemory app;

	private SharedPreferences prefs;
	
	@Override
	public void onCreate() {
		super.onCreate();
		app = this;
		
		if(D) Log.v(TAG, "Getting shared preferences");
		prefs = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
	}
	
	public static class Const{
		public static final int API = android.os.Build.VERSION.SDK_INT;
		public static final String VERSION = android.os.Build.VERSION.RELEASE;
	}
	
	public static NFCMemory get(){
		return app;
	}
}
