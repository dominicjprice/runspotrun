package uk.ac.horizon.runspotrun.hardware;

import java.util.ArrayList;
import java.util.List;

import uk.ac.horizon.runspotrun.app.Log;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

public class LocationObserver {
	
	private static final long MINIMUM_TIME = 60L * 1000L; // 60s
	
	private static final float MINIMUM_DISTANCE = 10f;
	
	private static final Location NULL_LOCATION = new Location("null") {
		{ this.setAccuracy(-1f); }
	};
	
	public static interface LocationChangedListener {
		public void onLocationChanged(Location location);
	}
	
	private final Object RUN_LOCK = new Object();

	private final LocationManager locationManager;
	
	private final List<LocationChangedListener> listeners = new ArrayList<LocationChangedListener>();
	
	private final android.location.LocationListener listener = new android.location.LocationListener() {
		
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) { }
		
		@Override
		public void onProviderEnabled(String provider) { }
		
		@Override
		public void onProviderDisabled(String provider) { }
		
		@Override
		public void onLocationChanged(Location location) {
			setCurrentLocation(location);
		}
	};
	
	private Location currentLocation = NULL_LOCATION;
	
	private boolean isRunning = false;
	
	public LocationObserver(LocationManager locationManager) {
		this.locationManager = locationManager;
	}
	
	public void addLocationChangedListener(LocationChangedListener listener) {
		listeners.add(listener);
	}
	
	public void removeLocationChangedListener(LocationChangedListener listener) {
		listeners.remove(listener);
	}
	
	public void start() {
		synchronized(RUN_LOCK) {
			if(!isRunning) {
				for(String provider : locationManager.getAllProviders()) {
					locationManager.requestLocationUpdates(
							provider,
							MINIMUM_TIME,
							MINIMUM_DISTANCE,
							listener);
					setCurrentLocation(locationManager.getLastKnownLocation(provider));
					isRunning = true;
				}
			}
		}
	}
	
	public void stop() {
		synchronized(RUN_LOCK) {
			if(isRunning) {
				locationManager.removeUpdates(listener);
				isRunning = false;
			}
		}
	}

	public Location getCurrentLocation() {
		return currentLocation == null 
				? NULL_LOCATION
				: currentLocation;
	}
	
	@Override
	protected void finalize()
	throws Throwable {
		stop();
		super.finalize();
	}

	private void setCurrentLocation(Location location) {
		if(location == null)
			return;
		if(currentLocation.getAccuracy() < 0f
				|| location.getAccuracy() <= currentLocation.getAccuracy()
				|| (location.getTime() - currentLocation.getTime()) 
						> MINIMUM_TIME ) {
			Log.v("Current location updated: " + location.toString());
			currentLocation = location;
			for(LocationChangedListener listener : listeners)
				listener.onLocationChanged(location);
		}
	}

}
