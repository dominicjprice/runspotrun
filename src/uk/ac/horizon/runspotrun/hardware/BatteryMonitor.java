package uk.ac.horizon.runspotrun.hardware;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class BatteryMonitor
extends BroadcastReceiver {
	
	private final Context context;
	
	private int currentStatus;
	
	public BatteryMonitor(Context context) {
		this.context = context;
		Intent batteryStatus = context.registerReceiver(
				this, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		currentStatus = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);		
	}
	
	public boolean isCharging() {
		return currentStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
				currentStatus == BatteryManager.BATTERY_STATUS_FULL;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		currentStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
	}	
	
	public void destroy() {
		try { context.unregisterReceiver(this); }
		catch(Exception e) {}
	}
	
	@Override
	protected void finalize() 
	throws Throwable {
		try { context.unregisterReceiver(this); }
		catch(Exception e) {}
		super.finalize();
	}	

}
