package uk.ac.horizon.runspotrun.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

class DAOEntryVideo 
extends DAO {
	
	public static final String TABLE_NAME = "video";
	
	public DAOEntryVideo(Context context) {
		super(context);
	}

	public void insert(EntryVideo entry) {
		final ContentValues values = new ContentValues();
		values.put("starttime", entry.startTime.getTime());
		values.put("endtime", entry.endTime.getTime());
		values.put("filename", entry.filename);	
		values.put("can_upload", entry.canUpload ? 1 : 0);
		values.put("uploaded", entry.uploaded ? 1 : 0);
		values.put("percent_uploaded", entry.percentUploaded);
		writeQueue.submit(new WriteTask() {
			@Override
			public void task() {
				database.insert(TABLE_NAME, null, values);
			}
		});
	}
	
	public void update(EntryVideo entry) {
		final ContentValues values = new ContentValues();
		final long id = entry.id;
		values.put("id", entry.id);
		values.put("starttime", entry.startTime.getTime());
		values.put("endtime", entry.endTime.getTime());
		values.put("filename", entry.filename);
		values.put("can_upload", entry.canUpload ? 1 : 0);
		values.put("uploaded", entry.uploaded ? 1 : 0);
		values.put("percent_uploaded", entry.percentUploaded);
		writeQueue.submit(new WriteTask() {
			@Override
			public void task() {
				database.update("video", values, "id = ?", new String[]{ Long.toString(id) });
			}
		});
	}	
	
	public List<EntryVideo> fetchAll() {
		synchronized(LOCK) {
			Cursor c = database.query(TABLE_NAME, null, null, null, null, null, null);
			c.moveToFirst();
			ArrayList<EntryVideo> entries = new ArrayList<>();
			while (!c.isAfterLast()) {
				entries.add(cursorToEntry(c));
				c.moveToNext();
			}
			c.close();
			return entries;
		}
	}
	
	public EntryVideo fetchSingleEntryToUpload(boolean manual) {
		synchronized(LOCK) {
			String query = manual ? "uploaded = ? AND can_upload = ?" : "uploaded = ?";
			String[] parameters = manual ? new String[]{ "0", "1" } : new String[]{ "0" };
			Cursor c = database.query(
					TABLE_NAME, null, query, parameters, null, null, null, "1");
			c.moveToFirst();
			EntryVideo entry = null;
			if (!c.isAfterLast())
				entry = cursorToEntry(c);
			c.close();
			return entry;
		}
	}
	
	public void markAsUploaded(EntryVideo entry) {
		final ContentValues values = new ContentValues();
		final long id = entry.id;
		values.put("uploaded", 1);
		writeQueue.submit(new WriteTask() {
			@Override
			public void task() {
				database.update(TABLE_NAME, values, "id = ?", new String[]{ Long.toString(id) });
			}
		});
		
	}
	
	public void markAsCanUpload(EntryVideo entry) {
		final ContentValues values = new ContentValues();
		final long id = entry.id;
		values.put("can_upload", 1);
		writeQueue.submit(new WriteTask() {
			@Override
			public void task() {
				database.update(TABLE_NAME, values, "id = ?", new String[]{ Long.toString(id) });
			}
		});
	}
	
	public void delete(EntryVideo entry) {
		final long id = entry.id;
		writeQueue.submit(new WriteTask() {
			@Override
			public void task() {
				database.delete(TABLE_NAME, "id = ?", new String[]{ Long.toString(id) });
			}
		});
	}
	
	@Override
	protected void finalize() {
		helper.close();
		writeQueue.shutdown();
	}
	
	private EntryVideo cursorToEntry(Cursor c) {
		EntryVideo e = new EntryVideo();
		e.id = c.getLong(0);
		e.startTime = new Date(c.getLong(1));
		e.endTime = new Date(c.getLong(2));
		e.filename = c.getString(3);
		e.canUpload = c.getInt(4) == 1;
		e.uploaded = c.getInt(5) == 1;
		e.percentUploaded = c.getInt(6);
		return e;
	}

}
