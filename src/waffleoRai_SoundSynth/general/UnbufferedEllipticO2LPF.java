package waffleoRai_SoundSynth.general;

import waffleoRai_SoundSynth.AudioSampleStream;
import waffleoRai_SoundSynth.Filter;

public class UnbufferedEllipticO2LPF implements Filter{
	
	/*----- Constants -----*/
	
	public double[] POLY_COEFFS_A1 = {-34.047, 37.524, -7.7791, -9.2793, 2};
	public double[] POLY_COEFFS_A2 = {-41.313, 53.032, -32.066, 7.9221, -0.9955};
	public double[] POLY_COEFFS_B0 = {16.347, -20.172, 9.9608, -0.4891, 0.2101};
	public double[] POLY_COEFFS_B1 = {28.141, -33.744, 13.478, 2.9075, -0.4505};
	//public double[] POLY_COEFFS_NORM = {25.021, -3.1519, 0.5044, 0.0614, 1};
	
	/*----- InstanceVariables -----*/
	
	private AudioSampleStream input;
	
	private float cutoff_freq;
	
	private double a1_coeff;
	private double a2_coeff;
	private double b0_coeff;
	private double b1_coeff;
	
	private double[] y1;
	private double[] y2;
	private double[] x1;
	private double[] x2;
	
	private TransformCallback tfunc;
	
	public UnbufferedEllipticO2LPF(AudioSampleStream in, float cutoff, int mode){
		input = in;
		cutoff_freq = cutoff;
		int channels = in.getChannelCount();
		y1 = new double[channels];
		x1 = new double[channels];
		y2 = new double[channels];
		x2 = new double[channels];
		
		//Determine coefficients
		//System.err.println("Cutoff = " + cutoff_freq);
		float samplerate = in.getSampleRate();
		//float nyquist = samplerate/2;
		double prop = (double)cutoff/(double)samplerate;
		calculateCoefficients(prop);
		
		//Set callback function
		switch(mode)
		{
		case Filter.LPF_MODE_BIQUAD_DIRECT_1: tfunc = new Biquad1Callback(); break;
		case Filter.LPF_MODE_BIQUAD_DIRECT_2: tfunc = new Biquad2Callback(); break;
		default: tfunc = new Biquad2Callback(); break;
		}
	}
	
	private void calculateCoefficients(double freqProp)
	{
		double squared = freqProp * freqProp;
		double cubed = squared * freqProp;
		double quar = cubed * freqProp;

		a1_coeff = quar * POLY_COEFFS_A1[0];
		a1_coeff += cubed * POLY_COEFFS_A1[1];
		a1_coeff += squared * POLY_COEFFS_A1[2];
		a1_coeff += freqProp * POLY_COEFFS_A1[3];
		a1_coeff += POLY_COEFFS_A1[4];
		
		a2_coeff = quar * POLY_COEFFS_A2[0];
		a2_coeff += cubed * POLY_COEFFS_A2[1];
		a2_coeff += squared * POLY_COEFFS_A2[2];
		a2_coeff += freqProp * POLY_COEFFS_A2[3];
		a2_coeff += POLY_COEFFS_A2[4];
		
		b0_coeff = quar * POLY_COEFFS_B0[0];
		b0_coeff += cubed * POLY_COEFFS_B0[1];
		b0_coeff += squared * POLY_COEFFS_B0[2];
		b0_coeff += freqProp * POLY_COEFFS_B0[3];
		b0_coeff += POLY_COEFFS_B0[4];
		
		b1_coeff = quar * POLY_COEFFS_B1[0];
		b1_coeff += cubed * POLY_COEFFS_B1[1];
		b1_coeff += squared * POLY_COEFFS_B1[2];
		b1_coeff += freqProp * POLY_COEFFS_B1[3];
		b1_coeff += POLY_COEFFS_B1[4];
		
		//System.err.println("FreqRatio = " + freqProp);
		//System.err.println("B0 = " + b0_coeff);
		//System.err.println("B1 = " + b1_coeff);
		//System.err.println("A1 = " + a1_coeff);
		//System.err.println("A2 = " + a2_coeff);
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
				double X2 = x2[i]; double X1 = x1[i]; double X0 = (double)in[i]; 
				double Y1 = y1[i]; double Y2 = y2[i];
				
				double val = X0 * b0_coeff;
				val += X1 * b1_coeff;
				val += X2 * b0_coeff;
				val += Y1 * a1_coeff;
				val += Y2 * a2_coeff;
				
				//Save
				x2[i] = X1; x1[i] = X0;
				y2[i] = Y1; y1[i] = val;
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
				
				double val = w_0 * b0_coeff;
				val += w_1 * b1_coeff;
				val += w_2 * b0_coeff;
				
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
	
	public void setInput(AudioSampleStream input) 
	{
		reset();
		this.input = input;
		int channels = input.getChannelCount();
		y1 = new double[channels];
		x1 = new double[channels];
		
		//Determine coefficients
		float samplerate = input.getSampleRate();
		//float nyquist = samplerate/2;
		double prop = (double)cutoff_freq/(double)samplerate;
		calculateCoefficients(prop);
	}
	
	/*----- Stream -----*/
	
	public void reset()
	{
		int ccount = input.getChannelCount();
		x1 = new double[ccount];
		y1 = new double[ccount];
		
	}
	
	public boolean done(){
		return input.done();
	}
	
	public int[] nextSample() throws InterruptedException {
		int[] in = input.nextSample();
		int[] out = tfunc.transformSamples(in);

		return out;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}


}
