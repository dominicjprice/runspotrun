package uk.ac.horizon.runspotrun.ui;

import uk.ac.horizon.runspotrun.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity 
extends PreferenceActivity {

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		addPreferencesFromResource(R.xml.settings);
	}
	
}
