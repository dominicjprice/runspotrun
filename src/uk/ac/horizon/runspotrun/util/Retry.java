package uk.ac.horizon.runspotrun.util;

public class Retry<T, E extends Throwable> {
	
	public static interface Action<T, E extends Throwable> {
		public T doAction() throws E;
	}
	
	public T exponentialBackoff(
			final Action<T, E> action, 
			final long initialPeriod,
			final int maxTries)
	throws E {
		try {
			return action.doAction();
		}
		catch(Throwable e) {
			if(maxTries == 0)
				throw e;
			try {
				Thread.sleep(initialPeriod);
			} catch(InterruptedException ie) { }
			return exponentialBackoff(action, initialPeriod * 2, maxTries - 1);
		}
	}
	
}
