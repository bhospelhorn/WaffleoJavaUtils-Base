package waffleoRai_soundbank;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import waffleoRai_Sound.Sound;
import waffleoRai_SoundSynth.AudioSampleStream;
import waffleoRai_SoundSynth.SoundSampleMap;

public class BasicSoundSampleMap implements SoundSampleMap{
	
	private ConcurrentMap<Integer, Sound> map;
	
	public BasicSoundSampleMap(){
		map = new ConcurrentHashMap<Integer, Sound>();
	}
	
	public void mapSample(int i, Sound s){
		map.put(i, s);
	}

	@Override
	public AudioSampleStream openSampleStream(String samplekey) {
		Sound s = map.get(Integer.parseInt(samplekey));
		if(s == null) return null;
		return s.createSampleStream();
	}

	@Override
	public AudioSampleStream openSampleStream(int index) {
		Sound s = map.get(index);
		if(s == null) return null;
		return s.createSampleStream();
	}

	@Override
	public AudioSampleStream openSampleStream(int index0, int index1) {
		Sound s = map.get(index1);
		if(s == null) return null;
		return s.createSampleStream();
	}
	
	

}
