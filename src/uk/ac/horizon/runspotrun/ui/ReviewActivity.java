package uk.ac.horizon.runspotrun.ui;

import java.io.File;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_review);
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
					thumb.setAlpha(0.75f);
					thumb.setBackgroundResource(R.drawable.activity_review_button);
					//thumb.setPadding(5, 0, 5, 0);
					TableRow.LayoutParams lp = (TableRow.LayoutParams)row1.generateLayoutParams(null);
					lp.span = 2;
					//lp.width = bmp.getWidth();
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
					delete.setAlpha(0.75f);
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
					//lp.width = bmp.getWidth();
					lp.weight = 0.25f;
					delete.setLayoutParams(lp);
					row2.addView(delete);
					
					if(!entry.uploaded && !entry.canUpload) {
						Button upload = new Button(ReviewActivity.this);
						upload.setBackgroundResource(R.drawable.activity_review_button);
						upload.setTextColor(getResources().getColorStateList(R.color.activity_review_button));
						upload.setText(R.string.activity_review_upload);
						upload.setHeight(pixels);
						upload.setAlpha(0.75f);
						upload.setPadding(5, 0, 5, 0);
						upload.setOnClickListener(new View.OnClickListener() {
							
							@Override
							public void onClick(View v) {
								row2.removeView(v);		
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

}
