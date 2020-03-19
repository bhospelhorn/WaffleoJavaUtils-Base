package waffleoRai_SoundSynth;

public interface SoundSampleMap {
	
	public AudioSampleStream openSampleStream(String samplekey);
	public AudioSampleStream openSampleStream(int index);
	public AudioSampleStream openSampleStream(int index0, int index1);

}
