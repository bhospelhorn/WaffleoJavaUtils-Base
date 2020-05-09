package waffleoRai_SoundSynth.general;

import waffleoRai_SoundSynth.SynthMath;

public class TriangleLFO extends BasicLFO{
	
	public TriangleLFO(int sampleRate) {
		super(sampleRate);
	}

	protected double getY(double x) {
		return SynthMath.quickTriangle(x);
	}

}
