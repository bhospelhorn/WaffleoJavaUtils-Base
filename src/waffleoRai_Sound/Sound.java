package waffleoRai_Sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import waffleoRai_SoundSynth.AudioSampleStream;

public interface Sound {

	public AudioFormat getFormat();
	public AudioInputStream getStream();
	public AudioSampleStream createSampleStream();
	public AudioSampleStream createSampleStream(boolean loop);
	//public AudioSampleStream createSampleStream(int sampleRateHz);
	
	public void setActiveTrack(int tidx);
	public int countTracks();
	
	@Deprecated
	public void jumpToFrame(int frame);
	@Deprecated
	public void rewind();
	@Deprecated
	public int nextSample(int channel);
	@Deprecated
	public int samplesLeft(int channel);
	@Deprecated
	public boolean hasSamplesLeft(int channel);
	public int totalFrames();
	public int totalChannels();
	
	public Sound getSingleChannel(int channel);
	
	public int[] getRawSamples(int channel);
	public int[] getSamples_16Signed(int channel);
	public int[] getSamples_24Signed(int channel);
	public BitDepth getBitDepth();
	public int getSampleRate();
	
	public boolean loops();
	public int getLoopFrame();
	public int getLoopEndFrame();
	
	public int getUnityNote();
	public int getFineTune();
	
	public void flushBuffer();
	
	public static int scaleSampleUp8Bits(int sample, BitDepth bits)
	{
		if (bits.getBitCount() == 32) return sample;
		if (bits.isSigned())
		{
			int shift = bits.getBitCount() - 1;
			int max = 0xFFFFFFFF;
			max = max << shift;
			
			int tmax = max << 8;
			max = ~max;
			tmax = ~tmax;
			
			double ratio = (double)sample/(double)max;
			return (int)Math.round(ratio * (double)tmax);
		}
		else
		{
			int shift = bits.getBitCount();
			long max = ~(0L);
			max = max << shift;
			
			long tmax = max << 8;
			max = ~max;
			tmax = ~tmax;
			
			double ratio = (double)sample/(double)max;
			return (int)Math.round(ratio * (double)tmax);
		}

	}
	
	public static int scaleSampleDown8Bits(int sample, BitDepth bits)
	{
		if (bits.getBitCount() == 8) return sample;
		if (bits.isSigned())
		{
			int shift = bits.getBitCount() - 1;
			int max = 0xFFFFFFFF;
			max = max << shift;
			
			int tmax = max >>> 8;
			max = ~max;
			tmax = ~tmax;
			
			double ratio = (double)sample/(double)max;
			return (int)Math.round(ratio * (double)tmax);
		}
		else
		{
			int shift = bits.getBitCount();
			long max = ~(0L);
			max = max << shift;
			
			long tmax = max >>> 8;
			max = ~max;
			tmax = ~tmax;
			
			double ratio = (double)sample/(double)max;
			return (int)Math.round(ratio * (double)tmax);
		}
	}
	
	public static int scaleSampleToUnsigned(int sample, BitDepth bits)
	{
		if (!bits.isSigned()) return sample;
		
		int shift = bits.getBitCount();
		long add = 1;
		add = add << shift;
		
		long sl = Integer.toUnsignedLong(sample);
		
		sl += add;

		return (int)sl;
	}
	
	public static int scaleSampleToSigned(int sample, BitDepth bits)
	{
		if (bits.isSigned()) return sample;
		
		int shift = bits.getBitCount();
		long add = 1;
		add = add << shift;
		
		long sl = Integer.toUnsignedLong(sample);
		
		sl -= add;

		return (int)sl;
	}

}
