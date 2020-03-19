package waffleoRai_SoundSynth.soundformats;

import waffleoRai_Sound.RandomAccessSound;
import waffleoRai_SoundSynth.AudioSampleStream;

public class PCMSampleStream implements AudioSampleStream{
	
	/*----- Constants -----*/
	
	/*----- InstanceVariables -----*/
	
	private RandomAccessSound source;
	
	private int bitsPerSample;
	
	private int currentFrame;
	private int loopStart;
	private int loopEnd;
	private int maxFrame;
	
	/*----- Construction -----*/
	
	public PCMSampleStream(RandomAccessSound src)
	{
		source = src;
		switch(source.getBitDepth())
		{
		case EIGHT_BIT_UNSIGNED: bitsPerSample = 8; break;
		case SIXTEEN_BIT_SIGNED: bitsPerSample = 16; break;
		case SIXTEEN_BIT_UNSIGNED: bitsPerSample = 16; break;
		case THIRTYTWO_BIT_SIGNED: bitsPerSample = 32; break;
		case TWENTYFOUR_BIT_SIGNED: bitsPerSample = 24; break;
		default: bitsPerSample = 16; break;
		}
		
		currentFrame = 0;
		if(source.loops())
		{
			loopStart = source.getLoopFrame();
			loopEnd = source.getLoopEndFrame();
			maxFrame = loopEnd;
		}
		else
		{
			loopStart = -1;
			loopEnd = -1;
			maxFrame = source.totalFrames();
		}
	}

	/*----- Getters -----*/
	
	@Override
	public float getSampleRate() 
	{
		return (float)source.getSampleRate();
	}

	@Override
	public int getBitDepth() 
	{
		return bitsPerSample;
	}

	@Override
	public int getChannelCount() 
	{
		return source.totalChannels();
	}
	
	public int oneShotFrames()
	{
		return source.totalFrames();
	}
	
	public int framesForLoops(int loopCount)
	{
		if(loopStart < 0) return oneShotFrames();
		int preloop = loopStart;
		int loopsize = loopEnd - loopStart;
		
		return preloop + (loopsize * loopCount);
	}
	
	/*----- Setters -----*/
	
	/*----- Stream -----*/
	
	@Override
	public int[] nextSample() throws InterruptedException 
	{
		int ccount = source.totalChannels();
		int[] samps = new int[ccount];
		if(currentFrame >= maxFrame)
		{
			if(loopStart < 0) return samps; //All zeroes
			currentFrame = loopStart;
		}
		
		for(int i = 0; i < ccount; i++) samps[i] = source.getSample(i, currentFrame);
		
		currentFrame++;
		
		return samps;
	}
	
	@Override
	public void close() 
	{
		//Don't need to do anything
	}

}
