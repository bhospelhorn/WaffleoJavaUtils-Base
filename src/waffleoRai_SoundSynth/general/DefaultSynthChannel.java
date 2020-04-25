package waffleoRai_SoundSynth.general;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import waffleoRai_SoundSynth.PanUtils;
import waffleoRai_SoundSynth.SynthChannel;
import waffleoRai_SoundSynth.SynthProgram;
import waffleoRai_SoundSynth.SynthSampleStream;

public class DefaultSynthChannel implements SynthChannel{
	
	/* ----- Constants ----- */
	
	public static final int CH_IDX_LEFT = 0;
	public static final int CH_IDX_RIGHT = 1;
	
	public static final int DEFO_MAX_VOX = 8;
	
	/* ----- Instance Variables ----- */
	
	private boolean closed = false;
	
	private Map<Byte, SynthSampleStream> voices;
	//private Map<Byte, Deque<SynthSampleStream>> releasedVoices;
	private Deque<SynthSampleStream> releasedVoices;
	//private Deque<Byte> onnotes;
	
	private int max_vox;
	private int vox_count;
	
	private SynthProgram program;
	private int bidx;
	
	private int sample_rate;
	private int bitDepth;
	private int maxLevel;
	private int minLevel;
	
	private double ch_vol;
	private double ch_exp;
	private double[][] ch_pan;
	private short pitch_wheel;
	private boolean polyphonic;
	
	private int debug_tag;
	
	/* ----- Constants ----- */
	
	public DefaultSynthChannel(int outSampleRate, int bitDepth)
	{
		debug_tag = -1;
		voices = new TreeMap<Byte, SynthSampleStream>();
	//	releasedVoices = new TreeMap<Byte, Deque<SynthSampleStream>>();
		releasedVoices = new LinkedList<SynthSampleStream>();
		//onnotes = new LinkedList<Byte>();
		ch_vol = 1.0;
		ch_exp = 1.0;
		ch_pan = new double[2][2];
		ch_pan[0][0] = 1.0; ch_pan[0][1] = 1.0;
		ch_pan[1][0] = 1.0; ch_pan[1][1] = 1.0;
		polyphonic = true;
		sample_rate = outSampleRate;
		
		max_vox = DEFO_MAX_VOX;
		
		this.bitDepth = bitDepth;
		switch(bitDepth)
		{
		case 8:
			maxLevel = Byte.MAX_VALUE;
			minLevel = Byte.MIN_VALUE;
			break;
		case 16:
			maxLevel = Short.MAX_VALUE;
			minLevel = Short.MIN_VALUE;
			break;
		case 24:
			maxLevel = 0x7FFFFF;
			minLevel = ~maxLevel;
			break;
		case 32:
			maxLevel = Integer.MAX_VALUE;
			minLevel = Integer.MIN_VALUE;
			break;
		}
	}
	
	/* ----- Getters ----- */
	
	public int getChannelCount()
	{
		return 2;
	}
	
	public int getCurrentBankIndex()
	{
		return bidx;
	}
	
	public float getSampleRate() 
	{
		return sample_rate;
	}

	public int getBitDepth() {
		return bitDepth;
	}
	
	public int getDebugTag(){
		return debug_tag;
	}
	
	/* ----- Setters ----- */
	
	public void tagMe(boolean b, int i){
		//Debug method
		if(b) debug_tag = i;
		else debug_tag = -1;
	}
	
	public void setMaxVoiceCount(int max){
		max_vox = max;
	}
	
	/* ----- Management ----- */
	
	private void freeOldestVoice(){
		if(!releasedVoices.isEmpty()){
			SynthSampleStream voice = releasedVoices.pop();
			freeVoice(voice);
		}
		else{
			//We're gonna have to find a note to turn off
			//For now, just take the lowest note
			for(byte i = 0; i < 128; i++){
				if(voices.containsKey(i)){
					SynthSampleStream voice = voices.remove(i);	
					//It does not get a release.
					//This method only called when max voices hit
					freeVoice(voice);
					break;
				}
			}
		}
	}
	
	private void freeVoice(SynthSampleStream str)
	{
		str.close();
		vox_count--;
	}
	
	private void stopAllVoices()
	{
		//For monophony
		for(SynthSampleStream voice : voices.values()) freeVoice(voice);
		voices.clear();
		/*for(Deque<SynthSampleStream> vlist : releasedVoices.values()){
			for(SynthSampleStream voice : vlist) freeVoice(voice);
		}*/
		for(SynthSampleStream voice : releasedVoices) freeVoice(voice);
		releasedVoices.clear();
	}
		
