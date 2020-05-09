package waffleoRai_SoundSynth.general;

import waffleoRai_SoundSynth.SynthMath;

public class SquareLFO extends BasicLFO{
	
	public SquareLFO(int sampleRate) {
		super(sampleRate);
	}

	protected double getY(double x) {
		return SynthMath.quickSquare(x);
	}

}
