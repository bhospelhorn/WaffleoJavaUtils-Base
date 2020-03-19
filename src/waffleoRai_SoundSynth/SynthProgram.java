package waffleoRai_SoundSynth;

public interface SynthProgram {

	public SynthSampleStream getSampleStream(byte pitch, byte velocity) throws InterruptedException;
	public SynthSampleStream getSampleStream(byte pitch, byte velocity, float targetSampleRate) throws InterruptedException;
}
