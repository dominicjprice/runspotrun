package uk.ac.horizon.runspotrun.ui;

import uk.ac.horizon.runspotrun.R;
import uk.ac.horizon.runspotrun.app.FirstRun;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class ActivityLaunch 
extends Activity {
	
	private FirstRun firstRun;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if((firstRun = new FirstRun(this)).is()) 
			setContentView(R.layout.activity_launch);
		else
			next();
	}
	
	public void onClickAgree(View view) {
		firstRun.set(false);
		next();
	}
	
	public void onClickDisagree(View view) {
		finish();
	}
		
	private void next() {
		startActivity(new Intent(this, ActivityHome.class));
		finish();
	}
	
}
