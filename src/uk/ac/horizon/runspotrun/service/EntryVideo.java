package uk.ac.horizon.runspotrun.service;

import java.util.Date;

public class EntryVideo {

	public long id;
	
	public Date startTime = new Date(0);
	
	public Date endTime = new Date(0);
	
	public String filename = "";
	
	public boolean canUpload = false;
	
	public boolean uploaded = false;
	
	public int percentUploaded = 0;
	
}
