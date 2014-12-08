package uk.ac.horizon.runspotrun.service;

import uk.ac.horizon.runspotrun.app.AccessToken;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;

public class ServiceMonitor 
extends Service {
	
	public static enum Reason {
		UPLOAD_COMPLETE,
		REQUIRE_LOGIN,
		REQUIRE_NETWORK,
		REQUIRE_WIFI,
		REQUIRE_POWER,
		REQUIRE_STORAGE;
	}
	
	public static final String ACTION_LOG = "ACTION_LOG";
	
	public static final String ACTION_LOGIN = "ACTION_LOGIN";
	
	public static final String ACTION_VIDEO = "ACTION_VIDEO";
	
	public class BinderServiceMonitor
	extends Binder {
			
		public void logUploadComplete(Reason reason) {
			ServiceMonitor.this.registerReceiver(
					logReceiver, new IntentFilter(
							reasonToAction(reason, ACTION_LOG)));
		}
		
		public void videoUploadComplete(Reason reason) {
			ServiceMonitor.this.registerReceiver(
					videoReceiver, new IntentFilter(
							reasonToAction(reason, ACTION_VIDEO)));
		}
		
		private String reasonToAction(Reason reason, 
				String uploadCompleteAction) {
			switch(reason) {
			case UPLOAD_COMPLETE:
				return uploadCompleteAction;
			case REQUIRE_LOGIN:
				return ACTION_LOGIN;
			case REQUIRE_NETWORK:
				return ConnectivityManager.CONNECTIVITY_ACTION;
			case REQUIRE_WIFI:
				return WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION;
			case REQUIRE_POWER:
				return Intent.ACTION_BATTERY_CHANGED;
			case REQUIRE_STORAGE:
				return Intent.ACTION_MEDIA_MOUNTED;
			default:
				throw new RuntimeException("Unknown reason given"); // UNCAUGHT
			}
		}
		
	}
	
	private BinderServiceMonitor binder = new BinderServiceMonitor();
	
	private final BroadcastReceiver videoReceiver = new BroadcastReceiver() {		
		@Override
		public void onReceive(Context context, Intent intent) {
			startService(new Intent(context, ServiceVideoUpload.class));
			context.unregisterReceiver(this);
		}
	};
	
	private final BroadcastReceiver logReceiver = new BroadcastReceiver() {		
		@Override
		public void onReceive(Context context, Intent intent) {
			startService(new Intent(context, ServiceLogUpload.class));
			context.unregisterReceiver(this);
		}
	};
	
	private AccessToken accessToken;
	
	private AccessToken.Listener accessTokenListener = 
			new AccessToken.Listener() {		
		@Override
		public void changed(String value) {
			if(value != null && !value.isEmpty())
				ServiceMonitor.this.sendBroadcast(new Intent(ACTION_LOGIN));
		}
	};
	
	@Override
	public void onCreate() {
		super.onCreate();
		accessToken = new AccessToken(this);
		accessToken.listen(accessTokenListener);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		accessToken.unlisten(accessTokenListener);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		startService(new Intent(this, ServiceVideoUpload.class));
		startService(new Intent(this, ServiceLogUpload.class));
		return START_STICKY;
	}
	
}
