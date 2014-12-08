package uk.ac.horizon.runspotrun.app;

import java.util.ArrayList;

import android.content.Context;
import android.preference.PreferenceManager;

public class AccessToken {

	public static interface Listener {
		public void changed(String value);
	}
	
	private final ArrayList<Listener> listeners = new ArrayList<Listener>();
	
	private final StringPreference pref;
	
	public AccessToken(Context context) {
		pref = new StringPreference(
				PreferenceManager.getDefaultSharedPreferences(context),
				"ACCESS_TOKEN",
				null,
				new StringPreference.Listener() {
					@Override
					public void changed(String value) {
						synchronized(listeners) {
							for(Listener l : listeners)
								l.changed(value);
						}
						
					}
				});
	}
	
	public String get() {
		return pref.get();
	}
	
	public void set(String value) {
		pref.set(value);
	}
	
	public void unset() {
		pref.unset();
	}
	
	public void listen(Listener listener) {
		synchronized(listeners) {
			listeners.add(listener);
		}
	}
	
	public void unlisten(Listener listener) {
		synchronized(listeners) {
			listeners.remove(listener);
		}
	}

}
