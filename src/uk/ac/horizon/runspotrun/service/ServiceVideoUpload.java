package uk.ac.horizon.runspotrun.service;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.azure.storage.blob.BlockEntry;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import uk.ac.horizon.runspotrun.app.AccessToken;
import uk.ac.horizon.runspotrun.app.App;
import uk.ac.horizon.runspotrun.app.Log;
import uk.ac.horizon.runspotrun.hardware.BatteryMonitor;
import uk.ac.horizon.runspotrun.net.JsonRpc;
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
	
	public interface UploadProgressListener {
		public void progressChanged(EntryVideo video);
	}
	
	public class VideoUploadBinder 
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
	}
	
	private final Object LOCK = new Object();
	
	private final List<UploadProgressListener> listeners = 
			new ArrayList<ServiceVideoUpload.UploadProgressListener>();
	
	private final VideoUploadBinder binder = new VideoUploadBinder();
	
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
	
	private DAOEntryVideo dao;
	
	private BinderServiceMonitor serviceMonitor;
	
	private ConnectivityManager connectivity;
	
	public ServiceVideoUpload() {
		super(ServiceVideoUpload.class.getName());
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		dao = new DAOEntryVideo(this);
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
					String url = rpc.requestVideoUploadUrl(video.filename);
					upload(video, url);
					rpc.submitVideoEncodingJob(video.filename);
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
		return ByteBuffer.wrap(Base64.decode(id, Base64.NO_WRAP)).asIntBuffer().get();
	}
	
	private String encodeId(int id) {
		return Base64.encodeToString(
				ByteBuffer.allocate(4).putInt(id).array(), Base64.NO_WRAP);
	}
	
}
