package waffleoRai_SoundSynth.general;

import waffleoRai_SoundSynth.SynthMath;

public class SawLFO extends BasicLFO{
	
	public SawLFO(int sampleRate) {
		super(sampleRate);
	}

	protected double getY(double x) {
		return SynthMath.quickSaw(x);
	}

}
