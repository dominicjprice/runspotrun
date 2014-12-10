package uk.ac.horizon.runspotrun.ui;

import uk.ac.horizon.runspotrun.R;
import android.os.Bundle;
import android.view.View;
import android.app.Activity;

public class ActivityAbout 
extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
	}

	public void onClickClose(View view) {
		finish();
	}
}
