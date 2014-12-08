package uk.ac.horizon.runspotrun.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

class DAOEntryLog
extends DAO {
	
	public static final String TABLE_NAME = "log";
	
	public DAOEntryLog(Context context) {
		super(context);
	}

	public void insert(EntryLog entry) {
		final ContentValues values = new ContentValues();
		values.put("timestamp", entry.timestamp.getTime());
		values.put("endpoint", entry.endpoint);			
		values.put("data", entry.data);
		values.put("uploaded", entry.uploaded ? 1 : 0);
		values.put("upload_retries", entry.uploadRetries);
		values.put("upload_failed", entry.uploadFailed ? 1 : 0);
		values.put("is_update", entry.isUpdate ? 1 : 0);
		writeQueue.submit(new WriteTask() {
			@Override
			public void task() {
				database.insert(TABLE_NAME, null, values);
			}
		});
	}
	
	public List<EntryLog> selectNotUploaded() {
		synchronized(LOCK) {
			Cursor c = database.query(
					TABLE_NAME, null, "uploaded = ? AND upload_failed = ?", 
							new String[]{ "0", "0" }, null, null, null); 
			c.moveToFirst();
			ArrayList<EntryLog> entries = new ArrayList<>();
		    while (!c.isAfterLast()) {
		      entries.add(cursorToEntry(c));
		      c.moveToNext();
		    }
		    c.close();
			return entries;	
		}
	}
	
	public void markAsUploaded(final EntryLog entry) {
		final ContentValues values = new ContentValues();
		values.put("uploaded", 1);
		writeQueue.submit(new WriteTask() {
			@Override
			public void task() {
				database.update(TABLE_NAME, values, "id = ?", new String[]{ Long.toString(entry.id) });
			}
		});
	}
	
	public void markAsUploadFailed(final EntryLog entry) {
		final ContentValues values = new ContentValues();
		values.put("upload_failed", 1);
		writeQueue.submit(new WriteTask() {
			@Override
			public void task() {
				database.update(TABLE_NAME, values, "id = ?", new String[]{ Long.toString(entry.id) });
			}
		});
	}
	
	public void update(final EntryLog entry) {
		final ContentValues values = new ContentValues();
		values.put("timestamp", entry.timestamp.getTime());
		values.put("endpoint", entry.endpoint);			
		values.put("data", entry.data);
		values.put("uploaded", entry.uploaded ? 1 : 0);
		values.put("upload_retries", entry.uploadRetries);
		values.put("upload_failed", entry.uploadFailed ? 1 : 0);
		values.put("is_update", entry.isUpdate ? 1 : 0);
		writeQueue.submit(new WriteTask() {
			@Override
			public void task() {
				database.update(TABLE_NAME, values, "id = ?", new String[]{ Long.toString(entry.id) });	
			}
		});
	}
	
	private EntryLog cursorToEntry(Cursor c) {
		EntryLog e = new EntryLog();
		e.id = c.getLong(0);
		e.timestamp = new Date(c.getLong(1));
		e.endpoint = c.getString(2);
		e.data = c.getString(3);
		e.uploaded = c.getLong(4) != 0L;
		e.uploadRetries = c.getInt(5);
		e.uploadFailed = c.getLong(6) != 0L;
		e.isUpdate = c.getLong(7) != 0L;
		return e;
	}
	
}
