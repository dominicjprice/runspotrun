package uk.ac.horizon.runspotrun.ui;

import uk.ac.horizon.runspotrun.R;
import uk.ac.horizon.runspotrun.app.AccessToken;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.app.Activity;
import android.app.AlertDialog;
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
		showGpsWarning();
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
	
	private void showGpsWarning() {
		LocationManager m = 
				(LocationManager)this.getSystemService(LOCATION_SERVICE);
		if(m.isProviderEnabled(LocationManager.GPS_PROVIDER))
			return;
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		final AlertDialog d = (AlertDialog)b.create();
		View v = View.inflate(this, R.layout.layout_gps_alert_dialog, null);
		((Button)v.findViewById(R.id.okay)).setOnClickListener(
				new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				d.dismiss();
				startActivity(new Intent(
						Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			}
		});
		((Button)v.findViewById(R.id.ignore)).setOnClickListener(
				new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				d.dismiss();				
			}
		});
		d.setView(v, 0, 0, 0, 0);
		d.show();
	}

}
