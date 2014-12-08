package uk.ac.horizon.runspotrun.service;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class DatabaseHelper 
extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "runspotrun";
	
	private static final int DATABASE_VERSION = 4;
	
	private static final String CREATE_LOG_TABLE =
			"CREATE TABLE " + DAOEntryLog.TABLE_NAME + " ("
			+ "id INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ "timestamp INTEGER NOT NULL, "
			+ "endpoint TEXT NOT NULL, " 
			+ "data TEXT NOT NULL, "
			+ "uploaded INTEGER NOT NULL, "
			+ "upload_retries INTEGER NOT NULL, "
			+ "upload_failed INTEGER NOT NULL, "
			+ "is_update INTEGER NOT NULL"
			+ ");";
	
	private static final String CREATE_VIDEO_TABLE =
			"CREATE TABLE " + DAOEntryVideo.TABLE_NAME + " ("
			+ "id INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ "starttime INTEGER NOT NULL, "
			+ "endtime INTEGER NOT NULL, "
			+ "filename TEXT NOT NULL, "
			+ "can_upload INTEGER NOT NULL, "
			+ "uploaded INTEGER NOT NULL, "
			+ "percent_uploaded INTEGER NOT NULL"
			+ ");";
	
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_LOG_TABLE);
		db.execSQL(CREATE_VIDEO_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { 
		final String[][] alterStatments = {
				{
					"ALTER TABLE " + DAOEntryVideo.TABLE_NAME 
					+ " ADD percent_uploaded INTEGER NOT NULL DEFAULT 0"
				}, {
					"DROP TABLE " + DAOEntryVideo.TABLE_NAME,
					CREATE_VIDEO_TABLE
				}, {
					"ALTER TABLE " + DAOEntryLog.TABLE_NAME 
					+ " ADD is_update INTEGER NOT NULL DEFAULT 0"
				}
		};
		for(int i = oldVersion - 1; i < alterStatments.length; i++) {
			for(String statement : alterStatments[i])
				db.execSQL(statement);
		}
	}

}