	/*private void putReleasedVoice(byte note, SynthSampleStream voice){
		Deque<SynthSampleStream> vlist = releasedVoices.get(note);
		if(vlist == null){
			vlist = new LinkedList<SynthSampleStream>();
			releasedVoices.put(note, vlist);
		}
		vlist.add(voice);
	}
	
	private Collection<SynthSampleStream> getReleasedVoices(){
		List<SynthSampleStream> list = new LinkedList<SynthSampleStream>();
		for(Deque<SynthSampleStream> voices : releasedVoices.values()){
			list.addAll(voices);
		}
		
		return list;
	}*/
	
	/* ----- Control ----- */
	
	public void setBankIndex(int idx)
	{
		bidx = idx;
	}
	
	public void setProgram(SynthProgram program)
	{
		this.program = program;
		//if(debug_tag >= 0) System.err.println("Tag " + debug_tag + ": Program Change!");
	}
	
	public void noteOn(byte note, byte velocity) throws InterruptedException
	{
		//if(debug_tag >= 0) System.err.println("Tag " + debug_tag + ": Note on -- " + note);
		
		//Do nothing if there is no program.
		if(program == null) return;
		
		//Check if already playing
		if(voices.containsKey(note)) return;
		if(!polyphonic) stopAllVoices();
		/*else //Actually let's not
		{
			SynthSampleStream v = releasedVoices.remove(note);
			if(v != null) freeVoice(v);	
		}*/
		
		SynthSampleStream v = program.getSampleStream(note, velocity, sample_rate);
		/*if(debug_tag >= 0){
			OffsetDateTime now = OffsetDateTime.now();
			v.tagMe(true, (int)now.toEpochSecond() ^ now.getNano());
		}*/
		voices.put(note, v);
		vox_count++;
		
		if(vox_count > max_vox) freeOldestVoice();
	}
	
	public void noteOff(byte note, byte velocity)
	{
		//System.err.println("Note Off!");
		//if(debug_tag >= 0) System.err.println("Tag " + debug_tag + ": Note off -- " + note);
		SynthSampleStream v = voices.remove(note);
		if(v == null) return; //Nothing to do
		v.releaseMe();
		//releasedVoices.put(note, v);
		//putReleasedVoice(note, v);
		releasedVoices.add(v);
	}
	
	public void setPolyphony(boolean b)
	{
		polyphonic = b;
	}
	
	public void setPan(byte pan)
	{
		ch_pan = PanUtils.getLRAmpRatios_Stereo2Stereo(pan);
	}
	
	public void setExpression(byte vol)
	{
		ch_exp = (double)vol/(double)0x7F;
		//System.err.println("Channel volume set: " + ch_vol);
	}
	
	public void setVolume(byte vol)
	{
		ch_vol = (double)vol/(double)0x7F;
		//System.err.println("Channel volume set: " + ch_vol);
	}
	
	public void setPitchWheelLevel(short value)
	{
		pitch_wheel = value;
		for(SynthSampleStream voice : voices.values()) voice.setPitchWheelLevel((int)pitch_wheel);
		//Collection<SynthSampleStream> released = getReleasedVoices();
		//for(SynthSampleStream voice : releasedVoices.values()) voice.setPitchWheelLevel((int)pitch_wheel);
		for(SynthSampleStream voice : releasedVoices) voice.setPitchWheelLevel((int)pitch_wheel);
	}
	
	public void setPitchBendDirect(int cents){
		//Always scales to 12 semis for pitch wheel...
		pitch_wheel = (short)Math.round(((double)cents/1200.0) * (double)0x7FFF);
		for(SynthSampleStream voice : voices.values()) voice.setPitchBendDirect(cents);
		//for(SynthSampleStream voice : releasedVoices.values()) voice.setPitchBendDirect(cents);
		//Collection<SynthSampleStream> released = getReleasedVoices();
		for(SynthSampleStream voice : releasedVoices) voice.setPitchBendDirect(cents);
	}
	
	public void allNotesOff()
	{
		for(Byte k : voices.keySet())
		{
			SynthSampleStream v = voices.get(k);
			v.releaseMe();
			//releasedVoices.put(k, v);
			//putReleasedVoice(k,v);
			releasedVoices.add(v);
		}
		voices.clear();
	}
	
