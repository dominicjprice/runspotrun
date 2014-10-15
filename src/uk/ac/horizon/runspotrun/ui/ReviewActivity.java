package uk.ac.horizon.runspotrun.ui;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import uk.ac.horizon.runspotrun.Constants;
import uk.ac.horizon.runspotrun.R;
import uk.ac.horizon.runspotrun.data.VideoEntry;
import uk.ac.horizon.runspotrun.data.VideoEntryDAO;
import uk.ac.horizon.runspotrun.util.Camcorder;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.app.Activity;
import android.content.Intent;

public class ReviewActivity 
extends Activity {

	private Method setAlphaMethod = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_review);
		
		try {
			setAlphaMethod = View.class.getMethod("setAlpha", float.class);
		} catch (NoSuchMethodException e) {}
	}

	@Override
	protected void onResume() {
		super.onResume();
		final TableLayout parent = (TableLayout)findViewById(R.id.review_layout);
		parent.removeAllViews();
		
		(new AsyncTask<Void, View, Void>() {

			@Override
			protected void onProgressUpdate(View... values) {
				for(View v : values)
					parent.addView(v);
			}

			@Override
			protected Void doInBackground(Void... params) {
				VideoEntryDAO dao = new VideoEntryDAO(ReviewActivity.this);
				dao.open();
				List<VideoEntry> entries = dao.fetchAll();
				dao.close();
				
				for(final VideoEntry entry : entries) {
					final File file = new File(
							Constants.APPLICATION_DIRECTORY, entry.filename);
					if (!file.exists()) {
						dao.open();
						dao.delete(entry);
						dao.close();
						continue;
					}
					
					LayoutParams l = new LayoutParams();
					
					final TableRow row1 = new TableRow(ReviewActivity.this);
					
					
					ImageButton thumb = new ImageButton(ReviewActivity.this);
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
					Bitmap bmp = ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), 
							MediaStore.Video.Thumbnails.MINI_KIND);
					
					thumb.setImageBitmap(bmp);
					setAlpha(thumb, 0.75f);
					thumb.setBackgroundResource(R.drawable.activity_review_button);
					TableRow.LayoutParams lp = (TableRow.LayoutParams)row1.generateLayoutParams(null);
					lp.span = 2;
					lp.weight = 0.5f;
					thumb.setLayoutParams(lp);
					row1.addView(thumb);
					
					final TableRow row2 = new TableRow(ReviewActivity.this);
					
					
					DisplayMetrics metrics = getResources().getDisplayMetrics();
					float dp = 50f;
					float fpixels = metrics.density * dp;
					int pixels = (int) (fpixels + 0.5f);
					l.setMargins(0, 0, 0, ((int) ((metrics.density * 2) + 0.5f)));
					row2.setLayoutParams(l);
					
					Button delete = new Button(ReviewActivity.this);
					delete.setText(R.string.activity_review_delete);
					delete.setBackgroundResource(R.drawable.activity_review_button);
					delete.setTextColor(getResources().getColorStateList(R.color.activity_review_button));
					delete.setHeight(pixels);
					delete.setPadding(5, 0, 5, 0);
					setAlpha(delete, 0.75f);
					delete.setOnClickListener(new View.OnClickListener() {
						
						@Override
						public void onClick(View v) {
							row1.setVisibility(View.GONE);
							row2.setVisibility(View.GONE);
							parent.removeView(row1);
							parent.removeView(row2);
							(new AsyncTask<Void, Void, Void>() {

								@Override
								protected Void doInBackground(Void... params) {
									VideoEntryDAO dao = new VideoEntryDAO(ReviewActivity.this);
									dao.open();
									dao.delete(entry);
									dao.close();
									file.delete();
									return null;
								}
								
							}).execute(null, null);
						}
					});
					lp = (TableRow.LayoutParams)row2.generateLayoutParams(null);
					lp.span = 1;
					lp.weight = 0.25f;
					delete.setLayoutParams(lp);
					row2.addView(delete);
					
					if(!entry.uploaded && !entry.canUpload) {
						final Button upload = new Button(ReviewActivity.this);
						upload.setBackgroundResource(R.drawable.activity_review_button);
						upload.setTextColor(getResources().getColorStateList(R.color.activity_review_button));
						upload.setText(R.string.activity_review_upload);
						upload.setHeight(pixels);
						setAlpha(upload, 0.75f);
						upload.setPadding(5, 0, 5, 0);
						upload.setOnClickListener(new View.OnClickListener() {
							
							@Override
							public void onClick(View v) {
								upload.setOnClickListener(null);
								upload.setText(R.string.activity_review_uploading);
								(new AsyncTask<Void, Void, Void>() {

									@Override
									protected Void doInBackground(Void... params) {
										VideoEntryDAO dao = new VideoEntryDAO(ReviewActivity.this);
										dao.open();
										dao.markAsCanUpload(entry);
										dao.close();
										return null;
									}
									
								}).execute(null, null);
							}
						});
						upload.setLayoutParams(lp);
						row2.addView(upload);
					} else if(!entry.uploaded && entry.canUpload) {
						final Button uploading = new Button(ReviewActivity.this);
						uploading.setBackgroundResource(R.drawable.activity_review_button);
						uploading.setTextColor(getResources().getColorStateList(R.color.activity_review_button));
						uploading.setText(R.string.activity_review_uploading);
						uploading.setHeight(pixels);
						setAlpha(uploading, 0.75f);
						uploading.setPadding(5, 0, 5, 0);
						uploading.setLayoutParams(lp);
						row2.addView(uploading);
					} else {
						final Button uploaded = new Button(ReviewActivity.this);
						uploaded.setBackgroundResource(R.drawable.activity_review_button);
						uploaded.setTextColor(getResources().getColorStateList(R.color.activity_review_button));
						uploaded.setText(R.string.activity_review_uploaded);
						uploaded.setHeight(pixels);
						setAlpha(uploaded, 0.75f);
						uploaded.setPadding(5, 0, 5, 0);
						uploaded.setLayoutParams(lp);
						row2.addView(uploaded);
					}
					
					TableLayout tl = new TableLayout(ReviewActivity.this);
					tl.addView(row1);
					tl.addView(row2);
					
					publishProgress(tl);

				}			
				return null;
			}
			
		}).execute(null, null);
	}
	
	private void setAlpha(View view, float alpha) {
		if(setAlphaMethod == null)
			return;
		try {
			setAlphaMethod.invoke(view, Float.valueOf(alpha));
		} catch (Exception e) {} 
	}

}
