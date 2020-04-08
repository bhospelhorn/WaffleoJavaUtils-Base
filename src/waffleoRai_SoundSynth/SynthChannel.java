package waffleoRai_SoundSynth;

public interface SynthChannel extends AudioSampleStream{
	
	//public int getChannelCount();
	
	public void setBankIndex(int idx);
	public int getCurrentBankIndex();
	public void setProgram(SynthProgram program);
	
	public void noteOn(byte note, byte velocity) throws InterruptedException;
	public void noteOff(byte node, byte velocity);
	public void setPolyphony(boolean b);
	
	public void setPan(byte pan);
	public void setVolume(byte vol);
	public void setExpression(byte vol);
	
	public void setPitchBendDirect(int cents); //This adjusts range automatically!
	public void setPitchWheelLevel(short value);
	
	public void allNotesOff();
	public int countActiveVoices();
	
	//public int[] nextSample() throws InterruptedException;

}
