package uk.ac.horizon.runspotrun.util;

import android.content.pm.ActivityInfo;
import android.view.Surface;

public class Display {
	
	public static int screenRotationToDegrees(int screenRotation) {
		switch(screenRotation) {
		case Surface.ROTATION_90:
			return 0;
		case Surface.ROTATION_180:
			return 270;
		case Surface.ROTATION_270:
			return 180;
		case Surface.ROTATION_0:
		default:
			return 90;
		}
	}
	
	public static int screenRotationToScreenOrientation(int screenRotation) {
		switch(screenRotation) {
		case Surface.ROTATION_90:
			return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
		case Surface.ROTATION_180:
			return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
		case Surface.ROTATION_270:
			return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
		case Surface.ROTATION_0:
		default:
			return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
		}
	}

}
