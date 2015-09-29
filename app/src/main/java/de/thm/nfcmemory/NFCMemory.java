package de.thm.nfcmemory;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.util.Log;
import android.view.WindowManager;

import java.io.FileNotFoundException;

import de.thm.nfcmemory.model.CardSet;
import de.thm.nfcmemory.model.Player;

public class NFCMemory extends Application {
	private static final String TAG = "NFCMemory";
	private static final boolean D = true;
	
	public static final String SHARED_PREFS = "de.thm.nfcmemory.shared.preferences";
	public static final String PREF_CARD_SET = "card.set";
	public static final String PREF_PLAYER_NAME = "player.name";

	private static NFCMemory app;

	private SharedPreferences prefs;
	private Temp temporary;
	
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

		if(D) Log.v(TAG, "Creating temporary object");
		temporary = new Temp();
		temporary.player = new Player(prefs.getString(PREF_PLAYER_NAME, "Player"));
		final String cardSet = prefs.getString(PREF_CARD_SET, "default");
		try {
			temporary.cardSet = new CardSet(cardSet);
			if(D) Log.v(TAG, "Temporary Player is " + temporary.player.name + ". Temporary CardSet id is " + temporary.cardSet.name);
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
	
	public static class Const{
		public static final int API = android.os.Build.VERSION.SDK_INT;
		public static final String VERSION = android.os.Build.VERSION.RELEASE;
		public static final String SD_FOLDER = "/NFCMemory";
		private static Screen screen = null;

		public static Screen getScreen(){ return screen; }
	}

	public class Temp{
		private CardSet cardSet;
		private Player player;

		public CardSet getCardSet(){ return cardSet; }
		protected void setCardSet(CardSet cardSet){
			this.cardSet = cardSet;
			final boolean success = prefs.edit()
					.putString(PREF_CARD_SET, cardSet.name)
					.commit();
			if(D){
				Log.v(TAG, "New CardSet set in temporary object");
				Log.v(TAG, success ? "Shared preferences updated: "
						+ PREF_CARD_SET + " = " + cardSet.name : "Shared preferences NOT updated.");
			}
		}

		protected Player getPlayer(){ return player; }
		protected void setPlayer(Player player){
			this.player = player;
			final boolean success = prefs.edit()
					.putString(PREF_PLAYER_NAME, player.name)
					.commit();
			if(D){
				Log.v(TAG, "New season set in temporary object");
				Log.v(TAG, success ? "Shared preferences updated: "
						+ PREF_PLAYER_NAME + " = " + player.name : "Shared preferences NOT updated.");
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
