package uk.ac.horizon.runspotrun.ui;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import uk.ac.horizon.runspotrun.R;
import uk.ac.horizon.runspotrun.app.App;
import uk.ac.horizon.runspotrun.app.Log;
import uk.ac.horizon.runspotrun.hardware.Camcorder;
import uk.ac.horizon.runspotrun.hardware.Display;
import uk.ac.horizon.runspotrun.hardware.LocationObserver;
import uk.ac.horizon.runspotrun.service.EntryLog;
import uk.ac.horizon.runspotrun.service.EntryVideo;
import uk.ac.horizon.runspotrun.service.ServiceLogUpload;
import uk.ac.horizon.runspotrun.service.ServiceVideoUpload;
import uk.ac.horizon.runspotrun.ui.view.RunnerTagEditText;
import uk.ac.horizon.runspotrun.util.Retry;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

@SuppressWarnings("deprecation")
public class ActivityCamera 
extends Activity {
	
	private static final String HOT_TAG_ID = "-99";
	
	private static final long CAMERA_STOP_DELAY_MS = 1500;
	
	@SuppressLint("SimpleDateFormat")
	private static final SimpleDateFormat LOG_ENTRY_DATE_FORMAT = 
			new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
	
	private static final int 
			SURFACE_DESTROYED = 0,
			SURFACE_CREATED = 1,
			SURFACE_CHANGED = 2,
			START_RECORDING = 3,
			STOP_RECORDING = 4;
	
	private final ServiceConnection videoConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			videoService = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {	
			synchronized(SERVICE_LOCK) {
				videoService = (ServiceVideoUpload.VideoUploadBinder)service;
				SERVICE_LOCK.notifyAll();
			}
		}
	};
	
	private final ServiceConnection logConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			logService = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			synchronized(SERVICE_LOCK) {
				logService = (ServiceLogUpload.LogUploadBinder)service;
				SERVICE_LOCK.notifyAll();
			}
		}
	};
	
	private final Object SERVICE_LOCK = new Object();
	
	private LocationObserver locationObserver = null;
	
	private Handler cameraHandler = null;
	
	private String filename = null;
	
	private String deviceID = null;
	
	private ServiceVideoUpload.VideoUploadBinder videoService;
	
	private ServiceLogUpload.LogUploadBinder logService;
	
	private RunnerTagEditText tagEntryField;
	
	private View
			keypad, 
			recordingBar,
			cameraActivityLayout,
			recordButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);
		
		tagEntryField = (RunnerTagEditText)findViewById(R.id.tagEntryField);
		keypad = findViewById(R.id.keypad);
		recordingBar = findViewById(R.id.recordingBar);
		cameraActivityLayout = findViewById(R.id.cameraActivityLayout);
		recordButton = findViewById(R.id.recordButton);
		
		locationObserver = new LocationObserver(
				(LocationManager)this.getSystemService(LOCATION_SERVICE));
		
		if(!bindService(new Intent(this, ServiceVideoUpload.class), 
        		videoConnection, Context.BIND_AUTO_CREATE))
        	Log.w("Unable to bind to ServiceVideoUpload");
		if(!bindService(new Intent(this, ServiceLogUpload.class), 
        		logConnection, Context.BIND_AUTO_CREATE))
        	Log.w("Unable to bind to ServiceLogUpload");
		
		final SurfaceView cameraView = 
				((SurfaceView)findViewById(R.id.cameraView));
		cameraView.setKeepScreenOn(true);
		final HandlerThread cameraThread = 
				new HandlerThread("runspotrun-camera-thread");
		final android.view.Display display = ((WindowManager)
				getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
		deviceID = Settings.Secure.getString(
				getContentResolver(), Settings.Secure.ANDROID_ID);
		
		cameraThread.start();
		cameraHandler = new Handler(cameraThread.getLooper()) {
			
			private final Retry<Camera, RuntimeException> 
					redoUntil = new Retry<>();
			
			private Camera camera = null;
			
			private MediaRecorder recorder = null;
			
			private EntryVideo videoEntry = null;
			
			private void stopCamera() {
				if(camera != null)
					camera.stopPreview();
			}
			
			private void stopAndReleaseCamera() {
				if(camera != null) {
					camera.stopPreview();
					camera.release();
					camera = null;
				}
			}
			
			private void openCamera() {
				camera = redoUntil.exponentialBackoff(
						new Retry.Action<Camera, RuntimeException>() {
	        		public Camera doAction() {
	        			return Camera.open();	
	        		}
				}, 100, 5);
			}
			
			private void startCamera() {
				if(camera != null) {
					camera.setDisplayOrientation(
							Display.screenRotationToDegrees(
									display.getRotation()));
					try {
						CamcorderProfile prof = Camcorder.getOptimumProfile();
			        	Camera.Parameters params = camera.getParameters();
			        	params.setPreviewSize
			        			(prof.videoFrameWidth, prof.videoFrameHeight);
			        	camera.setParameters(params);
					}
					catch(Exception e) { 
						Log.w("Unable to set camera parameters: " 
								+ e.getMessage(), e);
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
					if(!App.STORAGE_DIRECTORY.exists()) 
						App.STORAGE_DIRECTORY.mkdir();
					filename = deviceID + "_" + 
						UUID.randomUUID().toString() + ".mp4";
					File tmp = new File(App.STORAGE_DIRECTORY, filename);
					tmp.createNewFile();
					videoEntry = new EntryVideo();
					videoEntry.filename = filename;
					videoEntry.startTime = new Date();					
					videoEntry.canUpload = false;					
					videoEntry.uploaded = false;
					
					insertVideoLogEntry(videoEntry.startTime, filename);
					
					camera.unlock();
					recorder = Camcorder.createConfiguredRecorder(
							camera, tmp.getAbsolutePath());
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
						Log.e("Unable to stop the camera", e);
					}
					recorder.reset();
					recorder.release();
					videoEntry.endTime = new Date();
					synchronized(SERVICE_LOCK) {
						while(videoService == null)
							try { SERVICE_LOCK.wait(); }
							catch(InterruptedException e) { }
					}
					videoService.addVideo(videoEntry);
					videoEntry = null;
					recorder = null;
					filename = null;
					try {
						camera.reconnect();
					} catch (IOException e) {
						Log.e(e.getMessage(), e);
						throw new RuntimeException(e); // UNCAUGHT
					}
					camera.startPreview();
				}			
				try { Thread.sleep(CAMERA_STOP_DELAY_MS); }
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

		if(Build.VERSION.SDK_INT < 11)
			cameraView.getHolder().setType(
					SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
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
			public void surfaceChanged(
					SurfaceHolder holder, int format, int width, int height) {
				Message m = Message.obtain();
				m.what = SURFACE_CHANGED;
				cameraHandler.sendMessage(m);
			}
		});
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		locationObserver.start();
		locationObserver.addLocationChangedListener(
				new LocationObserver.LocationChangedListener() {	
			@Override
			public void onLocationChanged(Location location) {
				insertLocationUpdate(location);
			}
		});
		insertLocationUpdate(locationObserver.getCurrentLocation());
	}

	@Override
	protected void onStop() {
		super.onStop();
		locationObserver.stop();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(videoService != null) {
			unbindService(videoConnection);
			videoService = null;
		}
		if(logService != null) {
			unbindService(logConnection);
			logService = null;
		}
	}
	
	public void onClickRecordButton(View view) {
		Toast.makeText(this, R.string.activity_camera_recording_started_text,
				Toast.LENGTH_SHORT).show();
		if(cameraHandler != null) 
			cameraHandler.sendEmptyMessage(START_RECORDING);
		setVisible(recordingBar);
		setInvisible(recordButton);
		cameraActivityLayout.setOnClickListener(new View.OnClickListener() {
			private boolean toggle = true;
			@Override
			public void onClick(View v) {
				if(toggle) {
					setVisible(keypad);
					setInvisible(recordingBar);
				} else {
					setVisible(recordingBar);
					setInvisible(keypad);
					tagEntryField.clearRunnerTag();
				}
				toggle = !toggle;
			}
		});		
	}
	
	public void onClickStopButton(View view) {
		Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
		if(cameraHandler != null) 
			cameraHandler.sendEmptyMessage(STOP_RECORDING);
		setVisible(recordButton);
		setInvisible(keypad, recordingBar);
		cameraActivityLayout.setOnClickListener(null);
	}
	
	public void onClickHotTagButton(View view) {
		Toast.makeText(this, R.string.activity_camera_hottag_submitted_text,
				Toast.LENGTH_SHORT).show();
		setVisible(recordingBar);
		setInvisible(keypad);
		insertRunnerTag(HOT_TAG_ID);
		cameraActivityLayout.setOnClickListener(new View.OnClickListener() {
			private boolean toggle = true;
			@Override
			public void onClick(View v) {
				if(toggle) {
					setVisible(keypad);
					setInvisible(recordingBar);
				} else {
					setVisible(recordingBar);
					setInvisible(keypad);
					tagEntryField.clearRunnerTag();
				}
				toggle = !toggle;
			}
		});		
	}
	
	public void onClickKeypadButton(View view) {
		tagEntryField.appendRunnerTag(view.getTag().toString());
	}
	
	public void onClickDeleteButton(View view) {
		tagEntryField.backspaceRunnerTag();
	}
	
	public void onClickSubmitTag(View v) {
		if(tagEntryField.getRunnerTag().isEmpty())
			return;
		Toast.makeText(this, R.string.activity_camera_tag_submitted_text,
				Toast.LENGTH_SHORT).show();
		insertRunnerTag(tagEntryField.getAndClearRunnerTag());
		setVisible(recordingBar);
		setInvisible(keypad);
		cameraActivityLayout.setOnClickListener(new View.OnClickListener() {
			private boolean toggle = true;
			@Override
			public void onClick(View v) {
				if(toggle) {
					setVisible(keypad);
					setInvisible(recordingBar);
				} else {
					setVisible(recordingBar);
					setInvisible(keypad);
					tagEntryField.clearRunnerTag();
				}
				toggle = !toggle;
			}
		});
	}
	
	private void setVisible(View... views) {
		for(View v : views) {
			v.setVisibility(View.VISIBLE);
			v.bringToFront();
		}
	}
	
	private void setInvisible(View... views) {
		for(View v : views)
			v.setVisibility(View.INVISIBLE);
	}
	
	private void insertRunnerTag(String text) {
		Date now = new Date();
		Map<String, Object> data = new HashMap<String, Object>();
		Location location = locationObserver.getCurrentLocation();
		data.put("guid", deviceID + "_" + UUID.randomUUID().toString());
		data.put("video_guid", filename);
		data.put("accuracy", location.getAccuracy());
		data.put("latitude", location.getLatitude());
		data.put("longitude", location.getLongitude());
		data.put("time", LOG_ENTRY_DATE_FORMAT.format(now));
		data.put("runner_number", text);
		insertLogEntry("runnertag", now, data);
	}
	
	private void insertLocationUpdate(Location location) {
		Date now = new Date();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("accuracy", location.getAccuracy());
		data.put("latitude", location.getLatitude());
		data.put("longitude", location.getLongitude());
		data.put("time", LOG_ENTRY_DATE_FORMAT.format(now));
		insertLogEntry("positionupdate", now, data);
	}
	
	private void insertVideoLogEntry(Date starttime, String filename) {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("guid", filename);
		data.put("start_time", LOG_ENTRY_DATE_FORMAT.format(starttime));
		data.put("url", filename);
		insertLogEntry("video", starttime, data);
	}
	
	private void insertLogEntry(
			final String endpoint,
			final Date date,
			final Map<String, Object> data) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				synchronized(SERVICE_LOCK) {
					while(logService == null)
						try { SERVICE_LOCK.wait(); }
						catch(InterruptedException e) { }
				}
				JSONObject o = new JSONObject();
				try {
					for(Entry<String, Object> e : data.entrySet())
						o.put(e.getKey(), e.getValue());
				} catch(JSONException e) {
					Log.w("Error creating JSON object: " + e.getMessage(), e);
					return;
				}
				EntryLog entry = new EntryLog();
				entry.data = o.toString();
				entry.endpoint = endpoint;
				entry.uploaded = false;
				entry.timestamp = date;
				logService.insert(entry);				
			}
		}).start();
	}
	
}
