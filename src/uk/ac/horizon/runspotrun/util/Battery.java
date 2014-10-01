package uk.ac.horizon.runspotrun.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class Battery {

	public final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			currentStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		}
		
	};
	
	private final Context context;
	
	private int currentStatus;
	
	public Battery(Context context) {
		this.context = context;
		Intent batteryStatus = context.registerReceiver(
				broadcastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		currentStatus = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);		
	}
	
	public boolean isCharging() {
		return currentStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
				currentStatus == BatteryManager.BATTERY_STATUS_FULL;
	}
	
	@Override
	protected void finalize() 
	throws Throwable {
		context.unregisterReceiver(broadcastReceiver);
		super.finalize();
	}	

}
