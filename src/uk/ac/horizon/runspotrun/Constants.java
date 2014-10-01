package uk.ac.horizon.runspotrun;

import java.io.File;

import android.media.CamcorderProfile;
import android.os.Environment;

public class Constants {

	public static final String LOG_TAG = "runspotrun";
	
	public static final int[] CAMCORDER_PROFILES_TO_TRY = { 
		3, /* QUALITY_CIF */
		4, /* QUALITY_480P */ 
		CamcorderProfile.QUALITY_LOW 
	};
	
	public static final long LOCATION_UPDATE_MINIMUM_TIME = 60L * 1000L; // 60s
	
	public static final float LOCATION_UPDATE_MINIMUM_DISTANCE = 10f;
	
	public static final String DATABASE_NAME = "runspotrun.db";
	
	public static final int DATABASE_VERSION = 1;
	
	public static final String SERVER_BASE_URL = "https://www.runspotrun.co.uk/";
	
	public static final String SERVER_LOGOUT_URL = SERVER_BASE_URL + "accounts/logout/?next=/";
	
	public static final String SERVER_LOGIN_URL = SERVER_BASE_URL + "accounts/login/?next=/";
	
	public static final String SERVER_TEST_LOGIN_URL = 
			SERVER_BASE_URL + "api/v1/positionupdate/?format=json";

	public static final String MEDIA_STORE_CONNECTION_STRING = ""; 
	
	public static final String MEDIA_STORE_BASE_URL = "http://media.runspotrun.co.uk/";
	
	public static final String MEDIA_STORE_VIDEO_CONTAINER_NAME = "videos";
	
	public static final File APPLICATION_DIRECTORY = 
			new File(Environment.getExternalStorageDirectory(), "runspotrun");

	public static final int VIDEO_UPLOAD_BLOCK_SIZE_IN_BYTES = 1024 * 1024 * 4; // 4MB
	
	public static final long SERVICE_PAUSE_TIME = 60L * 1000L; // 60s
	
	public static final int LOG_ENTRY_UPLOAD_MAX_TRIES = 10;

}
