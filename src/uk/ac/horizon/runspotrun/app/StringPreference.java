package uk.ac.horizon.runspotrun.app;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

class StringPreference {

	public static interface Listener {
		public void changed(String value);
	}
	
	private final String defaultValue;
	
	private final String key;
	
	private final Listener listener;
	
	private final SharedPreferences prefs;	
	
	private final OnSharedPreferenceChangeListener prefsListener = 
			new OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			if(key != null && key.equals(StringPreference.this.key))
				listener.changed(get());
		}
	};
	
	public StringPreference(
			SharedPreferences prefs,
			String key,
			String defaultValue,
			Listener listener) {
		this.prefs = prefs;
		this.key = key;
		this.defaultValue = defaultValue;
		this.listener = listener;
		prefs.registerOnSharedPreferenceChangeListener(prefsListener);
	}
	
	public String get() {
		return prefs.getString(key, defaultValue);
	}
	
	public void set(String value) {
		Editor editor = prefs.edit();
		editor.putString(key, value);
		editor.apply();
	}
	
	public void unset() {
		set(defaultValue);
	}
}
