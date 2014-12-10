package uk.ac.horizon.runspotrun.app;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

class BooleanPreference {

	private boolean defaultValue;
	
	private String key;
	
	private SharedPreferences prefs;		
	
	public BooleanPreference(
			SharedPreferences prefs, String key, boolean defaultValue) {
		this.prefs = prefs;
		this.key = key;
		this.defaultValue = defaultValue;
	}
	
	public boolean is() {
		return prefs.getBoolean(key, defaultValue);
	}
	
	public void set(boolean value) {
		Editor editor = prefs.edit();
		editor.putBoolean(key, value);
		editor.apply();
	}
}
