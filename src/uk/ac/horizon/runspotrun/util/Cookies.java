package uk.ac.horizon.runspotrun.util;

import java.util.HashMap;
import java.util.Map;

import uk.ac.horizon.runspotrun.Constants;

import android.util.Log;

public class Cookies {

	private final Map<String, String> data;
	
	private Cookies(Map<String, String> data) {
		this.data = data;
	}
	
	public String get(String key) {
		return data.get(key);
	}
	
	public static Cookies parse(String c) {
		HashMap<String, String> data = new HashMap<>();
		try {
			for(String part : c.split(";")) {
				try {
					String[] pair = part.trim().split("=");
					data.put(pair[0], pair[1]);
				} catch(Exception e) {
					Log.w(Constants.LOG_TAG, "There was an error parsing cookie data", e);
				}
			}
		} catch(Exception e) {
			Log.w(Constants.LOG_TAG, "There was an error parsing cookie data", e);
		}
		return new Cookies(data);
	}

}
