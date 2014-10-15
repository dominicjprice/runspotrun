package uk.ac.horizon.runspotrun.ui;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.json.JSONObject;

import uk.ac.horizon.runspotrun.Constants;
import uk.ac.horizon.runspotrun.R;
import uk.ac.horizon.runspotrun.data.LogEntry;
import uk.ac.horizon.runspotrun.data.LogEntryDAO;
import uk.ac.horizon.runspotrun.data.VideoEntry;
import uk.ac.horizon.runspotrun.data.VideoEntryDAO;
import uk.ac.horizon.runspotrun.util.Camcorder;
import uk.ac.horizon.runspotrun.util.Display;
import uk.ac.horizon.runspotrun.util.LocationObserver;
import uk.ac.horizon.runspotrun.util.Retry;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

public class CameraActivity 
extends Activity {
	
	@SuppressLint("SimpleDateFormat")
	private static final SimpleDateFormat logDateFormat = 
			new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
	
	private static final int 
		SURFACE_DESTROYED = 0,
		SURFACE_CREATED = 1,
		SURFACE_CHANGED = 2,
		START_RECORDING = 3,
		STOP_RECORDING = 4;
	
	private LocationObserver locationListener = null;
	private Handler cameraHandler = null;
	private String filename = null;
	private String deviceID = null;	
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);
		
		final SurfaceView cameraView = ((SurfaceView)findViewById(R.id.cameraView));
		cameraView.setKeepScreenOn(true);
		final HandlerThread cameraThread = new HandlerThread("runspotrun-camera-thread");
		final android.view.Display display = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
		deviceID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
		
		cameraThread.start();
		cameraHandler = new Handler(cameraThread.getLooper()) {
			
			private final Retry<Camera, RuntimeException> redoUntil = new Retry<>();
			
			private Camera camera = null;
			
			private MediaRecorder recorder = null;
			
			private VideoEntry videoEntry = null;
			
			private void stopCamera() {
				if(camera != null) {
					camera.stopPreview();
				}
			}
			
			private void stopAndReleaseCamera() {
				if(camera != null) {
					camera.stopPreview();
					camera.release();
					camera = null;
				}
			}
			
			private void openCamera() {
				camera = redoUntil.exponentialBackoff(new Retry.Action<Camera, RuntimeException>() {
	        		public Camera doAction() {
	        			return Camera.open();	
	        		}
				}, 100, 5);
			}
			
			private void startCamera() {
				if(camera != null) {
					camera.setDisplayOrientation(Display.screenRotationToDegrees(display.getRotation()));
					try {
						CamcorderProfile prof = Camcorder.getOptimumProfile();
			        	Camera.Parameters params = camera.getParameters();
			        	params.setPreviewSize(prof.videoFrameWidth, prof.videoFrameHeight);
			        	camera.setParameters(params);
					}
					catch(Exception e) { 
						Log.w(Constants.LOG_TAG, "Unable to set camera parameters: " + e.getMessage(), e);
					}
		        	try {
						camera.setPreviewDisplay(cameraView.getHolder());
					} catch(IOException e) {
						throw new RuntimeException(e);
					}
		        	camera.startPreview();
				}
			}
			
			private void startRecording() {
				try {
					File dir = new File(Environment.getExternalStorageDirectory(), "runspotrun");
					if(!dir.exists()) dir.mkdir();
					filename = deviceID + "_" + UUID.randomUUID().toString() + ".mp4";
					File tmp = new File(dir, filename);
					tmp.createNewFile();
					videoEntry = new VideoEntry();
					videoEntry.filename = filename;
					videoEntry.startTime = new Date();
					Location loc = locationListener.getCurrentLocation();
					videoEntry.accuracy = loc.getAccuracy();
					videoEntry.latitude = loc.getLatitude();
					videoEntry.longitude = loc.getLongitude();					
					videoEntry.canUpload = false;					
					videoEntry.uploaded = false;
					
					submitVideoLogEntry(videoEntry.startTime, filename);
					
					camera.unlock();
					recorder = new MediaRecorder();
					recorder.setCamera(camera);
					recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
					recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
					recorder.setProfile(Camcorder.getOptimumProfile());
					recorder.setOutputFile(tmp.getAbsolutePath());
					recorder.prepare();
					recorder.start();
					
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				try { Thread.sleep(1000); }
				catch(InterruptedException e) { }
			}
			
			private void stopRecording() {
				if(recorder != null) {
					try {
						recorder.stop();
					} catch(Exception e) { 
						Log.e(Constants.LOG_TAG, "Unable to stop the camera");
					}
					recorder.reset();
					recorder.release();
					videoEntry.endTime = new Date();
					submitVideoEntry(videoEntry);
					videoEntry = null;
					try {
						camera.reconnect();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					camera.startPreview();
					recorder = null;
					filename = null;
				}			
				try { Thread.sleep(1000); }
				catch(InterruptedException e) { }
			}
			
			@Override
			public void handleMessage(Message msg) {
				switch(msg.what) {
				case SURFACE_CHANGED:
					stopCamera();
					startCamera();
					break;
				case SURFACE_CREATED: 
					stopAndReleaseCamera();
					openCamera();
					break;
				case SURFACE_DESTROYED: 
					stopRecording();
					stopAndReleaseCamera();
					break;
				case START_RECORDING:
					startRecording();
					break;
				case STOP_RECORDING:
					stopRecording();
					break;
				default: super.handleMessage(msg);
				}
			}
		};

		if(Build.VERSION.SDK_INT < 11) {
			cameraView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
		cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
			
			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				cameraHandler.sendEmptyMessage(SURFACE_DESTROYED);
			}
			
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				cameraHandler.sendEmptyMessage(SURFACE_CREATED);
			}
			
			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				Message m = Message.obtain();
				m.what = SURFACE_CHANGED;
				cameraHandler.sendMessage(m);
			}
		});
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		locationListener = new LocationObserver((LocationManager)this.getSystemService(LOCATION_SERVICE));
		locationListener.start();
		locationListener.addLocationChangedListener(new LocationObserver.LocationChangedListener() {
			
			@Override
			public void onLocationChanged(Location location) {
				submitLocationUpdate(location);
			}
		});
		submitLocationUpdate(locationListener.getCurrentLocation());		
	}

	@Override
	protected void onStop() {
		super.onStop();
		locationListener.stop();
	}
	
	public void onClickHomeButton(View view) {
		finish();
	}
	
	public void onClickRecordButton(View view) {
		Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
		if(cameraHandler != null) 
			cameraHandler.sendEmptyMessage(START_RECORDING);
		view.setVisibility(View.INVISIBLE);
		setVisible(findViewById(R.id.recordingBar));
		final View layout = findViewById(R.id.cameraActivityLayout);
		layout.setOnClickListener(new View.OnClickListener() {
			private boolean toggleSwitch = true;
			@Override
			public void onClick(View v) {
				if(toggleSwitch) {
					setVisible(findViewById(R.id.keypad));
					findViewById(R.id.recordingBar).setVisibility(View.INVISIBLE);
				} else {
					findViewById(R.id.keypad).setVisibility(View.INVISIBLE);
					setVisible(findViewById(R.id.recordingBar));
					EditText et = (EditText)findViewById(R.id.tagEntryField);
					et.setText("");
				}
				toggleSwitch = !toggleSwitch;
			}
		});		
	}
	
	public void onClickStopButton(View view) {
		Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
		if(cameraHandler != null) 
			cameraHandler.sendEmptyMessage(STOP_RECORDING);
		
		findViewById(R.id.keypad).setVisibility(View.INVISIBLE);
		findViewById(R.id.recordingBar).setVisibility(View.INVISIBLE);
		setVisible(findViewById(R.id.recordButton));
		final View layout = findViewById(R.id.cameraActivityLayout);
		layout.setOnClickListener(null);
	}
	
	public void onClickHotTagButton(View view) {
		Toast.makeText(this, "Hot spot submitted", Toast.LENGTH_SHORT).show();
		setVisible(findViewById(R.id.recordingBar));
		findViewById(R.id.keypad).setVisibility(View.INVISIBLE);
		
		submitRunnerTag("-99");
		
		final View layout = findViewById(R.id.cameraActivityLayout);
		layout.setOnClickListener(new View.OnClickListener() {
			private boolean toggleSwitch = true;
			@Override
			public void onClick(View v) {
				if(toggleSwitch) {
					setVisible(findViewById(R.id.keypad));
					findViewById(R.id.recordingBar).setVisibility(View.INVISIBLE);
				} else {
					findViewById(R.id.keypad).setVisibility(View.INVISIBLE);
					setVisible(findViewById(R.id.recordingBar));
					EditText et = (EditText)findViewById(R.id.tagEntryField);
					et.setText("");
				}
				toggleSwitch = !toggleSwitch;
			}
		});		
	}
	
	public void onClickKeypadButton(View view) {
		EditText et = (EditText)findViewById(R.id.tagEntryField);
		et.append(view.getTag().toString());
	}
	
	public void onClickDeleteButton(View view) {
		EditText et = (EditText)findViewById(R.id.tagEntryField);
		String s = et.getText().toString();
		if(s.length() > 0) {
			s = s.substring(0, s.length() - 1);
			et.setText(s);
		}
	}
	
	public void onClickSubmitTag(View v) {
		Toast.makeText(this, "Spot submitted", Toast.LENGTH_SHORT).show();
		EditText et = (EditText)findViewById(R.id.tagEntryField);
		if(!et.getText().toString().equals(""))
			submitRunnerTag(et.getText().toString());
		et.setText("");
		findViewById(R.id.keypad).setVisibility(View.INVISIBLE);
		setVisible(findViewById(R.id.recordingBar));
		final View layout = findViewById(R.id.cameraActivityLayout);
		layout.setOnClickListener(new View.OnClickListener() {
			private boolean toggleSwitch = true;
			@Override
			public void onClick(View v) {
				if(toggleSwitch) {
					setVisible(findViewById(R.id.keypad));
					findViewById(R.id.recordingBar).setVisibility(View.INVISIBLE);
				} else {
					findViewById(R.id.keypad).setVisibility(View.INVISIBLE);
					setVisible(findViewById(R.id.recordingBar));
					EditText et = (EditText)findViewById(R.id.tagEntryField);
					et.setText("");
				}
				toggleSwitch = !toggleSwitch;
			}
		});
	}
	
	
	
	private void submitRunnerTag(final String text) {
		(new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				JSONObject obj = new JSONObject();
				Date now = new Date();
				Location location = locationListener.getCurrentLocation();
				try {
					obj.put("guid", deviceID + "_" + UUID.randomUUID().toString());
					obj.put("video_guid", filename);
					obj.put("accuracy", location == null ? 0.0 : location.getAccuracy());
					obj.put("latitude", location == null ? 0.0 : location.getLatitude());
					obj.put("longitude", location == null ? 0.0 : location.getLongitude());
					obj.put("time", logDateFormat.format(now));
					obj.put("runner_number", text);
				} catch (Exception e) {}
				
				LogEntry entry = new LogEntry();
				entry.data = obj.toString();
				entry.endpoint = "runnertag";
				entry.uploaded = false;
				entry.timestamp = now;
				LogEntryDAO logEntryDAO = new LogEntryDAO(CameraActivity.this);
				logEntryDAO.open();
				logEntryDAO.add(entry);
				logEntryDAO.close();
				return null;
			}
		}).execute(null, null);
	}
	
	private void submitLocationUpdate(final Location location) {
		(new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				if(location == null) return null;
				JSONObject obj = new JSONObject();
				Date now = new Date();
				try {
					obj.put("accuracy", location.getAccuracy());
					obj.put("latitude", location.getLatitude());
					obj.put("longitude", location.getLongitude());
					obj.put("time", logDateFormat.format(now));
				} catch (Exception e) {}
				
				LogEntry entry = new LogEntry();
				entry.data = obj.toString();
				entry.endpoint = "positionupdate";
				entry.uploaded = false;
				entry.timestamp = now;
				LogEntryDAO logEntryDAO = new LogEntryDAO(CameraActivity.this);
				logEntryDAO.open();
				logEntryDAO.add(entry);
				logEntryDAO.close();
				return null;
			}
		}).execute(null, null);
	}
	
	private void submitVideoLogEntry(final Date starttime, final String filename) {
		(new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				JSONObject obj = new JSONObject();
				try {
					obj.put("guid", filename);
					obj.put("start_time", logDateFormat.format(starttime));
					obj.put("url", Constants.MEDIA_STORE_BASE_URL 
							+ Constants.MEDIA_STORE_VIDEO_CONTAINER_NAME + "/" + filename);
				} catch (Exception e) {}
				
				LogEntry entry = new LogEntry();
				entry.data = obj.toString();
				entry.endpoint = "video";
				entry.uploaded = false;
				entry.timestamp = starttime;
				LogEntryDAO logEntryDAO = new LogEntryDAO(CameraActivity.this);
				logEntryDAO.open();
				logEntryDAO.add(entry);
				logEntryDAO.close();
				return null;
			}
		}).execute(null, null);
	}
	
	private void submitVideoEntry(final VideoEntry videoEntry) {
		(new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				VideoEntryDAO dao = new VideoEntryDAO(CameraActivity.this);
				dao.open();
				dao.add(videoEntry);
				dao.close();
				return null;
			}
		}).execute(null, null);
	}
	
	private void setVisible(View v) {
		v.setVisibility(View.VISIBLE);
		v.bringToFront();
	}
	
}
