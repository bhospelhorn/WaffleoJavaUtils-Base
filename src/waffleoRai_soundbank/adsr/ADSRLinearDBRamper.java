package waffleoRai_soundbank.adsr;

import waffleoRai_Sound.DBScaling;

public class ADSRLinearDBRamper implements EnvelopeStreamer{

	//private boolean direction; //True is up
	private double start;
	private double end;
	private double enddb;
	
	private double slope;
	
	private double current_db; //Relative to start
	private double next_db;
	
	private int samples;
	
	public ADSRLinearDBRamper(boolean up, double init, double fin, int time_samples){

		//direction = up;
		start = init;
		end = fin;
		samples = time_samples;
		//System.err.println("samples = " + time_samples);
		
		current_db = 0.0;
		if(fin == 0.0){
			//-100 dB is bottom
			//See dB of init vs. top
			double initdb = 20.0 * Math.log10(init);
			slope = (-100.0 - initdb) / (double)samples;
			enddb = -100.0;
		}
		else if(init == 0.0){
			double findb = 20.0 * Math.log10(init);
			double dbdiff = findb + 100.0;
			slope = dbdiff / (double)samples;
			enddb = dbdiff;
		}
		else{enddb = (20.0 * Math.log10(fin/init)); slope = enddb/(double)samples;}
		//System.err.println("enddb = " + enddb);
		//System.err.println("slope = " + slope);
	}
	
	@Override
	public double getNextAmpRatio() {
		//if(done()) return end;
		
		current_db = next_db;
		next_db += slope;
		
		//double ratio = Math.pow(10.0, current_db/20.0);
		//System.err.println("currentdb = " + current_db);
		double ratio = DBScaling.quick_dB_2_ampratio(current_db);
		//if(ratio == 0.0) System.err.println("Zero!");
		
		return ratio * start;
	}

	@Override
	public boolean done() {
		if(end > start) return current_db >= enddb;
		else {
			//if(current_db <= enddb) System.err.println("Done!");
			return current_db <= enddb;
		}
	}
	
}
