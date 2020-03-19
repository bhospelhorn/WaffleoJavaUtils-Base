package waffleoRai_SoundSynth;

import waffleoRai_soundbank.adsr.Attack;
import waffleoRai_soundbank.adsr.Decay;
import waffleoRai_soundbank.adsr.Release;
import waffleoRai_soundbank.adsr.Sustain;

public abstract class SynthSampleStream implements AudioSampleStream{
	
	protected AudioSampleStream source;
	
	private int max_level;
	
	/*----- Construction -----*/
	
	public SynthSampleStream(AudioSampleStream src)
	{
		source = src;
		
		switch(source.getBitDepth())
		{
		case 8: max_level = 0x7F; break;
		case 16: max_level = 0x7FFF; break;
		case 24: max_level = 0x7FFFFF; break;
		case 32: max_level = 0x7FFFFFFF; break;
		}
	}
	
	/*----- Properties -----*/
	
	public int getBitDepth()
	{
		return source.getBitDepth();
	}
	
	public float getSampleRate()
	{
		return source.getSampleRate();
	}
	
	public int getChannelCount()
	{
		return source.getChannelCount();
	}
	
	public int getMaxPossibleAmplitude(){
		return max_level;
	}
	
	/*----- Volume Manipulation -----*/
	
	public abstract void setChannelVolume(byte vol);
	
	/*----- Pan -----*/
	
	public abstract double[] getInternalPanAmpRatios();
	
	/*----- Pitch Manipulation -----*/
	
	public abstract byte getKeyPlayed();
	public abstract void setPitchWheelLevel(int lvl);
	public abstract void setLFO(Oscillator osc);
	public abstract void removeLFO();
	
	/*----- Play -----*/
	
	public abstract void releaseMe();
	public abstract boolean releaseSamplesRemaining();

	/*----- ADSR/DAHDSR -----*/
	
	public abstract int getDelay();
	public abstract Attack getAttack();
	public abstract int getHold();
	public abstract Decay getDecay();
	public abstract Sustain getSustain();
	public abstract Release getRelease();
	
}
