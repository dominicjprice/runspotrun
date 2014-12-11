package uk.ac.horizon.runspotrun.ui;

import uk.ac.horizon.runspotrun.R;
import uk.ac.horizon.runspotrun.app.AccessToken;
import android.os.Bundle;
import android.view.View;
import android.app.Activity;
import android.content.Intent;

public class ActivityHome 
extends Activity {

	private AccessToken accessToken;
	
	private AccessToken.Listener accessTokenListener = 
			new AccessToken.Listener() {		
		@Override
		public void changed(String value) {
			setLogonSectionVisibility(value != null && !value.isEmpty());
		}
	};
	
	private View logonView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		logonView = findViewById(R.id.activity_home_logon_button);
		accessToken = new AccessToken(this);
		accessToken.listen(accessTokenListener);
		setLogonSectionVisibility();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		accessToken.unlisten(accessTokenListener);
		accessToken = null;
	}
	
	public void onClickShowSettings(View view) {
	    startActivity(new Intent(this, ActivitySettings.class));
	}
	
	public void onClickShowCamera(View view) {
	    startActivity(new Intent(this, ActivityCamera.class));
	}
	
	public void onClickShowAbout(View view) {
		startActivity(new Intent(this, ActivityAbout.class));
	}
	
	public void onClickShowReview(View view) {
		startActivity(new Intent(this, ActivityReview.class));
	}
	
	public void onClickLogon(View view) {
		startActivity(new Intent(this, ActivityLogon.class));
	}
	
	public void onClickExit(View view) {
		finish();
	}
	
	private void setLogonSectionVisibility() {
		if(accessToken != null) {
			String v = accessToken.get();
			setLogonSectionVisibility(
					v == null || v.isEmpty());
		}
	}
	
	private void setLogonSectionVisibility(boolean visible) {
		if(logonView != null) {
			logonView.setVisibility(visible ? View.VISIBLE : View.GONE);
			logonView.refreshDrawableState();
		}
	}

}
