package de.thm.nfcmemory;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.util.Log;
import android.view.WindowManager;

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

		if(D) Log.v(TAG, "Getting screen size");
		final Point screenSize = new Point();
		final WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		if(NFCMemory.Const.API >= 13)
			wm.getDefaultDisplay().getSize(screenSize);
		else{
			screenSize.x = wm.getDefaultDisplay().getWidth();
			screenSize.y = wm.getDefaultDisplay().getHeight();
		}
		Const.screen = new Screen(screenSize.x, screenSize.y);
		if(D) Log.v(TAG, "Screen width, height: " + Const.screen.width + ", " + Const.screen.height);
	}
	
	public static class Const{
		public static final int API = android.os.Build.VERSION.SDK_INT;
		public static final String VERSION = android.os.Build.VERSION.RELEASE;
		private static Screen screen = null;

		public static Screen getScreen(){ return screen; }
	}

	public class Screen{
		public final int width;
		public final int height;

		private Screen(int width, int height){
			this.width = width;
			this.height = height;
		}
	}
	
	public static NFCMemory get(){
		return app;
	}
}
