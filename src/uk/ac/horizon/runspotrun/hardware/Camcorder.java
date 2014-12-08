package uk.ac.horizon.runspotrun.hardware;

import uk.ac.horizon.runspotrun.app.Log;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;

@SuppressWarnings("deprecation")
public class Camcorder {

	private static final int[] CAMCORDER_PROFILES_TO_TRY = { 
		3, /* QUALITY_CIF */
		4, /* QUALITY_480P */ 
		CamcorderProfile.QUALITY_LOW 
	};	
	
	private static class OptimumProfile {
		private static CamcorderProfile OPTIMUM_PROFILE;
		private static final String MIME_TYPE;
		static {
			for(int i : CAMCORDER_PROFILES_TO_TRY) {
				try {
					CamcorderProfile p = CamcorderProfile.get(i);
					if(p != null) {
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
		if(OptimumProfile.OPTIMUM_PROFILE == null) {
			RuntimeException e = 
					new RuntimeException("No configured CamcorderProfile was available");
			Log.e(e);
			throw e; // UNCAUGHT
		}
		return OptimumProfile.OPTIMUM_PROFILE;
	}
	
	public static String getOptimumProfileMimeType() {
		return OptimumProfile.MIME_TYPE;		
	}
	
	public static MediaRecorder createConfiguredRecorder(
			Camera camera, String outputPath) {
		MediaRecorder recorder = new MediaRecorder();
		recorder.setCamera(camera);
		recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
		recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
		recorder.setProfile(getOptimumProfile());
		recorder.setOutputFile(outputPath);
		return recorder;
	}

}
