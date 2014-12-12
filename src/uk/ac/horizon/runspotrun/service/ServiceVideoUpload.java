package uk.ac.horizon.runspotrun.service;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONObject;

import com.microsoft.azure.storage.blob.BlockEntry;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import uk.ac.horizon.runspotrun.app.AccessToken;
import uk.ac.horizon.runspotrun.app.App;
import uk.ac.horizon.runspotrun.app.Log;
import uk.ac.horizon.runspotrun.hardware.BatteryMonitor;
import uk.ac.horizon.runspotrun.net.JsonRpc;
import uk.ac.horizon.runspotrun.service.ServiceLogUpload.BinderServiceLogUpload;
import uk.ac.horizon.runspotrun.service.ServiceMonitor.BinderServiceMonitor;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Base64;

public class ServiceVideoUpload
extends Service {

	public static final int BLOCK_SIZE_BYTES = 1024 * 1024 * 4; // 4MB
	
	private static int 
			UPLOAD_STATUS_UPLOADING = 1,
			UPLOAD_STATUS_UPLOADED = 2;
	
	public interface UploadProgressListener {
		public void progressChanged(EntryVideo video);
	}
	
	public class BinderServiceVideoUpload 
	extends Binder {
		
		public List<EntryVideo> listAllVideos() {
			return dao.fetchAll();
		}
		
		public void deleteVideo(EntryVideo entry) {
			new File(App.STORAGE_DIRECTORY, entry.filename).delete();
			dao.delete(entry);
		}
		
		public void uploadVideo(EntryVideo entry) {
			dao.markAsCanUpload(entry);
			sendBroadcast(new Intent(ServiceMonitor.ACTION_VIDEO));
		}
		
		public void addVideo(EntryVideo entry) {
			dao.insert(entry);
			if(!isManualUpload())
				sendBroadcast(new Intent(ServiceMonitor.ACTION_VIDEO));
		}
		
		public void addListener(UploadProgressListener listener) {
			listeners.add(listener);
		}
		
		public void removeListener(UploadProgressListener listener) {
			listeners.remove(listener);
		}
		
		public void process() {
			onHandleIntent(null);
		}
	}
	
	private final Object LOCK = new Object();
	
	private final List<UploadProgressListener> listeners = 
			new ArrayList<ServiceVideoUpload.UploadProgressListener>();
	
	private final BinderServiceVideoUpload binder = 
			new BinderServiceVideoUpload();
	
	private final ServiceConnection monitorConnection = 
			new ServiceConnection() {
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
	
	private final ServiceConnection logUploadConnection = 
			new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			serviceLogUpload = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {	
			synchronized(LOCK) {
				serviceLogUpload = (BinderServiceLogUpload)service;
				LOCK.notifyAll();
			}
		}
	};
	
	private DAOEntryVideo dao;
	
	private BinderServiceMonitor serviceMonitor;
	
	private BinderServiceLogUpload serviceLogUpload;
	
	private ConnectivityManager connectivity;
	
	public ServiceVideoUpload() {
		super(ServiceVideoUpload.class.getName());
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		dao = new DAOEntryVideo(this);
		if(!bindService(new Intent(this, ServiceMonitor.class), 
				monitorConnection, Context.BIND_AUTO_CREATE))
			Log.w("Unable to bind to ServiceMonitor");
		if(!bindService(new Intent(this, ServiceLogUpload.class),
				logUploadConnection, Context.BIND_AUTO_CREATE))
			Log.w("Unable to bind to ServiceLogUpload");		
		connectivity = (ConnectivityManager)
				getSystemService(CONNECTIVITY_SERVICE);
	}
	
	@Override 
	public void onDestroy() {
		super.onDestroy();
		if(serviceMonitor != null) {
			unbindService(monitorConnection);
			serviceMonitor = null;
		}
		if(serviceLogUpload != null) {
			unbindService(logUploadConnection);
			serviceLogUpload = null;
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		synchronized(LOCK) {
			while(serviceMonitor == null
					&& serviceLogUpload == null)
				try { LOCK.wait(); }
				catch(InterruptedException e) { }
		}
		serviceMonitor.videoUploadComplete(process());
	}
	
	private ServiceMonitor.Reason process() {
		
		BatteryMonitor battery = new BatteryMonitor(this);
		EntryVideo video;
		try {
			while((video = dao.fetchSingleEntryToUpload(isManualUpload())) != null) {
				if(!isLoggedIn())
					return ServiceMonitor.Reason.REQUIRE_LOGIN;
				
				final NetworkInfo ni = connectivity.getActiveNetworkInfo();
				if(ni == null || !ni.isConnectedOrConnecting())
					return ServiceMonitor.Reason.REQUIRE_NETWORK;
				
				if(isWifiUpload() && ni.getType() != ConnectivityManager.TYPE_WIFI)
					return ServiceMonitor.Reason.REQUIRE_WIFI;
				
				if(isPoweredUpload() && !battery.isCharging())
					return ServiceMonitor.Reason.REQUIRE_POWER;
				
				if(!Environment.getExternalStorageState().equals(
						Environment.MEDIA_MOUNTED))
					return ServiceMonitor.Reason.REQUIRE_STORAGE;
				
				JsonRpc rpc = new JsonRpc(new AccessToken(this).get());
				try {
					logVideoUploadStatus(
							video.filename, UPLOAD_STATUS_UPLOADING);
					String url = rpc.requestVideoUploadUrl(video.filename);
					upload(video, url);
					rpc.submitVideoEncodingJob(video.filename);
					logVideoUploadStatus(
							video.filename, UPLOAD_STATUS_UPLOADED);
				} catch(Exception e) {
					Log.w(e.getMessage(), e);
					continue;
				}
			}
		} finally {
			battery.destroy();
		}
		
		return ServiceMonitor.Reason.UPLOAD_COMPLETE;
	}
	
	private void upload(EntryVideo video, String url) 
	throws Exception {
		
		final byte[] buffer = new byte[BLOCK_SIZE_BYTES];
		final File file = new File(App.STORAGE_DIRECTORY, video.filename);
		
		if(!file.exists()) {
			dao.markAsUploaded(video);
			return;
		}
		
		CloudBlockBlob blob = new CloudBlockBlob(new URI(url));
		ArrayList<BlockEntry> blocks = blob.exists() 
				? blob.downloadBlockList() 
				: new ArrayList<BlockEntry>();
		BufferedInputStream input = new BufferedInputStream(
				new FileInputStream(file));
		try {
			int id = 0;
			if(blocks.size() > 0) {
				id = decodeId(blocks.get(blocks.size() -1).getId()) + 1;
				input.skip(BLOCK_SIZE_BYTES * id);
			}
			int read;
			while((read = input.read(buffer, 0, BLOCK_SIZE_BYTES)) > 0) {
				BlockEntry block = new BlockEntry(encodeId(id)); 
				blocks.add(block);
				blob.uploadBlock(block.getId(), 
						new ByteArrayInputStream(buffer, 0, read), read);
				blob.commitBlockList(blocks);
				id++;
				
				video.percentUploaded = Math.min(100, (int)((((id + 1) 
						* BLOCK_SIZE_BYTES) / file.length()) * 100));
				dao.update(video);
				for(UploadProgressListener l : listeners)
					l.progressChanged(video);
			}
						
			dao.markAsUploaded(video);
			
		} finally {
			input.close();
		}
	}
	
	private int decodeId(String id) {
		return ByteBuffer.wrap(
				Base64.decode(id, Base64.NO_WRAP)).asIntBuffer().get();
	}
	
	private String encodeId(int id) {
		return Base64.encodeToString(
				ByteBuffer.allocate(4).putInt(id).array(), Base64.NO_WRAP);
	}
	
	private void logVideoUploadStatus(String guid, int status) {
		try {
			JSONObject o = new JSONObject();
			o.put("guid", guid);
			o.put("upload_status", status);
			EntryLog entry = new EntryLog();
			entry.data = o.toString();
			entry.endpoint = "video";
			entry.uploaded = false;
			entry.timestamp = new Date();
			entry.isUpdate = true;
			serviceLogUpload.insert(entry);
		} catch(Exception e) {
			Log.w("Error logging video upload status: " + e.getMessage(), e);
		}
	}
	
}
