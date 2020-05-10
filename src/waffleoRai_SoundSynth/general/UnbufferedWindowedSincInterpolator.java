package waffleoRai_SoundSynth.general;

import waffleoRai_SoundSynth.AudioSampleStream;
import waffleoRai_SoundSynth.Filter;
import waffleoRai_SoundSynth.FunctionWindow;
import waffleoRai_SoundSynth.SynthMath;
import waffleoRai_SoundSynth.general.WindowedSincInterpolator.SampleWindow;

public class UnbufferedWindowedSincInterpolator implements Filter{

	/*----- Constants -----*/
	
	/*----- InstanceVariables -----*/
	
	//private boolean use_target_samplerate;
	
	private AudioSampleStream input;
	
	private int window_size; //# samples on each side (=(N-1)/2)
	
	private double input_samplerate;
	private double target_samplerate;
	private double output_samplerate;

	private double sr_ratio; //outputSR/inputSR
	private double inv_ratio; //Inverted
	private int j_counter;
	private int k_counter;
	private SampleWindow[] swindows;
	
	private FunctionWindow window;
	
	private UnbufferedEllipticO2LPF lpf;
	
	/*----- Construction -----*/
	
	public UnbufferedWindowedSincInterpolator(AudioSampleStream in, int samplesPerSide) throws InterruptedException
	{
		this(in, samplesPerSide, new BlackmanWindow((samplesPerSide << 1) + 1), 0);
	}
	
	public UnbufferedWindowedSincInterpolator(AudioSampleStream in, int samplesPerSide, int initCents) throws InterruptedException
	{
		this(in, samplesPerSide, new BlackmanWindow((samplesPerSide << 1) + 1), initCents);
	}
	
	public UnbufferedWindowedSincInterpolator(AudioSampleStream in, int samplesPerSide, FunctionWindow win, int initCents) throws InterruptedException
	{
		input = in;
		window = win;
		window_size = samplesPerSide;
		
		input_samplerate = in.getSampleRate();
		output_samplerate = input_samplerate;
		sr_ratio = SynthMath.cents2FreqRatio(initCents);
		target_samplerate = (sr_ratio) * input_samplerate;
		sr_ratio *= (output_samplerate/input_samplerate);
		inv_ratio = 1.0/sr_ratio;
		//adjustLPF();
		
		j_counter = 0;
		k_counter = 0;
		
		int ccount = in.getChannelCount();
		swindows = new SampleWindow[ccount];
		for(int i = 0; i < ccount; i++) swindows[i] = new SampleWindow(window_size);
		
		
		//Slide window to frame first sample as current sample
		for(int i = 0; i < window_size; i++) slideWindows();

		//System.err.println("Input sample rate: " + input_samplerate);
	}
	
	/*----- Getters -----*/
	
	public float getSampleRate() {
		//if(use_target_samplerate) return (float)target_samplerate;
		//return (float) input_samplerate;
		return (float)output_samplerate;
	}

	public int getBitDepth() {
		return input.getBitDepth();
	}

	public int getChannelCount() {
		return input.getChannelCount();
	}
		
	/*----- Setters -----*/
	
	protected void adjustLPF(){
		//TODO
		//Am I being downsampled or upsampled?
		//If upsampling, need output LPF
		if(sr_ratio > 1.0){
			if(target_samplerate > input_samplerate){
				double inv_eff = input_samplerate/sr_ratio;
				//System.err.println("SR Ratio: " + sr_ratio);
				//System.err.println("Inverted Effective SR: " + inv_eff);
				lpf = new UnbufferedEllipticO2LPF(new InnerStream(), (float)(inv_eff/2.0), Filter.LPF_MODE_BIQUAD_DIRECT_1);	
			}
			else{
				//TODO
				//System.err.println("Target SR: " + target_samplerate);
				lpf = new UnbufferedEllipticO2LPF(new InnerStream(), (float)(target_samplerate/4.0), Filter.LPF_MODE_BIQUAD_DIRECT_1);	
			}
		}
		else if(sr_ratio < 1.0){
			//Downsampling
			//System.err.println("Downsample");
			lpf = null;
		}
		else lpf = null;
	}
	
	private void adjustSRRatio(){
		sr_ratio = (target_samplerate/input_samplerate) * (output_samplerate/input_samplerate);
		inv_ratio = 1.0/sr_ratio;
		//adjustLPF();
	}
	
	public void setInput(AudioSampleStream input) 
	{
		this.input = input;

		input_samplerate = (double)input.getSampleRate();
		target_samplerate = (sr_ratio) * input_samplerate;
	
		reset();
	}
	
	public void setPitchShift(int cents)
	{
		//System.err.println("Cents: " + cents);
		sr_ratio = SynthMath.cents2FreqRatio(cents);
		target_samplerate = sr_ratio * input_samplerate;
		//System.err.println("Target SR: " + target_samplerate + " | Input SR: " + input_samplerate);
		sr_ratio *= (output_samplerate/input_samplerate);
		inv_ratio = 1.0/sr_ratio;
		//adjustLPF();
		
		//window.flushSavedValues();
		j_counter = 0;
		k_counter = 0;
		
		//System.err.println("Input Sample Rate: " + input_samplerate + " hz");
		//System.err.println("Scale Sample Rate: " + target_samplerate + " hz");
		//System.err.println("Sample Rate Ratio: " + sr_ratio);
	}

