package uk.ac.horizon.runspotrun.ui;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

public class ActivityReview 
extends Activity
implements ServiceVideoUpload.UploadProgressListener {

	private static final int DISPLAY_SLICE_SIZE = 10;
	
	private static final SimpleDateFormat DATE_FORMAT = 
			new SimpleDateFormat("HH:mm dd/MM/yy", Locale.getDefault());
	
	private final ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			service = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {	
			ActivityReview.this.service = 
					(ServiceVideoUpload.BinderServiceVideoUpload)service;
			ActivityReview.this.service.addListener(ActivityReview.this);
			videos = ActivityReview.this.service.listAllVideos();
			Collections.sort(videos, new Comparator<EntryVideo>() {
				@Override
				public int compare(EntryVideo lhs, EntryVideo rhs) {
					return rhs.startTime.compareTo(lhs.startTime);
				}
			});
			layout(currentPage);
		}
	};
	
	private ServiceVideoUpload.BinderServiceVideoUpload service;
	
	@SuppressLint("UseSparseArrays")
	private Map<Long, Button> buttons = new HashMap<Long, Button>();
	
	private List<EntryVideo> videos = new ArrayList<EntryVideo>();
	
	private int currentPage = 0;
	
	private ScrollView scrollView;
	
	private ProgressBar progressBar;
	
	public void onClickClose(View v) {
		close();
	}
	
	public void onClickLoadMore(View v) {
		layout(++currentPage);
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
		if(!bindService(new Intent(this, ServiceVideoUpload.class), 
        		connection, Context.BIND_AUTO_CREATE))
        	Log.w("Unable to bind to ServiceVideoUpload");
		scrollView = (ScrollView)findViewById(R.id.activity_review_scrollview);
		progressBar = (ProgressBar)findViewById(R.id.activity_review_spinner);
		progressBar.setVisibility(View.GONE);
	}
	
	protected void onDestroy() {
		super.onDestroy();
		if(service != null) {
			service.removeListener(this);
			unbindService(connection);
			service = null;
		}
	}

	private void layout(final int page) {
		final LinearLayout parent = 
				(LinearLayout)findViewById(R.id.review_layout);
		final LayoutInflater inflater = (LayoutInflater)getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		
		(new AsyncTask<Void, View, Void>() {

			private List<EntryVideo> entries;
			
			private View first = null;
			
			private ViewPropertyAnimatorCompat spinnerAnimation;
			
			private ViewPropertyAnimatorCompat viewAnimation;
			
			int duration = getResources().getInteger(
					android.R.integer.config_shortAnimTime);
			
			@Override
			protected void onPostExecute(Void result) {
				spinnerAnimation.alpha(0f).setDuration(duration)
						.setListener(new ViewPropertyAnimatorListener() {
							@Override
							public void onAnimationStart(View v) {}
							@Override
							public void onAnimationEnd(View v) {
								v.setVisibility(View.GONE);
							}
							@Override
							public void onAnimationCancel(View v) {}
						});
				viewAnimation.alpha(1f).setDuration(duration);
				if(first != null)
					scrollView.post(new Runnable() {
						@Override
						public void run() {
							scrollView.smoothScrollTo(0, 
									first.getBottom() - scrollView.getHeight());
						}
					});
			}

			@Override
			protected void onPreExecute() {
				entries = getPageEntries(page);
				ViewCompat.setAlpha(progressBar, 0f);
				progressBar.setVisibility(View.VISIBLE);
				spinnerAnimation = ViewCompat.animate(progressBar);
				spinnerAnimation.alpha(1f).setDuration(duration);
				viewAnimation = ViewCompat.animate(scrollView);
				viewAnimation.alpha(0.5f).setDuration(duration);
			}

			@Override
			protected void onProgressUpdate(View... values) {
				for(final View v : values) {
					parent.addView(v);
					if(first == null)
						first = v;
				}
			}

			@Override
			protected Void doInBackground(Void... params) {	
				for(final EntryVideo video : entries) {
					final File file = new File(
							App.STORAGE_DIRECTORY, video.filename);
					if (!file.exists()) {
						service.deleteVideo(video);
						continue;
					}
					
					final TableLayout view = (TableLayout)inflater.inflate(
							R.layout.layout_review_item, parent, false);
					
					TextView text = (TextView)
							view.findViewById(R.id.activity_review_item_text);
					text.setText(formatDetails(video));
					
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
					
					Button delete = (Button)view.findViewById(
							R.id.activity_review_delete_button);
					delete.setOnClickListener(new View.OnClickListener() {						
						@Override
						public void onClick(View v) {
							view.setVisibility(View.GONE);
							parent.removeView(view);
							service.deleteVideo(video);
						}
					});
					
					final Button upload = (Button)view.findViewById(
							R.id.activity_review_upload_button);
					buttons.put(video.id, upload);
					if(!video.uploaded && !video.canUpload) {
						upload.setText(R.string.activity_review_upload);
						upload.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								upload.setOnClickListener(null);
								upload.setText(R.string
										.activity_review_upload_pending);
								service.uploadVideo(video);
							}
						});
					} else if(!video.uploaded && video.canUpload) {
						if(video.percentUploaded == 0L)
							upload.setText(
									R.string.activity_review_upload_pending);
						else
							upload.setText(String.format(getString(
									R.string.activity_review_uploading),
									video.percentUploaded));
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
	
	private List<EntryVideo> getPageEntries(int page) {
		int start = page * DISPLAY_SLICE_SIZE;
		if(start >= videos.size())
			return new ArrayList<EntryVideo>();
		int end = start + DISPLAY_SLICE_SIZE;
		if(end > videos.size()) {
			end = videos.size();
			findViewById(R.id.activity_review_load_button).setEnabled(false);
		}
		return videos.subList(start, end);
	}
	
	private String formatDetails(EntryVideo video) {
		long length = (video.endTime.getTime() 
				- video.startTime.getTime()) / 1000;
		return "Date: " + DATE_FORMAT.format(video.startTime) + "\nLength: "
				+ length + " seconds";
	}

}
