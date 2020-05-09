package waffleoRai_SoundSynth.general;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import waffleoRai_SoundSynth.SynthSampleStream;
import waffleoRai_SoundSynth.VoiceMap;

public class TreeVoiceMap implements VoiceMap{
	
	private Map<Integer, SynthSampleStream> voices;
	
	//Debug
	/*private int in_count;
	private int out_count;
	private int net_count;
	private boolean tag;*/
	
	public TreeVoiceMap(){
		voices = new TreeMap<Integer, SynthSampleStream>();
	}
	
	public boolean setVoice(int note, SynthSampleStream voice){
		if(hasVoice(note)) return false;
		voices.put(note, voice);
		//if(tag && (voices.get(note) != null)) System.err.println("VoiceMap -- Voice added! (" + note + ")");
		
		//in_count++;
		//net_count++;
		return true;
	}
	
	public boolean hasVoice(int note){
		//if(tag) System.err.println("VoiceMap -- Voice check! (" + note + ")");
		return voices.containsKey(note);
	}
	
	public SynthSampleStream removeVoice(int note){
		SynthSampleStream v = voices.remove(note);
		if(v == null) return null;
		
		//if(tag) System.err.println("VoiceMap -- Voice remove! (" + note + ")");
		//out_count++; net_count--;
		
		return v;
	}
	
	public SynthSampleStream removeOneVoice(){
		if(voices.isEmpty()) return null;
		int k = 0;
		for(Integer key : voices.keySet()){k = key; break;}
		//if(tag) System.err.println("VoiceMap -- Random voice remove! (" + k + ")");
		return removeVoice(k);
	}
	
	public Collection<Integer> allCurrentNotes(){
		List<Integer> list = new LinkedList<Integer>();
		list.addAll(voices.keySet());
		//if(tag) System.err.println("VoiceMap -- Note list!");
		return list;
	}
	
	public Collection<SynthSampleStream> removeAll(){
		if(voices.isEmpty()) return new LinkedList<SynthSampleStream>();
		List<SynthSampleStream> col = new ArrayList<SynthSampleStream>(voices.values());
		voices.clear();
		
		/*if(tag) System.err.println("VoiceMap -- All voice remove!");
		int sz = col.size();
		out_count += sz; net_count -= sz;*/
		
		return col;
	}
	
	public void clear(){
		/*if(tag) System.err.println("VoiceMap -- Map clear!");
		int sz = voices.size();
		out_count += sz; net_count -= sz;*/

		voices.clear();
	}
	
	public int countVoices(){
		//if(tag) System.err.println("VoiceMap -- Voice count!");
		return voices.size();
	}
	
	/*public void tag(){tag = true;}
	public int getTotalAdded(){return in_count;}
	public int getTotalRemoved(){return out_count;}
	public int getNetVoiceTurnover(){return net_count;}
	public void resetCounters(){in_count = 0; out_count = 0; net_count = 0;}*/

	public Iterator<SynthSampleStream> iterator() {
		return voices.values().iterator();
	}

}
