package waffleoRai_SoundSynth.general;

import waffleoRai_SoundSynth.AudioSampleStream;
import waffleoRai_SoundSynth.Filter;
import waffleoRai_SoundSynth.FunctionWindow;
import waffleoRai_SoundSynth.SynthMath;
import waffleoRai_SoundSynth.general.WindowedSincInterpolator.SampleWindow;

public class UnbufferedWindowedSincInterpolator implements Filter{

	/*----- Constants -----*/
	
	/*----- InstanceVariables -----*/
	
	private boolean use_target_samplerate;
	
	private AudioSampleStream input;
	
	private int window_size; //# samples on each side (=(N-1)/2)
	
	private double input_samplerate;
	private double target_samplerate;

	private double sr_ratio; //outputSR/inputSR
	private int j_counter;
	private int k_counter;
	private SampleWindow[] swindows;
	
	private FunctionWindow window;
	
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
		sr_ratio = SynthMath.cents2FreqRatio(initCents);
		target_samplerate = (sr_ratio) * input_samplerate;
		
		j_counter = 0;
		k_counter = 0;
		
		int ccount = in.getChannelCount();
		swindows = new SampleWindow[ccount];
		for(int i = 0; i < ccount; i++) swindows[i] = new SampleWindow(window_size);
		
		
		//Slide window to frame first sample as current sample
		for(int i = 0; i < window_size; i++) slideWindows();

	}
	
	/*----- Getters -----*/
	
	public float getSampleRate() 
	{
		if(use_target_samplerate) return (float)target_samplerate;
		return (float) input_samplerate;
	}

	public int getBitDepth() 
	{
		return input.getBitDepth();
	}

	public int getChannelCount() 
	{
		return input.getChannelCount();
	}
		
	/*----- Setters -----*/
	
	public void setInput(AudioSampleStream input) 
	{
		this.input = input;

		input_samplerate = (double)input.getSampleRate();
		target_samplerate = (sr_ratio) * input_samplerate;
	
		reset();
	}
	
	public void setPitchShift(int cents)
	{
		sr_ratio = SynthMath.cents2FreqRatio(cents);
		target_samplerate = sr_ratio * input_samplerate;
		
		//window.flushSavedValues();
		j_counter = 0;
		k_counter = 0;
		
		//System.err.println("Input Sample Rate: " + input_samplerate + " hz");
		//System.err.println("Scale Sample Rate: " + target_samplerate + " hz");
		//System.err.println("Sample Rate Ratio: " + sr_ratio);
	}

	public void setOutputSampleRate(float sr)
	{
		target_samplerate = sr;
		sr_ratio = target_samplerate/input_samplerate;
		
		//window.flushSavedValues();
		j_counter = 0;
		k_counter = 0;
	}
	
	public void setUseTargetSampleRate(boolean b)
	{
		this.use_target_samplerate = b;
	}
	
	/*----- Filter -----*/
	
	private void slideWindows() throws InterruptedException
	{
		int[] insamps = input.nextSample();
		
		for(int i = 0; i < insamps.length; i++) swindows[i].slide(insamps[i]);
	}
	
	/*----- Stream -----*/
	
	public int[] nextSample() throws InterruptedException
	{
		double J = (double)j_counter * (input_samplerate/target_samplerate);
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
			if(sr_ratio < 1.0) val = SynthMath.quicksinc(sr_ratio * diff);
			else val = SynthMath.quicksinc(diff);
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
				val *= (double)swindows[c].getPastSample(s);
				val *= window.getMultiplier(mydiff + shift);
				sum += val;
				
				//After
				mydiff = diff + (s+1);
				if(sr_ratio < 1.0) val = SynthMath.quicksinc(sr_ratio * mydiff);
				else val = SynthMath.quicksinc(mydiff);
				val *= (double)swindows[c].getFutureSample(s);
				val *= window.getMultiplier(mydiff + shift);
				sum += val;
			}
			
			//Multiply by gain factor if applicable
			if(sr_ratio < 1.0) sum *= sr_ratio;
			out[c] = (int)Math.round(sum);
		}
		
		j_counter++;
		
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
