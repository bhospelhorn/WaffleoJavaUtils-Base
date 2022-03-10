package waffleoRai_Threads;

public class SyncedInt {

	private volatile int value;
	
	public SyncedInt(int initval){value = initval;}
	public int get(){return value;}
	
	public synchronized void set(int newval){value = newval;}
	
}
