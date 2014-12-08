package uk.ac.horizon.runspotrun.service;

import java.util.Date;

public class EntryLog {

	public long id;
	
	public Date timestamp = new Date(0);
	
	public String endpoint = "";
	
	public String data = "";	
	
	public boolean uploaded = false;
	
	public int uploadRetries = 0;
	
	public boolean uploadFailed = false;
	
	public boolean isUpdate = false;

}
