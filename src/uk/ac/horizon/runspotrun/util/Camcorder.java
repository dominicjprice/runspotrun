package uk.ac.horizon.runspotrun.util;

import uk.ac.horizon.runspotrun.Constants;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.Log;

public class Camcorder {

	private static class OptimumProfile {
		private static CamcorderProfile OPTIMUM_PROFILE;
		private static final String MIME_TYPE;
		static {
			for(int i : Constants.CAMCORDER_PROFILES_TO_TRY) {
				try {
					CamcorderProfile p = CamcorderProfile.get(i);
					if(p != null) {
						Log.d(Constants.LOG_TAG, "Selected optimum CamcorderProfile: " + i);
						OPTIMUM_PROFILE = p;
						break;
					}
				} catch (Exception e) { }
			}
		}
		static {
			if(OptimumProfile.OPTIMUM_PROFILE == null)
				MIME_TYPE = "application/octet-stream";
			else {
				switch(OptimumProfile.OPTIMUM_PROFILE.fileFormat) {
				case MediaRecorder.OutputFormat.AAC_ADTS: 
					MIME_TYPE = "audio/aac";
					break;
				case MediaRecorder.OutputFormat.AMR_NB: 
				case MediaRecorder.OutputFormat.AMR_WB: 
					MIME_TYPE = "audio/AMR";
					break;
				case MediaRecorder.OutputFormat.MPEG_4: 
					MIME_TYPE = "video/mp4";
					break;
				case MediaRecorder.OutputFormat.THREE_GPP: 
					MIME_TYPE = "video/3gpp";
					break;
				case MediaRecorder.OutputFormat.DEFAULT:
				default: 
					MIME_TYPE = "application/octet-stream";
				}
			}
		}
	}		
		
	public static CamcorderProfile getOptimumProfile() {
		if(OptimumProfile.OPTIMUM_PROFILE == null)
			throw new RuntimeException("No configured CamcorderProfile was available");
		return OptimumProfile.OPTIMUM_PROFILE;
	}
	
	public static String getOptimumProfileMimeType() {
		return OptimumProfile.MIME_TYPE;		
	}

}
