package uk.ac.horizon.runspotrun.data;

import uk.ac.horizon.runspotrun.Constants;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class Helper 
extends SQLiteOpenHelper {

	public Helper(Context context) {
		super(context, Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + LogEntryDAO.TABLE_NAME + " ("
				+ "id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "timestamp INTEGER NOT NULL, "
				+ "endpoint TEXT NOT NULL, " 
				+ "data TEXT NOT NULL, "
				+ "uploaded INTEGER NOT NULL, "
				+ "upload_retries INTEGER NOT NULL, "
				+ "upload_failed INTEGER NOT NULL"
				+ ");");
		
		db.execSQL("CREATE TABLE " + VideoEntryDAO.TABLE_NAME + " ("
				+ "id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "starttime INTEGER NOT NULL, "
				+ "endtime INTEGER NOT NULL, "
				+ "filename TEXT NOT NULL, " 
				+ "accuracy REAL NOT NULL, "
				+ "latitude REAL NOT NULL, "
				+ "longitude REAL NOT NULL, "
				+ "can_upload INTEGER NOT NULL, "
				+ "uploaded INTEGER NOT NULL"
				+ ");");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }

}
