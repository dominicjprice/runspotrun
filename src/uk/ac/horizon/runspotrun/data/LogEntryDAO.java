package uk.ac.horizon.runspotrun.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class LogEntryDAO {
	
	public static final String TABLE_NAME = "log";

	private final Object OPEN_LOCK = new Object();
	
	private final Helper helper;
	
	private SQLiteDatabase database;
	
	private boolean isOpen = false;
	
	public LogEntryDAO(Context context) {
		helper = new Helper(context);
	}
	
	public void open() {
		synchronized (OPEN_LOCK) {
			if (!isOpen) {
				database = helper.getWritableDatabase();
				isOpen = true;
			}
		}
	}
	
	public void close() {
		synchronized (OPEN_LOCK) {
			if (isOpen) {
				helper.close();
				database = null;
				isOpen = false;
			}
		}
	}

	public LogEntry add(LogEntry entry) {
		synchronized (OPEN_LOCK) {
			openGuard();
			ContentValues values = new ContentValues();
			values.put("timestamp", entry.timestamp.getTime());
			values.put("endpoint", entry.endpoint);			
			values.put("data", entry.data);
			values.put("uploaded", entry.uploaded ? 1 : 0);
			values.put("upload_retries", entry.upload_retries);
			values.put("upload_failed", entry.upload_failed ? 1 : 0);
			entry.id = database.insert(TABLE_NAME, null, values);
			return entry;	
		}		
	}
	
	public List<LogEntry> fetchNotUploaded() {
		synchronized (OPEN_LOCK) {
			openGuard();
			Cursor c = database.query(
					TABLE_NAME, null, "uploaded = ? AND upload_failed = ?", 
							new String[]{ "0", "0" }, null, null, null); 
			c.moveToFirst();
			ArrayList<LogEntry> entries = new ArrayList<>();
		    while (!c.isAfterLast()) {
		      entries.add(cursorToEntry(c));
		      c.moveToNext();
		    }
		    c.close();
			return entries;
		}
	}
	
	public void markAsUploaded(LogEntry entry) {
		synchronized (OPEN_LOCK) {
			openGuard();
			ContentValues values = new ContentValues();
			values.put("uploaded", 1);
			database.update(TABLE_NAME, values, "id = ?", new String[]{ Long.toString(entry.id) });
		}
	}
	
	public void markAsUploadFailed(LogEntry entry) {
		synchronized (OPEN_LOCK) {
			openGuard();
			ContentValues values = new ContentValues();
			values.put("upload_failed", 1);
			database.update(TABLE_NAME, values, "id = ?", new String[]{ Long.toString(entry.id) });
		}
	}
	
	public void save(LogEntry entry) {
		synchronized (OPEN_LOCK) {
			openGuard();
			ContentValues values = new ContentValues();
			values.put("timestamp", entry.timestamp.getTime());
			values.put("endpoint", entry.endpoint);			
			values.put("data", entry.data);
			values.put("uploaded", entry.uploaded ? 1 : 0);
			values.put("upload_retries", entry.upload_retries);
			values.put("upload_failed", entry.upload_failed ? 1 : 0);
			database.update(TABLE_NAME, values, "id = ?", new String[]{ Long.toString(entry.id) });	
		}		
	}
	
	public void deleteAll() {
		synchronized (OPEN_LOCK) {
			openGuard();
			database.delete("log", null, null);
		}
	}
	
	private LogEntry cursorToEntry(Cursor c) {
		LogEntry e = new LogEntry();
		e.id = c.getLong(0);
		e.timestamp = new Date(c.getLong(1));
		e.endpoint = c.getString(2);
		e.data = c.getString(3);
		e.uploaded = c.getLong(4) != 0L;
		e.upload_retries = c.getInt(5);
		e.upload_failed = c.getLong(6) != 0L;
		return e;
	}
	
	private void openGuard() {
		if (!isOpen)
			throw new RuntimeException("Database is not open");
	}
	
}
