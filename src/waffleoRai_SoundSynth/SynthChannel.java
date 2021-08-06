package waffleoRai_SoundSynth;

public interface SynthChannel extends AudioSampleStream{
	
	public static final int OP_RESULT_FAIL = 0;
	public static final int OP_RESULT_SUCCESS = 1;
	public static final int OP_RESULT_NOTEON_OVERLAP = 2;
	public static final int OP_RESULT_NOTEOFF_NOTON = 3;
	public static final int OP_RESULT_NOPROG = 4;
	public static final int OP_RESULT_NOREG = 5;
	
	//public int getChannelCount();
	
	public void setBankIndex(int idx);
	public int getCurrentBankIndex();
	public void setProgram(SynthProgram program);
	
	public int noteOn(byte note, byte velocity) throws InterruptedException;
	public int noteOff(byte node, byte velocity);
	public void setPolyphony(boolean b);
	
	public void setPan(byte pan);
	public void setVolume(byte vol);
	public void setExpression(byte vol);
	
	public void setPitchBendDirect(int cents); //This adjusts range automatically!
	public void setPitchWheelLevel(short value);
	
	public void setEffect(int effect, int value);
	
	public void allNotesOff();
	public int countActiveVoices();
	
	public void tagMe(boolean b, int i);
	
	public void clear();
	
	//public int[] nextSample() throws InterruptedException;

}
