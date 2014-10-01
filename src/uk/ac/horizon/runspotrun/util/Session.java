package uk.ac.horizon.runspotrun.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class Session {

	private static final String PREF_SESSIONID = "PREF_SESSIONID";
	
	private static final String PREF_CSRFTOKEN = "PREF_CSRFTOKEN";
	
	private final SharedPreferences preferences;
	
	public Session(Context context) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	public String getSessionId() {
		return preferences.getString(PREF_SESSIONID, "");
	}
	
	public String getCsrfToken() {
		return preferences.getString(PREF_CSRFTOKEN, "");
	}
	
	public void set(String sessionId, String csrfToken) {
		Editor editor = preferences.edit();
		editor.putString(PREF_CSRFTOKEN, csrfToken);
		editor.putString(PREF_SESSIONID, sessionId);
		editor.apply();
	}
	
	public void clear() {
		Editor editor = preferences.edit();
		editor.clear();
		editor.apply();
	}
	
}
