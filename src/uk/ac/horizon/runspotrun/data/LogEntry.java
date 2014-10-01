package uk.ac.horizon.runspotrun.data;

import java.util.Date;

public class LogEntry {

	public long id;
	
	public Date timestamp = new Date(0);
	
	public String endpoint = "";
	
	public String data = "";	
	
	public boolean uploaded = false;
	
	public int upload_retries = 0;
	
	public boolean upload_failed = false;

}
