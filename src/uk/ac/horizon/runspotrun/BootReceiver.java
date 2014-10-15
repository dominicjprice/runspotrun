package uk.ac.horizon.runspotrun;

import uk.ac.horizon.runspotrun.service.LogUploader;
import uk.ac.horizon.runspotrun.service.VideoUploader;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver
extends BroadcastReceiver {	

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(Constants.LOG_TAG, "Boot completed, uploader services starting");
		context.startService(new Intent(context, LogUploader.class));
		context.startService(new Intent(context, VideoUploader.class));
	}
}
