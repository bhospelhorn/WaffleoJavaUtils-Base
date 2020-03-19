package waffleoRai_soundbank.adsr;

public class ADSRLinearAmpRamper implements EnvelopeStreamer{

	private boolean direction; //True is up
	private double start;
	private double end;
	
	private double slope;
	
	private double current;
	private double next;
	
	private int samples;
	
	public ADSRLinearAmpRamper(boolean up, double init, double fin, int time_samples){

		direction = up;
		start = init;
		end = fin;
		samples = time_samples;
		
		current = init;
		next = init;
		slope = Math.abs(end-start)/samples;
	}
	
	@Override
	public double getNextAmpRatio() {
		current = next;
		if(direction) next += slope;
		else next -= slope;
		
		return current;
	}

	@Override
	public boolean done() {
		if(direction) return (current >= end);
		return (current <= end);
	}

}
