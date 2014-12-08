package uk.ac.horizon.runspotrun.service;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import uk.ac.horizon.runspotrun.app.AccessToken;
import uk.ac.horizon.runspotrun.app.Log;
import uk.ac.horizon.runspotrun.app.Urls;
import uk.ac.horizon.runspotrun.service.ServiceMonitor.BinderServiceMonitor;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;

public class ServiceLogUpload
extends Service {

	private static final int UPLOAD_MAX_TRIES = 10;
	
	public class LogUploadBinder 
	extends Binder {	
		public void insert(EntryLog entry) {
			dao.insert(entry);
			sendBroadcast(new Intent(ServiceMonitor.ACTION_LOG));
		}
	}
	
	private final Object LOCK = new Object();
		
	private final LogUploadBinder binder = new LogUploadBinder();
	
	private final ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			serviceMonitor = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {	
			synchronized(LOCK) {
				serviceMonitor = (BinderServiceMonitor)service;
				LOCK.notifyAll();
			}
		}
	};
	
	private DAOEntryLog dao;
	
	private BinderServiceMonitor serviceMonitor;
	
	private ConnectivityManager connectivity;
	
	public ServiceLogUpload() {
		super(ServiceLogUpload.class.getName());
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		dao = new DAOEntryLog(this);
		if(!bindService(new Intent(this, ServiceMonitor.class), 
				connection, Context.BIND_AUTO_CREATE))
			Log.w("Unable to bind to ServiceMonitor");
		connectivity = (ConnectivityManager)
				getSystemService(CONNECTIVITY_SERVICE);
	}
	
	@Override 
	public void onDestroy() {
		super.onDestroy();
		unbindService(connection);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		synchronized(LOCK) {
			while(serviceMonitor == null)
				try { LOCK.wait(); }
				catch(InterruptedException e) { }
		}
		serviceMonitor.logUploadComplete(process());
	}
	
	private ServiceMonitor.Reason process() {
		AccessToken accessToken = new AccessToken(this);
		for(EntryLog entry : dao.selectNotUploaded()) {
			if(!isLoggedIn())
				return ServiceMonitor.Reason.REQUIRE_LOGIN;
			
			final NetworkInfo ni = connectivity.getActiveNetworkInfo();
			if(ni == null || !ni.isConnectedOrConnecting())
				return ServiceMonitor.Reason.REQUIRE_NETWORK;
			
			upload(accessToken, entry);
		}
		return ServiceMonitor.Reason.UPLOAD_COMPLETE;
	}
	
	public void upload(AccessToken accessToken, EntryLog entry) {
		Log.d("Uploading log entry: " + entry.endpoint + " - " + entry.data);
		HttpPost p = new HttpPost(Urls.API(entry.endpoint, accessToken.get()));
		p.setHeader("Accept", "application/json");
		p.setHeader("Content-Type", "application/json");
		p.setHeader("Referer", Urls.API("", accessToken.get()));
		
		HttpClient c = new DefaultHttpClient();
		try {
			p.setEntity(new StringEntity(entry.data, "UTF-8"));
			HttpResponse r = c.execute(p);
			int code = r.getStatusLine().getStatusCode();
			r.getEntity().consumeContent();
			if(code > 199 && code < 300) {
				Log.d("Log entry uploaded successfully");
				dao.markAsUploaded(entry);
			} else if(code == 401 || code == 403) {
				Log.d("Upload not authorized, not logged in");
				accessToken.unset();
			} else {
				Log.w("Log entry upload failed, reason: ("
						+ code + ") " + r.getStatusLine().getReasonPhrase());
				if((entry.uploadRetries++) == UPLOAD_MAX_TRIES) {
					Log.d("Maximum number of retries reached");
					dao.markAsUploadFailed(entry);
				} else
					dao.update(entry);
			}
		} catch(Exception e) {
				Log.e("A network error occurred during log entry upload: " 
						+ e.getMessage(), e);
		}
	}
	
}
