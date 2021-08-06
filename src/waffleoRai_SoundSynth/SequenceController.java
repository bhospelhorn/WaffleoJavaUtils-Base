package waffleoRai_SoundSynth;

public interface SequenceController {

	//Interface that can be auto controlled by a sequence or by something reading a sequence
	//Can interface to a player or converter or whatever.
	
	//Overall settings
	public void setMasterVolume(byte value);
	public void setMasterPan(byte value);
	public void setTempo(int tempo_uspqn);
	public void addMarkerNote(String note);
	
	//Channel settings
	public void setChannelVolume(int ch, byte value);
	public void setChannelExpression(int ch, byte value);
	public void setChannelPan(int ch, byte value);
	public void setChannelPriority(int ch, byte value);
	public void setModWheel(int ch, short value);
	public void setPitchWheel(int ch, short value);
	public void setPitchBend(int ch, int cents);
	public void setPitchBendRange(int ch, int cents);
	public void setProgram(int ch, int bank, int program);
	public void setProgram(int ch, int program);
	public void addNRPNEvent(int ch, int index, int value, boolean omitFine);
	
	//Music control
	public void noteOn(int ch, byte note, byte vel);
	public void noteOff(int ch, byte note, byte vel);
	public void noteOff(int ch, byte note);
	
	public long advanceTick();
	public void complete();
	
}
