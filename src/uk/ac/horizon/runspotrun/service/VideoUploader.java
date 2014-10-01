package uk.ac.horizon.runspotrun.service;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.BlockEntry;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import uk.ac.horizon.runspotrun.Constants;
import uk.ac.horizon.runspotrun.R;
import uk.ac.horizon.runspotrun.data.VideoEntry;
import uk.ac.horizon.runspotrun.data.VideoEntryDAO;
import uk.ac.horizon.runspotrun.util.Battery;
import uk.ac.horizon.runspotrun.util.Camcorder;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

public class VideoUploader
extends IntentService {

	public VideoUploader() {
		super(VideoUploader.class.getName());
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final VideoEntryDAO dao = new VideoEntryDAO(this);
		final Battery battery = new Battery(this);
		final ConnectivityManager cm =
		        (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
		CloudStorageAccount account = null;
		try {
			account = CloudStorageAccount.parse(Constants.MEDIA_STORE_CONNECTION_STRING);
		} catch(Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		
		while(true) {
			InnerLoop: while(true) {
				Log.d(Constants.LOG_TAG, "Checking for videos available to upload");
				final NetworkInfo net = cm.getActiveNetworkInfo();
				if((net == null || !net.isConnected())
						|| (prefs.getString(getString(
								R.string.preference_allowed_connection_types_for_upload), "").equals("wifi")
								&& net.getType() != ConnectivityManager.TYPE_WIFI) 
						|| (prefs.getBoolean(getString(R.string.preference_upload_when_plugged_in_only), false) 
								&& !battery.isCharging())
						|| (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)
								&& !Environment.getExternalStorageState().equals(
										Environment.MEDIA_MOUNTED_READ_ONLY))) {
					break InnerLoop;
				}				
				
				try {
					dao.open();
					boolean manual = prefs.getString(getString(
							R.string.preference_allow_automated_uploads), "manual").equals("manual");
					VideoEntry entry = dao.fetchSingleEntryToUpload(manual);
					if(entry == null)
						break InnerLoop;
					if(entry != null) {
						try {
							CloudBlobClient client = account.createCloudBlobClient();
							CloudBlobContainer container = client.getContainerReference(
									Constants.MEDIA_STORE_VIDEO_CONTAINER_NAME);
							container.createIfNotExists();
							upload(dao, entry, container);										
						} catch(StorageException e) {
							Log.e(Constants.LOG_TAG, e.getMessage() + ": " 
									+ e.getExtendedErrorInformation().getErrorMessage(), e);
							break InnerLoop;
						} catch(Exception e) {
							Log.e(Constants.LOG_TAG, e.getMessage(), e);
							break InnerLoop;
						}
					}
				} finally {
					dao.close();
				}
			}
		
			try { 
				Thread.sleep(Constants.SERVICE_PAUSE_TIME);
			} catch(InterruptedException e) { }
		}
	}
		
	private void upload(
			final VideoEntryDAO dao,
			final VideoEntry entry,
			final CloudBlobContainer container) 
	throws Exception {
		
		final int BLOCK_SIZE = Constants.VIDEO_UPLOAD_BLOCK_SIZE_IN_BYTES;
		final byte[] buffer = new byte[BLOCK_SIZE];
		final File file = new File(Constants.APPLICATION_DIRECTORY, entry.filename);
		
		if(!file.exists()) {
			Log.d(Constants.LOG_TAG, "Video '" + entry.filename + "' has been deleted");
			dao.markAsUploaded(entry);
			return;
		}		
		
		Log.d(Constants.LOG_TAG, "Uploading video '" + entry.filename + "'");
		
		CloudBlockBlob blob = container.getBlockBlobReference(entry.filename);
		ArrayList<BlockEntry> blocks = blob.exists() 
				? blob.downloadBlockList() 
				: new ArrayList<BlockEntry>();
		BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
		try {
			
			int id = 0;
			if(blocks.size() > 0) {
				id = decodeId(blocks.get(blocks.size() -1).getId()) + 1;
				input.skip(BLOCK_SIZE * id);
				Log.d(Constants.LOG_TAG, "Resuming video upload '" 
						+ entry.filename + "' at block " + id);
			}
			int read;
			while((read = input.read(buffer, 0, BLOCK_SIZE)) > 0) {
				BlockEntry block = new BlockEntry(encodeId(id)); 
				blocks.add(block);
				blob.uploadBlock(block.getId(), new ByteArrayInputStream(buffer, 0, read), read);
				blob.commitBlockList(blocks);
				id++;
			}
			
			blob.downloadAttributes();
			BlobProperties props = blob.getProperties();
			props.setContentType(Camcorder.getOptimumProfileMimeType());
			blob.uploadProperties();			
			dao.markAsUploaded(entry);
			
		} finally {
			input.close();
		}
		Log.d(Constants.LOG_TAG, "Video upload complete '" + entry.filename + "'");	
	}
	
	private int decodeId(String id) {
		return ByteBuffer.wrap(Base64.decode(id, Base64.NO_WRAP)).asIntBuffer().get();
	}
	
	private String encodeId(int id) {
		return Base64.encodeToString(
				ByteBuffer.allocate(4).putInt(id).array(), Base64.NO_WRAP);
	}
	
}
