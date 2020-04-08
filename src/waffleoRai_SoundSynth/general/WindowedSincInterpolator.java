package waffleoRai_SoundSynth.general;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import waffleoRai_SoundSynth.AudioSampleStream;
import waffleoRai_SoundSynth.BufferedFilter;
import waffleoRai_SoundSynth.FunctionWindow;
import waffleoRai_SoundSynth.SynthMath;

public class WindowedSincInterpolator extends BufferedFilter{
	
	/*----- Constants -----*/
	
	public static final int BUFFER_SECONDS = 2; //2 second buffer
	
	/*----- InstanceVariables -----*/
	
	private AudioSampleStream input;
	
	private int window_size; //# samples on each side (=(N-1)/2)
	
	private boolean use_target_samplerate;
	private double input_samplerate;
	private double target_samplerate;

	private double sr_ratio; //outputSR/inputSR
	private int j_counter;
	private int k_counter;
	private SampleWindow[] swindows;
	
	private FunctionWindow window;
	
	private StateBuffer[] inbuffer; //For input samples in case of rewind - keeps (window side) samples before last sample read
	private StateBuffer[] altin; //For input samples in case of rewind
	
	/*----- Construction -----*/
	
	public WindowedSincInterpolator(AudioSampleStream in, int samplesPerSide) throws InterruptedException
	{
		this(in, samplesPerSide, new BlackmanWindow((samplesPerSide << 1) + 1), 0, false);
	}
	
	public WindowedSincInterpolator(AudioSampleStream in, int samplesPerSide, FunctionWindow win, int initCents, boolean backbuffer) throws InterruptedException
	{
		super((int)(in.getSampleRate() * 2), in.getChannelCount());
		input = in;
		window = win;
		window_size = samplesPerSide;
		
		input_samplerate = in.getSampleRate();
		sr_ratio = SynthMath.cents2FreqRatio(initCents);
		target_samplerate = (sr_ratio) * input_samplerate;
		
		j_counter = 0;
		k_counter = 0;
		
		int ccount = in.getChannelCount();
		for(int i = 0; i < ccount; i++) swindows[i] = new SampleWindow(window_size);
		
		if(backbuffer)
		{
			//int buffcap = (int)(input_samplerate) << 1;
			inbuffer = new StateBuffer[ccount];
			for(int i = 0; i < ccount; i++) inbuffer[i] = new StateBuffer();
		}
		
		//Slide window to frame first sample as current sample
		for(int i = 0; i < window_size; i++) slideWindows();

		super.startBuffering();
	}
	
	/*----- Inner Classes -----*/
	
	public static class StateBuffer
	{
		private Queue<SampleState> queue;
		
		public StateBuffer()
		{
			queue = new ConcurrentLinkedQueue<SampleState>();
		}
		
		public SampleState pop()
		{
			return queue.poll();
		}
		
		public boolean put(SampleState state)
		{
			return queue.add(state);
		}
		
	}
	
	public static class SampleState
	{
		public int sampleValue;
		//public int j;
		//public int k;
		
		public SampleState(int val){sampleValue = val;}
		//public SampleState(int val, int j, int k){sampleValue = val; this.j = j; this.k = k;}
	}
	
	public static class SampleWindow
	{
		private int[] past;
		private int[] future;
		private int current;
		
		public SampleWindow(int sampsPerSide)
		{
			past = new int[sampsPerSide];
			future = new int[sampsPerSide];
		}
		
		public void flush()
		{
			current = 0;
			for(int i = 0; i < past.length; i++) {past[i] = 0; future[i] = 0;}
		}
		
		public int getCurrentSample(){return current;}
		public int getPastSample(int i){return past[i];}
		public int getFutureSample(int i){return future[i];}
		
		public void slide(int newSample)
		{
			for (int i = past.length-1; i > 0; i--) past[i] = past[i-1];
			past[0] = current;
			current = future[0];
			for(int i = 0; i < past.length-1; i++) future[i] = future[i+1];
			future[future.length-1] = newSample;
		}
		
		public boolean zeroed(){
			if(current != 0) return false;
			for(int i = 0; i < past.length; i++){
				if(past[i] != 0) return false;
			}
			for(int i = 0; i < future.length; i++){
				if(future[i] != 0) return false;
			}
			return true;
		}
	}
	
