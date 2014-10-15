package uk.ac.horizon.runspotrun.ui;

import uk.ac.horizon.runspotrun.R;
import uk.ac.horizon.runspotrun.service.LogUploader;
import uk.ac.horizon.runspotrun.service.VideoUploader;
import android.os.Bundle;
import android.view.View;
import android.app.Activity;
import android.content.Intent;

public class HomeActivity 
extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		startService(new Intent(this, LogUploader.class));
		startService(new Intent(this, VideoUploader.class));
	}
	
	public void onClickShowSettings(View view) {
	    startActivity(new Intent(this, SettingsActivity.class));
	}
	
	public void onClickShowCamera(View view) {
	    startActivity(new Intent(this, CameraActivity.class));
	}
	
	public void onClickShowAbout(View view) {
		startActivity(new Intent(this, AboutActivity.class));
	}
	
	public void onClickShowReview(View view) {
		startActivity(new Intent(this, ReviewActivity.class));
	}
	
	/*public void onClickLogout(View view) {
		final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch(which) {
				case DialogInterface.BUTTON_POSITIVE:
					(new AsyncTask<Void, Void, Void>() {
						@Override
						protected void onPostExecute(Void result) {
							finish();
						}

						@Override
						protected Void doInBackground(Void... params) {
							LogEntryDAO loDao = new LogEntryDAO(HomeActivity.this);
							loDao.open();
							loDao.deleteAll();
							loDao.close();
							VideoEntryDAO vDao = new VideoEntryDAO(HomeActivity.this);
							vDao.open();
							vDao.deleteAll();
							vDao.close();
							return null;
						}
					}).execute(null, null);
					new Session(HomeActivity.this).clear();
					startActivity(new Intent(HomeActivity.this, LogonActivity.class));
					break;
				default:
					break;
				}
			}
		};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.activity_home_logout_message))
			.setPositiveButton(getString(R.string.activity_home_logout_yes), listener)
			.setNegativeButton(getString(R.string.activity_home_logout_no), listener)
			.show();
	}*/
}
