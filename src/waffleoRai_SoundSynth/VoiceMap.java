package waffleoRai_SoundSynth;

import java.util.Collection;

public interface VoiceMap extends Iterable<SynthSampleStream>{
	
	public boolean hasVoice(int note);
	public boolean setVoice(int note, SynthSampleStream voice);
	public SynthSampleStream removeVoice(int note);
	public Collection<Integer> allCurrentNotes();
	public Collection<SynthSampleStream> removeAll();
	public void clear();
	public int countVoices();
	
	public SynthSampleStream removeOneVoice();
	
	/*public int getTotalAdded();
	public int getTotalRemoved();
	public int getNetVoiceTurnover();
	public void resetCounters();
	public void tag();*/

}