	public int countActiveVoices()
	{
		//return voices.size() + releasedVoices.size();
		/*Collection<SynthSampleStream> released = getReleasedVoices();
		return voices.size() + released.size();*/
		return vox_count;
	}
	
	/* ----- Stream ----- */
	
	private int saturate(int in)
	{
		if(in > maxLevel) return maxLevel;
		if(in < minLevel) return minLevel;
		return in;
	}
	
	public int[] nextSample() throws InterruptedException
	{
		/*int[] out = new int[2];
		for(SynthSampleStream voice : voices.values())
		{
			int[] s = voice.nextSample();
			out[0] = s[0]; out[1] = s[0];
		}
		return out;*/
		
		double[] sum = new double[2];
		for(SynthSampleStream voice : voices.values())
		{
			double mono = (double)voice.nextSample()[0];
			//System.err.println("Voice mono output: " + mono);
			if(voice.getBitDepth() != bitDepth)
			{
				//Scale...
				double prop = mono/(double)voice.getMaxPossibleAmplitude();
				mono = prop * (double)maxLevel;
			}
			double[] vpan = voice.getInternalPanAmpRatios();
			sum[0] += (mono * vpan[0]);
			sum[1] += (mono * vpan[1]);
			//if(!voices.isEmpty())System.err.println("Pan factors: " + vpan[0] + " | " + vpan[1]);
			//sum[0] += mono;
			//sum[1] += mono;
		}
		//Set<Byte> deletes = new TreeSet<Byte>();
		/*for(Byte k : releasedVoices.keySet())
		{
			Deque<SynthSampleStream> voicelist = releasedVoices.get(k);
			if(voicelist == null) continue;
			int size = voicelist.size();
			
			for(int i = 0; i < size; i++){
				//Pop
				SynthSampleStream voice = voicelist.pop();
				double mono = (double)voice.nextSample()[0];
				if(voice.getBitDepth() != bitDepth)
				{
					//Scale...
					double prop = mono/(double)voice.getMaxPossibleAmplitude();
					mono = prop * (double)maxLevel;
				}
				double[] vpan = voice.getInternalPanAmpRatios();
				sum[0] += mono * vpan[0];
				sum[1] += mono * vpan[1];
				//If done, free. If not, put back in list.
				if(voice.releaseSamplesRemaining()) voicelist.add(voice);
				else freeVoice(voice);
			}
			//System.err.println("Release voices: " + releasedVoices.size());
		}*/
		//for(Byte k : deletes){releasedVoices.remove(k);}
		
		int size = releasedVoices.size();
		for(int i = 0; i < size; i++){
			//Pop
			SynthSampleStream voice = releasedVoices.pop();
			double mono = (double)voice.nextSample()[0];
			if(voice.getBitDepth() != bitDepth)
			{
				//Scale...
				double prop = mono/(double)voice.getMaxPossibleAmplitude();
				mono = prop * (double)maxLevel;
			}
			double[] vpan = voice.getInternalPanAmpRatios();
			sum[0] += mono * vpan[0];
			sum[1] += mono * vpan[1];
			//If done, free. If not, put back in list.
			if(voice.releaseSamplesRemaining()) releasedVoices.add(voice);
			else freeVoice(voice);
		}
				
		//Apply channel level processing
		//Pan
		double[] panned = new double[2];
		panned[0] = (sum[0] * ch_pan[0][0]) + (sum[1] * ch_pan[1][0]);
		panned[1] = (sum[0] * ch_pan[0][1]) + (sum[1] * ch_pan[1][1]);
		//if(!voices.isEmpty())System.err.println("Raw channel stereo output: " + panned[0] + " | " + panned[1]);
		
		//Volume
		int[] out = new int[2];
		out[0] = saturate((int)Math.round(panned[0] * ch_vol * ch_exp));
		out[1] = saturate((int)Math.round(panned[1] * ch_vol * ch_exp));
		//if(!voices.isEmpty())System.err.println("Channel stereo output: " + String.format("%04x | %04x", out[0], out[1]));
		
		//out[0] = saturate((int)Math.round(sum[0]));
		//out[1] = saturate((int)Math.round(sum[1]));
		
		return out;
	}

	@Override
	public void close() {
		closed = true;
		//stopAllVoices();
		allNotesOff();
	}

	public boolean done(){
		//Has been closed and all voices stopped
		if(!closed) return false;
		return (releasedVoices.isEmpty());
	}
	
}