	/*----- Mathy -----*/
	
	

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
		super.close();
		this.input = input;
		//window.flushSavedValues();
		
		input_samplerate = (double)input.getSampleRate();
		target_samplerate = (sr_ratio) * input_samplerate;
		
		j_counter = 0;
		k_counter = 0;
		
		for(SampleWindow sw : swindows) sw.flush();
		if(inbuffer != null)
		{
			int ccount = input.getChannelCount();
			//int buffcap = (int)(input_samplerate) << 1;
			inbuffer = new StateBuffer[ccount];
			for(int i = 0; i < ccount; i++) inbuffer[i] = new StateBuffer();
		}
		super.reset();
	}
	
	public void setPitchShift(int cents)
	{
		resetBuffer(false);
		
		sr_ratio = SynthMath.cents2FreqRatio(cents);
		target_samplerate = sr_ratio * input_samplerate;
		//window.flushSavedValues();
		
		super.setBufferHold(false);
	}

	/*----- Filter -----*/
	
	private void resetBuffer(boolean releasehold)
	{
		super.setBufferHold(true);
		super.flushBuffer(); //Emptying should block threads trying to get new samples
		
		altin = inbuffer;
		inbuffer = new StateBuffer[altin.length];
		for(int i = 0; i < inbuffer.length; i++) inbuffer[i] = new StateBuffer();
		
		//Slide window to current sample...
		SampleState state = null;
		for(int i = 0; i < window_size; i++)
		{
			for(int j = 0; j < altin.length; j++){
				state = altin[j].pop();
				swindows[j].slide(state.sampleValue);
			}
		}
		
		//Reset j and k...
		//j_counter = state.j;
		//k_counter = state.k;
		j_counter = 0;
		k_counter = 0;

		if(releasehold) super.setBufferHold(false);
	}
	
	private void slideWindows() throws InterruptedException
	{
		int[] insamps = null;
		if(altin != null)
		{
			insamps = new int[altin.length];
			for(int i = 0; i < altin.length; i++) insamps[i] = altin[i].pop().sampleValue;
		}
		else insamps = input.nextSample();
		
		//Save sample
		if(inbuffer != null)
		{
			for(int i = 0; i < inbuffer.length; i++) inbuffer[i].put(new SampleState(insamps[i]));
		}
		
		for(int i = 0; i < insamps.length; i++) swindows[i].slide(insamps[i]);
	}
	
	protected int[] generateNextSamples() throws InterruptedException 
	{
		//Determine how many samples to skip...
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
			return out;
		}	
		//Thru Channels
		for(int c = 0; c < ccount; c++)
		{
			double sum = 0.0;
			double shift = (double)window_size;
			//Do K
			double val = 0.0;
			if(sr_ratio < 1.0) val = SynthMath.sinc(sr_ratio * diff);
			else val = SynthMath.sinc(diff);
			val *= (double)swindows[c].getCurrentSample();
			val *= window.getMultiplier(diff + shift);
			sum += val;
			
			//Do samples before and after K
			for(int s = 0; s < window_size; s++)
			{
				//Before
				double mydiff = diff - (s+1);
				if(sr_ratio < 1.0) val = SynthMath.sinc(sr_ratio * mydiff);
				else val = SynthMath.sinc(mydiff);
				val *= (double)swindows[c].getPastSample(s);
				val *= window.getMultiplier(mydiff + shift);
				sum += val;
				
				//After
				mydiff = diff + (s+1);
				if(sr_ratio < 1.0) val = SynthMath.sinc(sr_ratio * mydiff);
				else val = SynthMath.sinc(mydiff);
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
	
	/*----- Stream -----*/
	
	public int[] nextSample() throws InterruptedException
	{
		int[] next = super.nextSample();
		if(inbuffer != null)
		{
			//TODO Just pop? Something eles?
			//Pop
			for(StateBuffer b : inbuffer) b.pop();
		}
		return next;
	}

	public void close()
	{
		super.close();
		for(int i = 0; i < swindows.length; i++)swindows[i].flush();
		inbuffer = null;
		altin = null;
	}
	
	public boolean done(){
		//Source is done and sample window is all 0s
		if(!input.done()) return false;
		for(SampleWindow sw : swindows) if(!sw.zeroed()) return false;
		return true;
	}
}
