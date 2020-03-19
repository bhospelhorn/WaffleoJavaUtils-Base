package waffleoRai_Utils;

public class TimeUtils {
	
	private static long time;
	
	public static void tick()
	{
		time = System.nanoTime();
	}
	
	public static void tock()
	{
		long ntime = System.nanoTime();
		long elapsed = ntime - time;
		double micros = ((double)elapsed)/ 1000.0;
		System.err.println("Time elapsed: " + micros + " us");
	}

}
