package uk.ac.horizon.runspotrun.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

abstract class DAO {

	protected abstract class WriteTask
	implements Runnable {
		@Override
		public final void run() {
			synchronized(LOCK) {
				task();
			}
		}
		public abstract void task();
	}
	
	protected final Object LOCK = new Object();
	
	protected final ExecutorService writeQueue;

	protected final DatabaseHelper helper;
	
	protected final SQLiteDatabase database;

	public DAO(Context context) {
		helper = new DatabaseHelper(context);
		database = helper.getWritableDatabase();
		writeQueue = Executors.newFixedThreadPool(1);
	}
	
	@Override
	protected void finalize() {
		helper.close();
		writeQueue.shutdown();
	}

}