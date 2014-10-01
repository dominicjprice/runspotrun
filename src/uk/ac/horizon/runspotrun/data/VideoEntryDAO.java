package uk.ac.horizon.runspotrun.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class VideoEntryDAO {

	public static final String TABLE_NAME = "video";

	private final Object OPEN_LOCK = new Object();
	
	private final Helper helper;
	
	private SQLiteDatabase database;
	
	private boolean isOpen = false;
	
	public VideoEntryDAO(Context context) {
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

	public VideoEntry add(VideoEntry entry) {
		synchronized (OPEN_LOCK) {
			openGuard();
			ContentValues values = new ContentValues();
			values.put("starttime", entry.startTime.getTime());
			values.put("endtime", entry.endTime.getTime());
			values.put("filename", entry.filename);		
			values.put("accuracy", entry.accuracy);
			values.put("longitude", entry.longitude);
			values.put("latitude", entry.latitude);
			values.put("can_upload", entry.canUpload ? 1 : 0);
			values.put("uploaded", entry.uploaded ? 1 : 0);
			entry.id = database.insert(TABLE_NAME, null, values);
			return entry;
		}
	}
	
	public void update(VideoEntry entry) {
		synchronized (OPEN_LOCK) {
			openGuard();
			ContentValues values = new ContentValues();
			values.put("id", entry.id);
			values.put("starttime", entry.startTime.getTime());
			values.put("endtime", entry.endTime.getTime());
			values.put("filename", entry.filename);
			values.put("accuracy", entry.accuracy);
			values.put("longitude", entry.longitude);
			values.put("latitude", entry.latitude);
			values.put("can_upload", entry.canUpload ? 1 : 0);
			values.put("uploaded", entry.uploaded ? 1 : 0);
			database.update("video", values, "id = ?", new String[]{ Long.toString(entry.id) });
		}
	}	
	
	public List<VideoEntry> fetchAll() {
		synchronized (OPEN_LOCK) {
			openGuard();
			Cursor c = database.query(TABLE_NAME, null, null, null, null, null, null);
			c.moveToFirst();
			ArrayList<VideoEntry> entries = new ArrayList<>();
		    while (!c.isAfterLast()) {
		      entries.add(cursorToEntry(c));
		      c.moveToNext();
		    }
		    c.close();
			return entries;
		}
	}
	
	public VideoEntry fetchSingleEntryToUpload(boolean manual) {
		synchronized (OPEN_LOCK) {
			openGuard();
			String query = manual ? "uploaded = ? AND can_upload = ?" : "uploaded = ?";
			String[] parameters = manual ? new String[]{ "0", "1" } : new String[]{ "0" };
			Cursor c = database.query(
					TABLE_NAME, null, query, parameters, null, null, null, "1");
			c.moveToFirst();
			VideoEntry entry = null;
		    if (!c.isAfterLast())
		      entry = cursorToEntry(c);
		    c.close();
			return entry;
		}
	}
	
	public void markAsUploaded(VideoEntry entry) {
		synchronized (OPEN_LOCK) {
			openGuard();
			ContentValues values = new ContentValues();
			values.put("uploaded", 1);
			database.update(TABLE_NAME, values, "id = ?", new String[]{ Long.toString(entry.id) });
		}
	}
	
	public void markAsCanUpload(VideoEntry entry) {
		synchronized (OPEN_LOCK) {
			openGuard();
			ContentValues values = new ContentValues();
			values.put("can_upload", 1);
			database.update(TABLE_NAME, values, "id = ?", new String[]{ Long.toString(entry.id) });
		}
	}
	
	public void delete(VideoEntry entry) {
		synchronized (OPEN_LOCK) {
			openGuard();
			database.delete(TABLE_NAME, "id = ?", new String[]{ Long.toString(entry.id) });
		}
	}
	
	public void deleteAll() {
		synchronized (OPEN_LOCK) {
			openGuard();
			database.delete(TABLE_NAME, null, null);
		}
	}
	
	private VideoEntry cursorToEntry(Cursor c) {
		VideoEntry e = new VideoEntry();
		e.id = c.getLong(0);
		e.startTime = new Date(c.getLong(1));
		e.endTime = new Date(c.getLong(2));
		e.filename = c.getString(3);
		e.accuracy = c.getDouble(4);
		e.longitude = c.getDouble(5);
		e.latitude = c.getDouble(6);
		e.canUpload = c.getInt(7) == 1;
		e.uploaded = c.getInt(8) == 1;
		return e;
	}
	
	private void openGuard() {
		if (!isOpen)
			throw new RuntimeException("Database is not open");
	}

}
