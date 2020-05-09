package waffleoRai_SoundSynth.general;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import waffleoRai_SoundSynth.SynthSampleStream;
import waffleoRai_SoundSynth.VoiceMap;

public class ArrayVoiceMap implements VoiceMap{

	private SynthSampleStream[] voices;
	
	//Debug
	/*private int in_count;
	private int out_count;
	private int net_count;*/
	
	public ArrayVoiceMap(){
		voices = new SynthSampleStream[128];
	}
	
	public boolean hasVoice(int note){
		if(note < 0 || note >= 128) return false;
		return(voices[note] != null);
	}
	
	public boolean setVoice(int note, SynthSampleStream voice){
		if(note < 0 || note >= 128) return false;
		if(voices[note] != null) return false;
		voices[note] = voice;
		
		//in_count++; net_count++;
		return true;
	}
	
	public SynthSampleStream removeVoice(int note){
		SynthSampleStream v = voices[note];
		if(v == null) return null;
		
		voices[note] = null;
		//out_count++; net_count--;
		
		return v;
	}
	
	public SynthSampleStream removeOneVoice(){
		for(int k = 0; k < 128; k++){
			if(voices[k] != null){
				SynthSampleStream v = voices[k];
				voices[k] = null;
				
				//out_count++; net_count--;
				return v;
			}
		}
		return null;
	}
	
	public Collection<Integer> allCurrentNotes(){
		List<Integer> list = new LinkedList<Integer>();
		for(int i = 0; i < 128; i++){
			if(voices[i] != null)list.add(i);
		}
		return list;
	}
	
	public Collection<SynthSampleStream> removeAll(){
		List<SynthSampleStream> col = new LinkedList<SynthSampleStream>();
		for(int i = 0; i < 128; i++){
			if(voices[i] != null){
				col.add(voices[i]);
				voices[i] = null;
				
				//out_count++; net_count--;
			}
		}
		
		return col;
	}
	
	public void clear(){
		for(int i = 0; i < 128; i++){
			if(voices[i] != null){
				voices[i] = null;
				
				//out_count++; net_count--;
			}
		}
	}
	
	public int countVoices(){
		int tot = 0;
		for(int i = 0; i < 128; i++){
			if(voices[i] != null) tot++;
		}
		return tot;
	}
	
	/*public void tag(){}
	public int getTotalAdded(){return in_count;}
	public int getTotalRemoved(){return out_count;}
	public int getNetVoiceTurnover(){return net_count;}
	public void resetCounters(){in_count = 0; out_count = 0; net_count = 0;}*/
	
	private class Iter implements Iterator<SynthSampleStream>{

		private int pos;
		private SynthSampleStream next;
		
		public Iter(){
			for(int i = 0; i < 128; i++){
				if(voices[i] != null){
					pos = i+1;
					next = voices[i];
					return;
				}
			}
			pos = 128;
		}
		
		public boolean hasNext() {
			return next != null;
		}

		public SynthSampleStream next() {
			SynthSampleStream n = next;
			
			boolean found = false;
			for(int i = pos; i < 128; i++){
				if(voices[i] != null){
					pos = i+1;
					next = voices[i];
					found = true;
					break;
				}
			}
			if(!found){
				pos = 128; next = null;
			}
			
			return n;
		}
		
	}
	
	public Iterator<SynthSampleStream> iterator() {
		return new Iter();
	}

}
