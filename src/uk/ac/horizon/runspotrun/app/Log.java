package uk.ac.horizon.runspotrun.app;

public class Log {

	private static final String LOG_TAG = "runspotrun";
	
	public static void d(String msg) {
		android.util.Log.d(LOG_TAG, msg);
	}
	
	public static void e(String msg, Throwable tr) {
		android.util.Log.e(LOG_TAG, msg, tr);
	}
	
	public static void e(Throwable tr) {
		android.util.Log.e(LOG_TAG, tr.getMessage(), tr);
	}
	
	public static void v(String msg) {
		android.util.Log.v(LOG_TAG, msg);
	}
	
	public static void w(String msg) {
		android.util.Log.w(LOG_TAG, msg);
	}
	
	public static void w(String msg, Throwable tr) {
		android.util.Log.w(LOG_TAG, msg, tr);
	}
	
	public static void w(Throwable tr) {
		android.util.Log.w(LOG_TAG, tr.getMessage(), tr);
	}
	
}
