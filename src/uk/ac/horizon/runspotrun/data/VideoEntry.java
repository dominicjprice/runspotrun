package uk.ac.horizon.runspotrun.data;

import java.util.Date;

public class VideoEntry {

	public long id;
	
	public Date startTime = new Date(0);
	
	public Date endTime = new Date(0);
	
	public String filename = "";
	
	public double accuracy = 0f;
	
	public double latitude = 0f;
	
	public double longitude = 0f;
	
	public boolean canUpload = false;
	
	public boolean uploaded = false;
	
}
