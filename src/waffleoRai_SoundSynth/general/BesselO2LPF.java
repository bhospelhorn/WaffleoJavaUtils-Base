package waffleoRai_SoundSynth.general;

import waffleoRai_SoundSynth.AudioSampleStream;
import waffleoRai_SoundSynth.BufferedFilter;
import waffleoRai_SoundSynth.Filter;

public class BesselO2LPF extends BufferedFilter{

	/*----- Constants -----*/
	
	public double[] POLY_COEFFS_A1 = {-20.648, 15.493, -10.564, 1.9952};
	public double[] POLY_COEFFS_A2 = {-89.211, 89.186, -42.535, 10.122, -0.9913};
	public double[] POLY_COEFFS_B = {22.675, -17.569, 6.9309, 0.0854, 0};

	/*----- InstanceVariables -----*/
	
	private AudioSampleStream input;
	
	private float cutoff_freq;
	
	private double a1_coeff;
	private double a2_coeff;
	private double gain_coeff;
	
	private double[] y1;
	private double[] y2;
	private double[] x1;
	private double[] x2;
	
	private TransformCallback tfunc;
	
	/*----- Construction -----*/
	
	public BesselO2LPF(AudioSampleStream in, float cutoff, int mode, int buffer_size)
	{
		super(buffer_size, in.getChannelCount());
		
		input = in;
		cutoff_freq = cutoff;
		int channels = in.getChannelCount();
		y1 = new double[channels];
		x1 = new double[channels];
		y2 = new double[channels];
		x2 = new double[channels];
		
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
		double quar = cubed * freqProp;
		
		gain_coeff = quar * POLY_COEFFS_B[0];
		gain_coeff += cubed * POLY_COEFFS_B[1];
		gain_coeff += squared * POLY_COEFFS_B[2];
		gain_coeff += freqProp * POLY_COEFFS_B[3];
		
		a2_coeff = quar * POLY_COEFFS_A2[0];
		a2_coeff += cubed * POLY_COEFFS_A2[1];
		a2_coeff += squared * POLY_COEFFS_A2[2];
		a2_coeff += freqProp * POLY_COEFFS_A2[3];
		a2_coeff += POLY_COEFFS_A2[4];
		
		a1_coeff += cubed * POLY_COEFFS_A1[1];
		a1_coeff += squared * POLY_COEFFS_A1[2];
		a1_coeff += freqProp * POLY_COEFFS_A1[3];
		a1_coeff += POLY_COEFFS_A1[4];
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
				double x_0 = (double)in[i];
				double x_1 = x1[i];
				double y_1 = y1[i];
				double x_2 = x2[i];
				double y_2 = y2[i];
				
				x1[i] = x_0;
				x2[i] = x_1;
				y2[i] = y_1;
				
				double val = x_0 * gain_coeff;
				val += x_1 * 2.0 * gain_coeff;
				val += x_2 * gain_coeff;
				val -= y_1 * a1_coeff;
				val -= y_2 * a2_coeff;
				y1[i] = val;
				out[i] = (int)Math.round(val);
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
				double x_0 = (double)in[i];
				double w_1 = x1[i];
				double w_2 = x2[i];
				double w_0 = x_0;
				w_0 -= (w_1) * a1_coeff;
				w_0 -= (w_2) * a2_coeff;
				
				x2[i] = w_1;
				x1[i] = w_0;
				
				double val = w_0 * gain_coeff;
				val += w_1 * 2.0 * gain_coeff;
				val += w_2 * gain_coeff;
				
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
	
	public boolean done(){
		return input.done();
	}

	
}
