package waffleoRai_SoundSynth.general;

import java.util.Random;

public class RandomLFO extends BasicLFO{
	
	private Random r;
	
	public RandomLFO(int sampleRate) {
		super(sampleRate);
		r = new Random();
	}

	protected double getY(double x) {
		return r.nextDouble();
	}

}
