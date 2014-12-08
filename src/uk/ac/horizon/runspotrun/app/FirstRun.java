package uk.ac.horizon.runspotrun.app;

import android.content.Context;
import android.preference.PreferenceManager;

public class FirstRun {

	private final BooleanPreference pref;
	
	public FirstRun(Context context) {
		pref = new BooleanPreference(
				PreferenceManager.getDefaultSharedPreferences(context),
				"FIRST_RUN",
				true);
	}
	
	public boolean is() {
		return pref.is();
	}

	public void set(boolean value) {
		pref.set(value);
	}
	
}
