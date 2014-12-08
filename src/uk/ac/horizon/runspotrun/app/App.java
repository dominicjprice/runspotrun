package uk.ac.horizon.runspotrun.app;

import java.io.File;

import uk.ac.horizon.runspotrun.service.ServiceMonitor;
import android.app.Application;
import android.content.Intent;
import android.os.Environment;

public class App
extends Application {
	
	public static final File STORAGE_DIRECTORY = 
			new File(Environment.getExternalStorageDirectory(), "runspotrun");
	
	public static final String XMLNS_URI = 
			"http://www.runspotrun.co.uk/dev/android";
	
	public static final String OAUTH_CLIENT_ID = 
			"c80915f0cfedf11b5cbe";
	
	public static final String OAUTH_CLIENT_SECRET = 
			"2ce731481105e209ddb363f05e6a525ceb8eca43";
	
	@Override
	public void onCreate() {
		super.onCreate();
		startService(new Intent(this, ServiceMonitor.class));
	}

}
