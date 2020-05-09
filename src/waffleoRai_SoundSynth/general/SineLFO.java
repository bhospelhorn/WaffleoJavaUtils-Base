package waffleoRai_SoundSynth.general;

import waffleoRai_SoundSynth.SynthMath;

public class SineLFO extends BasicLFO{

	public SineLFO(int sampleRate) {
		super(sampleRate);
	}

	protected double getY(double x) {
		return SynthMath.quicksin(x);
	}
	
	
	
}
