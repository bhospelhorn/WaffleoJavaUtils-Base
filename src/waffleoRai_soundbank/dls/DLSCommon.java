package waffleoRai_soundbank.dls;

public class DLSCommon {
	
	public static final double CAP_16 = (double)0x10000;
	public static final double GAIN_SCALER = 200.0 * CAP_16;
	public static final double TIME_SCALER = 1200.0 * CAP_16;

	public static double dlsGainToVolRatio(int gain){
		//Per DLS doc: gain = 200 * log10(ratio) * 65536
		//Therefore ratio = 10 ^ (gain / scaler)
		return Math.pow(10.0, (gain / GAIN_SCALER));
	}
	
	public static int dlsPanToStdPan(int dlsPan){
		//Apparently in 0.1% units
		double ratio = (double)dlsPan / 1000.0;
		int snap = (int)Math.round(ratio * 63.0);
		return 0x40 + snap;
	}
	
	public static int absTimeToMillis(int time){
		//Abs time = 1200 * log2(seconds) * 65536
		if(time == 0x80000000) return 0; //Reserved value for 0.
		double tt = Math.pow(2.0, (time / TIME_SCALER));
		tt *= 1000.0;
		return (int)Math.round(tt);
	}
	
}
