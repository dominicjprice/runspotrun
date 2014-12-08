package uk.ac.horizon.runspotrun.ui;

import uk.ac.horizon.runspotrun.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.View;

public class ActivitySettings 
extends PreferenceActivity {

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		addPreferencesFromResource(R.xml.settings);
	}
	
	public void onClickClose(View v) {
		close();
	}
	
	private void close() {
		finish();
	}
	
}
