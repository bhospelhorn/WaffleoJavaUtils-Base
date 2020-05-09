package waffleoRai_SoundSynth.general;

import waffleoRai_SoundSynth.Oscillator;

public abstract class BasicLFO implements Oscillator{
	
	private int sample_rate;
	private int lfo_rate;
	private double srratio; //lfo/sr
	
	private double amplitude;
	private int delay; //in ms
	
	private int ctr;
	private int delay_remain;
	
	public BasicLFO(int sampleRate){
		sample_rate = sampleRate;
		lfo_rate = 100;
		srratio = (double)lfo_rate/(double)sample_rate;
		
		amplitude = 1.0;
		delay = 0;
		
		reset();
	}
	
	public int getSampleRate(){return sample_rate;}
	public int getLFORate(){return lfo_rate;}
	public double getAmplitude(){return amplitude;}
	public int getDelay(){return delay;}
	
	public void setSampleRate(int sr){
		sample_rate = sr;
		srratio = (double)lfo_rate/(double)sample_rate;
		ctr = 0;
	}
	
	public void setLFORate(int rate){
		lfo_rate = rate;
		srratio = (double)lfo_rate/(double)sample_rate;
		ctr = 0;
	}
	
	public void setAmplitude(double a){
		amplitude = a;
	}
	
	public void setDelay(int delay_ms){
		double s = (double)delay_ms /1000.0;
		delay = (int)Math.round((double)sample_rate * s);
	}
	
	public void reset(){
		ctr = 0;
		delay_remain = delay;
	}
	
	private double getCoord(){
		double c = srratio * (double)ctr++;
		if(((double)(int)c) == c) ctr = 0; //Is an int
		return srratio * (double)ctr;
	}

	public double getNextValue() {
		
		if(delay_remain > 0){
			delay_remain--;
			return 0.0;
		}
		
		double y = getY(getCoord());
		y *= amplitude;
		
		return y;
	}

	protected abstract double getY(double x);
	
}
