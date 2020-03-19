package waffleoRai_SoundSynth;

public interface Filter extends AudioSampleStream{
	
	public static final int LPF_MODE_BIQUAD_DIRECT_1 = 1;
	public static final int LPF_MODE_BIQUAD_DIRECT_2 = 2;

	public void setInput(AudioSampleStream input);
	public void reset();
	
}
