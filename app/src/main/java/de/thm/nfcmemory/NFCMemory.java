package de.thm.nfcmemory;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;

import java.io.FileNotFoundException;

import de.thm.nfcmemory.model.CardSet;
import de.thm.nfcmemory.model.Player;

public class NFCMemory extends Application implements SharedPreferences.OnSharedPreferenceChangeListener {
	private static final String TAG = "NFCMemory";
	private static final boolean D = true;
	
	public static final String SHARED_PREFS = "de.thm.nfcmemory.shared.preferences";
	public static final String PREF_CARD_SET = "card.set";
	public static final String PREF_PLAYER_NAME = "player.name";

	private static NFCMemory app;

	private SharedPreferences prefs;
	private SharedPreferences defaultPrefs;
	private Temp temporary;
	
	@Override
	public void onCreate() {
		super.onCreate();
		app = this;
		
		if(D) Log.v(TAG, "Getting shared preferences");
		prefs = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
		defaultPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		defaultPrefs.registerOnSharedPreferenceChangeListener(this);

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

		if(D) Log.v(TAG, "Creating temporary object");
		temporary = new Temp();
		temporary.playerName = prefs.getString(PREF_PLAYER_NAME, "Player");
		if(D) Log.v(TAG, "Temporary Player: '" + temporary.playerName + "'");
		final String cardSet = defaultPrefs.getString(SettingsActivity.KEY_PREFS_CARD_SET, "default");
		try {
			temporary.cardSet = new CardSet(cardSet);
			if(D) Log.v(TAG,"Temporary CardSet ID: '" + temporary.cardSet.name + "'");
		} catch (FileNotFoundException e) {
			if(D) Log.v(TAG, "CardSet '" + cardSet + "' does not exist.");
		}
	}

	public Temp getTemp(){
		return temporary;
	}

	public static NFCMemory get(){
		return app;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		switch(key){
			case SettingsActivity.KEY_PREFS_CARD_SET:
				final String cardSet = defaultPrefs.getString(key, "default");
				try {
					temporary.setCardSet(new CardSet(cardSet));
				} catch (FileNotFoundException e) {
					if(D) Log.d(TAG, "Failed to apply card set: '" + cardSet + "'");
					e.printStackTrace();
				}
				break;
		}
	}

	public static class Const{
		public static final int API = android.os.Build.VERSION.SDK_INT;
		public static final String VERSION = android.os.Build.VERSION.RELEASE;
		public static final String SD_FOLDER = "/NFCMemory";
		private static Screen screen = null;

		public static Screen getScreen(){ return screen; }
	}

	public class Temp{
		private CardSet cardSet;
		private String playerName;

		public CardSet getCardSet(){ return cardSet; }
		protected void setCardSet(CardSet cardSet){
			this.cardSet = cardSet;
			Log.d(TAG, "Card set '" + cardSet.name + "' activated. Shared preferences handled by SettingsActivity...");
		}

		protected String getPlayerName(){ return playerName; }
		protected void setPlayerName(String playerName){
			this.playerName = playerName;
			final boolean success = prefs.edit()
					.putString(PREF_PLAYER_NAME, playerName)
					.commit();
			if(D){
				Log.v(TAG, "New player name set in temporary object");
				Log.v(TAG, success ? "Shared preferences updated: "
						+ PREF_PLAYER_NAME + " = " + playerName : "Shared preferences NOT updated.");
			}
		}
	}

	public class Screen{
		public final int width;
		public final int height;

		private Screen(int width, int height){
			this.width = width;
			this.height = height;
		}
	}
}
