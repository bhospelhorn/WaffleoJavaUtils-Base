package waffleoRai_SoundSynth.general;

import waffleoRai_SoundSynth.AudioSampleStream;
import waffleoRai_SoundSynth.Filter;

public class Amp implements Filter{
	
	private AudioSampleStream source;
	private double ampratio;
	
	public Amp(AudioSampleStream input)
	{
		this(input, 1.0);
	}
	
	public Amp(AudioSampleStream input, double initRatio)
	{
		source = input;
		ampratio = initRatio;
	}

	@Override
	public float getSampleRate() {
		return source.getSampleRate();
	}

	@Override
	public int getBitDepth() {
		return source.getBitDepth();
	}

	@Override
	public int getChannelCount() {
		return source.getChannelCount();
	}

	public void setLevel8(byte level)
	{
		if(level < 0) level *= -1;
		ampratio = (int)level/(double)0x7F;
	}
	
	public void setLevel16(short level)
	{
		if(level < 0) level *= -1;
		ampratio = (int)level/(double)0x7FFF;
	}
	
	public void setLevel24(int level)
	{
		if(level < 0) level *= -1;
		ampratio = level/(double)0x7FFFFF;
	}
	
	public void setLevel32(int level)
	{
		if(level < 0) level *= -1;
		ampratio = level/(double)0x7FFFFFFF;
	}
	
	public void setAmplitudeRatio(double aratio)
	{
		ampratio = aratio;
	}
	
	@Override
	public int[] nextSample() throws InterruptedException 
	{

		int[] in = source.nextSample();
		int ccount = in.length;
		int[] out = new int[ccount];
		for(int c = 0; c < ccount; c++)
		{
			out[c] = (int)Math.round((double)in[c] * ampratio);
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
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}


}
