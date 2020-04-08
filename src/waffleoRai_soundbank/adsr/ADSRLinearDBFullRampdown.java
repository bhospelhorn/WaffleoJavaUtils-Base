package waffleoRai_soundbank.adsr;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ADSRLinearDBFullRampdown implements EnvelopeStreamer{
	
	private static ConcurrentMap<Integer, Double> kmap;
	private static final double THRESHOLD = 0.00001;
	
	private double start;
	private double end;
	
	private double current_lvl;
	private double next_lvl;
	
	private double k; //step factor
	
	public ADSRLinearDBFullRampdown(int samples){
		this(1.0, 0.0, samples);
	}
	
	public ADSRLinearDBFullRampdown(double init, double fin, int samples){
		if(kmap == null) kmap = new ConcurrentHashMap<Integer, Double>();
		start = init;
		
		Double get = kmap.get(samples);
		if(get != null) {
			//System.err.println("Entry for " + samples + ": " + get);
			k = get;
		}
		else{
			double rate = -100.0/(double)samples;
			//System.err.println("samples = " + samples);
			//System.err.println("rate = " + rate);
			rate /= 20.0;
			double pow = Math.pow(10.0, rate);
			k = pow / (1.0 - pow);
			//System.err.println("k = " + k);
			kmap.put(samples, k);
		}
		
		current_lvl = start;
		next_lvl = start;
		end = fin;
		if(end < THRESHOLD) end = THRESHOLD;
	}

	public double getNextAmpRatio() {
		
		if(done()) return 0.0;
		
		current_lvl = next_lvl;
		
		double stepdown = current_lvl/k;
		next_lvl = current_lvl - stepdown;
		
		return current_lvl;
	}

	public boolean done() {
		return current_lvl <= end;
	}

}
