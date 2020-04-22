package waffleoRai_SoundSynth;

public interface SynthBank {

	public SynthProgram getProgram(int bankIndex, int programIndex);
	public void setName(String s);
	public String getName();
	
}
