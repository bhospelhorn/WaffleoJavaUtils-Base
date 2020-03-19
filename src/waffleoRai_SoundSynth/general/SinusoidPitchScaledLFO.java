package waffleoRai_SoundSynth.general;

import waffleoRai_SoundSynth.Oscillator;

public class SinusoidPitchScaledLFO implements Oscillator{
	
	private float sampleRate; //Hz
	private double frequency; //Hz (Frequency of oscillator)
	private int scale; //Amplitude in cents
	
	private double fratio;
	private int counter;
	//private int[] table;
	
	public SinusoidPitchScaledLFO(double freq, int pitchfluc_cents, float samplerate)
	{
		sampleRate = samplerate;
		frequency = freq;
		scale = pitchfluc_cents;
		//generateTable();
		fratio = frequency /(double)sampleRate;
	}
	
	public double getNextValue()
	{
		double x = (double)counter++ * fratio;
		//If x is a whole number, reset to 0
		if((x - (double)Math.round(x)) == 0) counter = 0;
		
		double tpx = 2.0 * Math.PI * x;
		
		return Math.sin(tpx) * (double)scale;
	}

}
