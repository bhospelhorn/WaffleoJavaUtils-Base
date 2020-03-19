package waffleoRai_SoundSynth.general;

import waffleoRai_SoundSynth.AudioSampleStream;
import waffleoRai_SoundSynth.BufferedFilter;
import waffleoRai_SoundSynth.Filter;

//Coeffs calculated from https://www.micromodeler.com/dsp/

public class BesselO1LPF extends BufferedFilter{
	
	/*----- Constants -----*/
	
	public double[] CUBIC_COEFFS_A = {-14.621, 11, -5.8269, 1};
	public double[] CUBIC_COEFFS_GAIN = {7.1346, -5.3402, 2.8752, 0.0033};

	/*----- InstanceVariables -----*/
	
	private AudioSampleStream input;
	
	private float cutoff_freq;
	
	private double a_coeff;
	private double gain_coeff;
	
	private double[] y1;
	private double[] x1;
	
	private TransformCallback tfunc;
	
	/*----- Construction -----*/
	
	public BesselO1LPF(AudioSampleStream in, float cutoff, int mode, int buffer_size)
	{
		super(buffer_size, in.getChannelCount());
		
		input = in;
		cutoff_freq = cutoff;
		int channels = in.getChannelCount();
		y1 = new double[channels];
		x1 = new double[channels];
		
		//Determine coefficients
		float samplerate = in.getSampleRate();
		float nyquist = samplerate/2;
		double prop = (double)cutoff/(double)nyquist;
		calculateCoefficients(prop);
		
		//Set callback function
		switch(mode)
		{
		case Filter.LPF_MODE_BIQUAD_DIRECT_1: tfunc = new Biquad1Callback(); break;
		case Filter.LPF_MODE_BIQUAD_DIRECT_2: tfunc = new Biquad2Callback(); break;
		default: tfunc = new Biquad2Callback(); break;
		}
		
		super.startBuffering();
	}
	
	private void calculateCoefficients(double freqProp)
	{
		double squared = freqProp * freqProp;
		double cubed = squared * freqProp;
		a_coeff = CUBIC_COEFFS_A[0] * cubed;
		a_coeff += CUBIC_COEFFS_A[1] * squared;
		a_coeff += CUBIC_COEFFS_A[2] * freqProp;
		a_coeff += CUBIC_COEFFS_A[3];
		
		gain_coeff = CUBIC_COEFFS_GAIN[0] * cubed;
		gain_coeff += CUBIC_COEFFS_GAIN[1] * squared;
		gain_coeff += CUBIC_COEFFS_GAIN[2] * freqProp;
		gain_coeff += CUBIC_COEFFS_GAIN[3];
	}
	
	/*----- Inner Classes -----*/
	
	private interface TransformCallback
	{
		public int[] transformSamples(int[] in);
	}
	
	private class Biquad1Callback implements TransformCallback
	{
		public int[] transformSamples(int[] in)
		{
			int[] out = new int[in.length];
			for(int i = 0; i < in.length; i++)
			{
				double lastx = x1[i];
				double lasty = y1[i];
				x1[i] = in[i];
				double val = (lastx * gain_coeff) - (lasty * a_coeff);
				val += (double)in[i] * gain_coeff;
				y1[i] = val;
				int samp = (int)Math.round(val);
				out[i] = samp;
			}
			
			return out;
		}
	}
	
	private class Biquad2Callback implements TransformCallback
	{
		public int[] transformSamples(int[] in)
		{
			int[] out = new int[in.length];
			for(int i = 0; i < in.length; i++)
			{
				double lastw = x1[i];
				double x0 = (double)in[i];
				double val = x0 - (lastw * a_coeff);
				x1[i] = val;
				val = (val * gain_coeff) + (lastw * gain_coeff);
				out[i] = (int)Math.round(val);
			}
			
			return out;
		}
	}
	
	/*----- Getters -----*/
	
	public float getSampleRate() {return input.getSampleRate();}
	public int getBitDepth() {return input.getBitDepth();}
	public int getChannelCount() {return x1.length;}

	/*----- Setters -----*/
	
	@Override
	public void setInput(AudioSampleStream input) 
	{
		reset();
		this.input = input;
		int channels = input.getChannelCount();
		y1 = new double[channels];
		x1 = new double[channels];
		
		//Determine coefficients
		float samplerate = input.getSampleRate();
		float nyquist = samplerate/2;
		double prop = (double)cutoff_freq/(double)nyquist;
		calculateCoefficients(prop);
	}
	
	/*----- Stream -----*/
	
	public void reset()
	{
		super.close();
		int ccount = input.getChannelCount();
		x1 = new double[ccount];
		y1 = new double[ccount];
		
		super.reset();
	}
	
	protected int[] generateNextSamples() throws InterruptedException
	{
		int[] in = input.nextSample();
		int[] out = tfunc.transformSamples(in);
		
		//int[][] both = new int[2][in.length];
		//both[0] = in;
		//both[1] = out;
		
		return out;
	}
	
}
