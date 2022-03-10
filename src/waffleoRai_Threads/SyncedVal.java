package waffleoRai_Threads;

public class SyncedVal<T> {

	private volatile T value;
	
	public SyncedVal(T initval){value = initval;}
	public T get(){return value;}
	
	public synchronized void set(T newval){value = newval;}
	
}