	public void setOutputSampleRate(float sr)
	{
		output_samplerate = sr;
		adjustSRRatio();
		
		//window.flushSavedValues();
		j_counter = 0;
		k_counter = 0;
	}
	
	/*----- Internal -----*/
	
	private class InnerStream implements AudioSampleStream{

		public float getSampleRate() {return (float)output_samplerate;}
		public int getBitDepth() {return input.getBitDepth();}
		public int getChannelCount() {return input.getChannelCount();}

		public int[] nextSample() throws InterruptedException {
			return nextInternalSample();
		}

		public void close() {
			reset();
		}

		@Override
		public boolean done() {
			if(!input.done()) return false;
			for(SampleWindow sw : swindows) if(!sw.zeroed()) return false;
			return true;
		}
		
	}
	
	/*----- Filter -----*/
	
	private void slideWindows() throws InterruptedException
	{
		int[] insamps = input.nextSample();
		
		for(int i = 0; i < insamps.length; i++) swindows[i].slide(insamps[i]);
	}
	
	/*----- Stream -----*/
	
	public int[] nextSample() throws InterruptedException {
		if(lpf != null) return lpf.nextSample();
		else return nextInternalSample();
	}
	
	public int[] nextInternalSample() throws InterruptedException
	{
		//System.err.println("uh... hey?");
		//double J = (double)j_counter * (input_samplerate/target_samplerate);
		double J = (double)j_counter * inv_ratio;
		double K = Math.round(J);
		while(K < k_counter)K++; //Make sure K is present or future sample
		
		while(k_counter < K)
		{
			//Advance window (skip samples)
			slideWindows();
			k_counter++;
		}
		
		int ccount = swindows.length;
		int[] out = new int[ccount];
		
		double diff = K - J; //K is K CENTER
		//System.err.println("K = " + K + ", J = " + J + ", k = " + k_counter + ", j = " + j_counter);
		if(diff == 0.0)
		{
			//It's on the sample.
			//Reset counters to zero (so no overflow) and return sample.
			j_counter = 1; //For next sample (0+1)
			k_counter = 0;
			for(int c = 0; c < ccount; c++)
			{
				out[c] = swindows[c].getCurrentSample();
			}
			//System.err.println("Return sample");
			return out;
		}	
		//Thru Channels
		for(int c = 0; c < ccount; c++)
		{
			double sum = 0.0;
			double shift = (double)window_size;
			//Do K
			double val = 0.0;
			
			//sinc
			if(sr_ratio < 1.0) val = SynthMath.quicksinc(sr_ratio * diff);
			else val = SynthMath.quicksinc(diff);
			//if(sr_ratio < 1.0) val = SynthMath.sinc(sr_ratio * diff);
			//else val = SynthMath.sinc(diff);
			
			val *= (double)swindows[c].getCurrentSample();
			val *= window.getMultiplier(diff + shift);
			sum += val;
			
			//Do samples before and after K
			for(int s = 0; s < window_size; s++)
			{
				//Before
				double mydiff = diff - (s+1);
				
				if(sr_ratio < 1.0) val = SynthMath.quicksinc(sr_ratio * mydiff);
				else val = SynthMath.quicksinc(mydiff);
				//if(sr_ratio < 1.0) val = SynthMath.sinc(sr_ratio * mydiff);
				//else val = SynthMath.sinc(mydiff);
				
				val *= (double)swindows[c].getPastSample(s);
				val *= window.getMultiplier(mydiff + shift);
				sum += val;
				
				//After
				mydiff = diff + (s+1);
				
				if(sr_ratio < 1.0) val = SynthMath.quicksinc(sr_ratio * mydiff);
				else val = SynthMath.quicksinc(mydiff);
				//if(sr_ratio < 1.0) val = SynthMath.sinc(sr_ratio * mydiff);
				//else val = SynthMath.sinc(mydiff);
				
				val *= (double)swindows[c].getFutureSample(s);
				val *= window.getMultiplier(mydiff + shift);
				sum += val;
			}
			
			//Multiply by gain factor if applicable
			if(sr_ratio < 1.0) sum *= sr_ratio;
			out[c] = (int)Math.round(sum);
		}
		
		j_counter++;
		
		//System.err.println("returning");
		return out;
	}
	
	public void reset()
	{
		j_counter = 0;
		k_counter = 0;
		for(SampleWindow sw : swindows) sw.flush();
		//window.flushSavedValues();
	}
	
	public void close()
	{
		reset();
	}
	
	public boolean done(){
		if(!input.done()) return false;
		for(SampleWindow sw : swindows) if(!sw.zeroed()) return false;
		return true;
	}
	
}
