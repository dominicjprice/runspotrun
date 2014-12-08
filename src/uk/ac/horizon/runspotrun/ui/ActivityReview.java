package uk.ac.horizon.runspotrun.ui;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import uk.ac.horizon.runspotrun.R;
import uk.ac.horizon.runspotrun.app.App;
import uk.ac.horizon.runspotrun.app.Log;
import uk.ac.horizon.runspotrun.hardware.Camcorder;
import uk.ac.horizon.runspotrun.service.EntryVideo;
import uk.ac.horizon.runspotrun.service.ServiceVideoUpload;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

public class ActivityReview 
extends Activity
implements ServiceVideoUpload.UploadProgressListener {

	private final ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			service = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {	
			ActivityReview.this.service = (ServiceVideoUpload.VideoUploadBinder)service;
			ActivityReview.this.service.addListener(ActivityReview.this);
			layout();
		}
	};
	
	private ServiceVideoUpload.VideoUploadBinder service;
	
	private Map<Long, Button> buttons = new HashMap<Long, Button>();
	
	public void onClickClose(View v) {
		close();
	}
	
	@Override
	public void progressChanged(final EntryVideo video) {
		runOnUiThread(new Runnable() {			
			@Override
			public void run() {
				if(buttons.containsKey(video.id)) {
					Button b = buttons.get(video.id);
					if(!video.uploaded && video.canUpload 
							&& video.percentUploaded < 100)
						b.setText(String.format(getString(
								R.string.activity_review_uploading), 
								video.percentUploaded));
					else
						b.setText(R.string.activity_review_uploaded);
				}
			}
		});
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_review);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
        if(!bindService(new Intent(this, ServiceVideoUpload.class), 
        		connection, Context.BIND_AUTO_CREATE))
        	Log.w("Unable to bind to ServiceVideoUpload");
	}
	
	protected void onStop() {
		super.onStop();
		if(service != null) {
			service.removeListener(this);
			unbindService(connection);
			service = null;
		}
	}

	private void layout() {
		final LinearLayout parent = (LinearLayout)findViewById(R.id.review_layout);
		final LayoutInflater inflater = (LayoutInflater)getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		parent.removeAllViews();
		buttons.clear();
		
		(new AsyncTask<Void, View, Void>() {

			@Override
			protected void onProgressUpdate(View... values) {
				for(View v : values)
					parent.addView(v);
			}

			@Override
			protected Void doInBackground(Void... params) {	
				for(final EntryVideo video : service.listAllVideos()) {
					final File file = new File(
							App.STORAGE_DIRECTORY, video.filename);
					if (!file.exists()) {
						service.deleteVideo(video);
						continue;
					}
					
					final TableLayout view = (TableLayout)inflater.inflate(
							R.layout.layout_review_item, parent, false);
					
					ImageButton thumb = (ImageButton)view.findViewById(
							R.id.activity_review_thumbnail_button);
					thumb.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent i = new Intent();
							i.setAction(Intent.ACTION_VIEW);
							i.setDataAndType(Uri.fromFile(file), 
									Camcorder.getOptimumProfileMimeType());
							startActivity(i);					
						}
					});
					thumb.setImageBitmap(ThumbnailUtils.createVideoThumbnail(
							file.getAbsolutePath(), 
							MediaStore.Video.Thumbnails.MINI_KIND));
					
					Button delete = (Button)view.findViewById(R.id.activity_review_delete_button);
					delete.setOnClickListener(new View.OnClickListener() {						
						@Override
						public void onClick(View v) {
							view.setVisibility(View.GONE);
							parent.removeView(view);
							service.deleteVideo(video);
						}
					});
					
					final Button upload = (Button)view.findViewById(R.id.activity_review_upload_button);
					buttons.put(video.id, upload);
					if(!video.uploaded && !video.canUpload) {
						upload.setText(R.string.activity_review_upload);
						upload.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								upload.setOnClickListener(null);
								upload.setText(R.string.activity_review_upload_pending);
								service.uploadVideo(video);
							}
						});
					} else if(!video.uploaded && video.canUpload) {
						if(video.percentUploaded == 0L)
							upload.setText(R.string.activity_review_upload_pending);
						else
							upload.setText(String.format(getString(
									R.string.activity_review_uploading), video.percentUploaded));
					} else {
						upload.setText(R.string.activity_review_uploaded);
					}
					publishProgress(view);

				}			
				return null;
			}
			
		}).execute(null, null);
	}
	
	private void close() {
		finish();
	}

}
