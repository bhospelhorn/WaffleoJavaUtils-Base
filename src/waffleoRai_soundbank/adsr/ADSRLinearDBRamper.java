package waffleoRai_soundbank.adsr;

public class ADSRLinearDBRamper implements EnvelopeStreamer{

	//private boolean direction; //True is up
	private double start;
	private double end;
	
	private double slope;
	
	private double current_db; //Relative to start
	private double next_db;
	
	private int samples;
	
	public ADSRLinearDBRamper(boolean up, double init, double fin, int time_samples){

		//direction = up;
		start = init;
		end = fin;
		samples = time_samples;
		
		current_db = 0.0;
		slope = (20.0 * Math.log10(fin/init))/samples;
	}
	
	@Override
	public double getNextAmpRatio() {
		if(done()) return end;
		
		current_db = next_db;
		next_db += slope;
		
		double ratio = Math.pow(10.0, current_db/20.0);
		
		return ratio * start;
	}

	@Override
	public boolean done() {
		if(end > start) return current_db >= end;
		else return current_db <= end;
	}
	
}
