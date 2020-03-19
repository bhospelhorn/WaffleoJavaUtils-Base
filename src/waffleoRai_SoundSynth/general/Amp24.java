package waffleoRai_SoundSynth.general;

import waffleoRai_SoundSynth.AudioSampleStream;
import waffleoRai_SoundSynth.Filter;

public class Amp24 implements Filter{
	
	public static final int MAX_LEVEL = 0x7FFFFF;
	public static final double MAXRATIO_24_TO_32 = (double)MAX_LEVEL / (double)0x7FFFFFFF;
	public static final double MAXRATIO_24_TO_16 = (double)MAX_LEVEL / (double)0x7FFF;
	public static final double MAXRATIO_24_TO_8 = (double)MAX_LEVEL / (double)0x7F;
	
	private AudioSampleStream source;
	private int alevel;
	
	private int inmax;
	
	public Amp24(AudioSampleStream input, int initLevel)
	{
		source = input;
		/*if(source.getBitDepth() != 16){
			System.err.println("Amp16.<init> || WARNING: Amp16 does not scale for input that is not 16-bit signed.");
			System.err.println("Filter may produce undesired results!");
		}*/
		int bd = input.getBitDepth();
		if(bd == 8) inmax = 0x7F;
		else if (bd == 16) inmax = 0x7FFF;
		else if (bd == 32) inmax = 0x7FFFFFFF;
		else inmax = MAX_LEVEL;
		
		if(initLevel < 0)
		{
			System.err.println("Amp24.<init> || WARNING: Negative level not allowed. Multiplying by -1...");
			initLevel *= -1;
		}
		if(initLevel > MAX_LEVEL) initLevel = MAX_LEVEL;
		alevel = (int)initLevel;
	}

	@Override
	public float getSampleRate() {
		return source.getSampleRate();
	}

	@Override
	public int getBitDepth() {
		return 24;
	}

	@Override
	public int getChannelCount() {
		return source.getChannelCount();
	}

	public void setLevel8(byte level)
	{
		if(level < 0) level *= -1;
		alevel = (int)Math.round((double)level * MAXRATIO_24_TO_8);
	}
	
	public void setLevel16(short level)
	{
		if(level < 0) level *= -1;
		alevel = (int)Math.round((double)level * MAXRATIO_24_TO_16);
	}
	
	public void setLevel24(int level)
	{
		if(level < 0) level *= -1;
		if(level > MAX_LEVEL) level = MAX_LEVEL;
		alevel = level;
	}
	
	public void setLevel32(int level)
	{
		if(level < 0) level *= -1;
		alevel = (int)Math.round((double)level * MAXRATIO_24_TO_32);
	}
	
	@Override
	public int[] nextSample() throws InterruptedException 
	{

		int[] in = source.nextSample();
		int ccount = in.length;
		int[] out = new int[ccount];
		double dlv = (double)alevel;
		double maxin = (double)inmax;
		for(int c = 0; c < ccount; c++)
		{
			double dsamp = (double)in[c];
			out[c] = (int)Math.round((dsamp/maxin) * dlv);
		}
		
		return out;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setInput(AudioSampleStream input) {
		source = input;
		int bd = input.getBitDepth();
		if(bd == 8) inmax = 0x7F;
		else if (bd == 16) inmax = 0x7FFF;
		else if (bd == 32) inmax = 0x7FFFFFFF;
		else inmax = MAX_LEVEL;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

}
