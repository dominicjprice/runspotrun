package uk.ac.horizon.runspotrun.service;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import uk.ac.horizon.runspotrun.Constants;
import uk.ac.horizon.runspotrun.data.LogEntry;
import uk.ac.horizon.runspotrun.data.LogEntryDAO;
import uk.ac.horizon.runspotrun.util.Session;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

public class LogUploader
extends IntentService {

	public LogUploader() {
		super(LogUploader.class.getName());
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		final LogEntryDAO dao = new LogEntryDAO(this);
		final ConnectivityManager cm =
		        (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		while(true) {
			NetworkInfo net = cm.getActiveNetworkInfo();
			if(net != null && net.isConnected())
				doUpload(dao, prefs);
			try {
				Thread.sleep(Constants.SERVICE_PAUSE_TIME);
			} catch(InterruptedException e) { }
		}
	}
	
	public void doUpload(LogEntryDAO dao, SharedPreferences prefs) {
		
		final Session session = new Session(this);	
		
		dao.open();
		try {
			for(LogEntry entry : dao.fetchNotUploaded()) {
				Log.d(Constants.LOG_TAG, "Uploading log entry: " + entry.endpoint + " - " + entry.data);
				
				HttpPost p = new HttpPost(Constants.SERVER_BASE_URL + "api/v1/" 
						+ entry.endpoint + "/?format=json");
				p.setHeader("Accept", "application/json");
				p.setHeader("Content-Type", "application/json");
				p.setHeader("Cookie", "csrftoken=" + session.getCsrfToken() 
						+ "; sessionid=" + session.getSessionId());
				p.setHeader("Referer", Constants.SERVER_BASE_URL + "api/v1/");
				p.setHeader("X-CSRFToken", session.getCsrfToken());
				
				HttpClient c = new DefaultHttpClient();
				try {
					p.setEntity(new StringEntity(entry.data, "UTF-8"));
					HttpResponse r = c.execute(p);
					int code = r.getStatusLine().getStatusCode();
					r.getEntity().consumeContent();
					if(code > 199 && code < 300) {
						Log.d(Constants.LOG_TAG, "Log entry uploaded successfully");
						dao.markAsUploaded(entry);
						// TODO: Check for auth errors, these are re-tryable 401 and 403?
					} else {
						Log.w(Constants.LOG_TAG, "Log entry upload failed, reason: ("
								+ code + ") " + r.getStatusLine().getReasonPhrase());
						if((entry.upload_retries++) == Constants.LOG_ENTRY_UPLOAD_MAX_TRIES) {
							Log.d(Constants.LOG_TAG, "Maximum number of retries reached");
							dao.markAsUploadFailed(entry);
						} else
							dao.save(entry);
					}
				} catch (Exception e) {
					Log.e(Constants.LOG_TAG, "A network error occurred during log entry upload: " 
							+ e.getMessage(), e);
				}
			}
		} finally {
			dao.close();
		}		
	}
	
}
