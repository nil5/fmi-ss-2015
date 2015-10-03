package de.thm.nfcmemory;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class DefaultActivity extends AppCompatActivity {
	private static final String TAG = "DefaultActivity";
	private static int activityCounter = 0;
	
	protected NFCMemory app;
	protected int id = ++activityCounter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// nur Portrait-Modus erlauben (Hochkant)
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		app = (NFCMemory) getApplication();
	}

	@Override
	protected void onStart() {
		super.onStart();
		
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
	}
}
