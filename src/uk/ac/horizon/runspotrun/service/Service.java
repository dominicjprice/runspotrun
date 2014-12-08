package uk.ac.horizon.runspotrun.service;

import uk.ac.horizon.runspotrun.R;
import uk.ac.horizon.runspotrun.app.AccessToken;
import android.app.IntentService;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public abstract class Service 
extends IntentService {

	private SharedPreferences preferences;
	
	public Service(String name) {
		super(name);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
	}
	
	protected boolean isManualUpload() {
		return preferences.getString(getString(
				R.string.preference_allow_automated_uploads), 
				"manual").equals("manual");
	}
	
	protected boolean isLoggedIn() {
		String at = new AccessToken(this).get();
		return at != null && !at.isEmpty();
	}
	
	protected boolean isWifiUpload() {
		return preferences.getString(getString(
				R.string.preference_allowed_connection_types_for_upload),
				"").equals("wifi");
	}
	
	protected boolean isPoweredUpload() {
		return preferences.getBoolean(getString(
				R.string.preference_upload_when_plugged_in_only), false);
	}

}