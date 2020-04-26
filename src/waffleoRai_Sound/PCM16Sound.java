package waffleoRai_Sound;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import waffleoRai_SoundSynth.AudioSampleStream;
import waffleoRai_SoundSynth.soundformats.PCMSampleStream;
import waffleoRai_SoundSynth.soundformats.WAVWriter;

public class PCM16Sound implements PCMSound{
	
	/*----- Instance Variables -----*/
	
	private SampleChannel16[] data;
	private float sampleRate;
	
	private int loopStart;
	private int loopEnd;
	
	private byte unityKey;
	private int fineTune_cents;
	
	/*----- Construction -----*/
	
	private PCM16Sound(){}
	
	private void constructCore(int ch, int frames){
		sampleRate = 44100;
		loopStart = -1;
		loopEnd = -1;
		unityKey = 60;
		
		data = new SampleChannel16[ch];
		for(int c = 0; c < ch; c++){
			data[c] = new SampleChannel16(frames);
		}
	}
	
	public static PCM16Sound createSound(int channels, int frameAlloc, float sample_rate){
		if(channels < 1) return null;
		if(frameAlloc < 1) return null;
		
		PCM16Sound snd = new PCM16Sound();
		snd.constructCore(channels, frameAlloc);
		snd.sampleRate = sample_rate;
		
		return snd;
	}
	
	public static PCM16Sound createSound(AudioSampleStream src, int frameAlloc){
		//Ignores bit depth of source! Reads it as 16 bit anyway!!! (Truncates to lowest 16 bits)
		
		if(frameAlloc < 1) return null;
		int channels = src.getChannelCount();
		if(channels < 1) return null;
		
		PCM16Sound snd = new PCM16Sound();
		snd.constructCore(channels, frameAlloc);
		snd.sampleRate = src.getSampleRate();
		
		
		//Copy samples
		for(int f = 0; f < frameAlloc; f++){
			try {
				int[] samps = src.nextSample();
				for(int c = 0; c < channels; c++){
					snd.data[c].addSample(samps[c]);
				}
			} 
			catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		return snd;
	}
	
	/*----- Getters -----*/
	
	public int totalChannels() {
		if(data == null) return 0;
		return data.length;
	}

	public BitDepth getBitDepth() {return BitDepth.SIXTEEN_BIT_SIGNED;}
	public int getSampleRate() {return (int)sampleRate;}
	
	public int totalFrames() {
		if(data == null) return 0;
		if(data[0] == null) return 0;
		return data[0].countSamples();
	}

	public boolean loops() {return loopStart != -1;}
	public int getLoopFrame() {return loopStart;}
	public int getLoopEndFrame() {return loopEnd;}
	public int getUnityNote() {return Byte.toUnsignedInt(unityKey);}
	public int getFineTune() {return fineTune_cents;}
	
	/*----- Setters -----*/
	
	public void setSampleRate(float sample_rate){sampleRate = sample_rate;}
	public void setLoopStart(int pos){loopStart = pos;}
	public void setLoopEnd(int pos){loopEnd = pos;}
	public void setLoopPoints(int stpos, int edpos){loopStart = stpos; loopEnd = edpos;}
	public void clearLoop(){loopStart = -1; loopEnd = -1;}
	public void setUnityKey(byte note){unityKey = note;}
	public void setFineTune(int cents){this.fineTune_cents = cents;}
	
	/*----- Data Views/Transform -----*/
	
	public int getSample(int channel, int frame) {
		if(data == null) throw new IndexOutOfBoundsException("Sound has no channels allocated");
		if(channel < 0 || channel >= data.length) throw new IndexOutOfBoundsException("Sound has no channel " + channel);
		if(data[channel] == null) throw new IndexOutOfBoundsException("Sound channel " + channel + " is not allocated!");
		return data[channel].getSample(frame);
	}
	
	public int[] getRawSamples(int channel) {
		if(data == null) throw new IndexOutOfBoundsException("Sound has no channels allocated");
		if(channel < 0 || channel >= data.length) throw new IndexOutOfBoundsException("Sound has no channel " + channel);
		if(data[channel] == null) throw new IndexOutOfBoundsException("Sound channel " + channel + " is not allocated!");
		return data[channel].toArray();
	}

	public int[] getSamples_16Signed(int channel) {return getRawSamples(channel);}
	
	public Sound getSingleChannel(int channel) {
		if(data == null) throw new IndexOutOfBoundsException("Sound has no channels allocated");
		if(channel < 0 || channel >= data.length) throw new IndexOutOfBoundsException("Sound has no channel " + channel);
		if(data[channel] == null) throw new IndexOutOfBoundsException("Sound channel " + channel + " is not allocated!");
		
		PCM16Sound copy = new PCM16Sound();
		copy.data = new SampleChannel16[1];
		copy.data[0] = data[channel].copyme();
		copy.sampleRate = sampleRate;
		copy.loopStart = loopStart;
		copy.loopEnd = loopEnd;
		copy.unityKey = unityKey;
		copy.fineTune_cents = fineTune_cents;
		return copy;
	}

	public int[] getSamples_24Signed(int channel) {
		
		int[] s16 = getRawSamples(channel);
		int[] s24 = new int[s16.length];
		for(int i = 0; i < s16.length; i++){
			double ratio = (double)s16[i]/(double)0x7FFF;
			s24[i] = (int)Math.round(ratio * (double)0x7FFFFF);
		}
		
		return s24;
	}
	
	public short getMaxAmplitude(){
		if(data == null) return 0;
		int chcount = data.length;
		int max = 0;
		
		for(int c = 0; c < chcount; c++){
			if(data[c] == null) continue;
			int fcount = data[c].countSamples();
			//System.err.println("Frame count: " + fcount);
			for(int f = 0; f < fcount; f++){
				int s = data[c].getSample(f);
				s = Math.abs(s);
				if(s > max) max = s;
			}
		}
		//Saturate just in case
		//System.err.println("Max (pre saturation): 0x" + String.format("%04x", max));
		max = Math.min(max, 0x7FFF);
		return (short)max;
	}
	
	public void normalizeAmplitude(){

		//Get max amplitude
		short max = getMaxAmplitude();
		//System.err.println("Max amp: 0x" + String.format("%04x", max) + " (" + max + ")");
		if(max == 0) return; //Assumed empty or silent. Can't divide by 0.
		
		//Determine ratio
		int m = Short.toUnsignedInt(max);
		double ratio = (double)0x7FFF / (double)m;
		//System.err.println("Ratio: "+ ratio);
		if(ratio <= 1.0) return; //Don't need to do anything.
		
		//Multiply all samples by this ratio
		if(data == null) return;
		for(int c = 0; c < data.length; c++){
			if(data[c] == null) continue;
			int fcount = data[c].countSamples();
			for(int f = 0; f < fcount; f++){
				double s = (double)data[c].getSample(f);
				s *= ratio;
				data[c].setSample(f, (int)Math.round(s));
			}
		}
	}
	
	/*----- Java Sound API -----*/
	
	public byte[] frame2Bytes(int frame){
		int fsz = totalChannels() << 1;
		if(fsz < 2) return null;
		byte[] b = new byte[fsz];
		
		int i = 0;
		for(int c = 0; c < data.length; c++){
			int s = 0;
			if(data[c] != null) s = data[c].getSample(frame);
			b[i++] = (byte)(s & 0xFF);
			b[i++] = (byte)((s >>> 8) & 0xFF);
		}
		
		return b;
	}
	
	public AudioFormat getFormat() {
		AudioFormat fmt = new AudioFormat(sampleRate, 16, data.length, true, false);
		return fmt;
	}

	private class PCM16InputStream extends InputStream{

		private int pos;
		
		private int fsize;
		private int chcount;
		private int fpos;
		private int[] frame;
		
		private boolean end;
		
		public PCM16InputStream(){
			pos = 0; fpos = 0;
			//Calculate frame size
			chcount = data.length;
			fsize = 2 * chcount;
			frame = new int[fsize];
			loadNextFrame();
		}
		
		private boolean loadNextFrame(){
			int f = 0;
			for(int c = 0; c < chcount; c++){
				if(pos >= data[c].countSamples()) return false;
				int samp = data[c].getSample(pos);
				frame[f++] = samp & 0xFF;
				frame[f++] = (samp >>> 8) & 0xFF;
			}
			fpos = 0;
			pos++;
			return true;
		}
		
		public int read() throws IOException {
			if(end) return -1;
			if(fpos >= frame.length){
				if(!loadNextFrame()){end = true; return -1;}
			}
			
			return frame[fpos++];
		}

	}
	
	public AudioInputStream getStream() {
		return new AudioInputStream(new PCM16InputStream(), getFormat(), totalFrames());
	}
	
	/*----- Sound Synthesis Interface -----*/

	public AudioSampleStream createSampleStream() {return createSampleStream(loops());}

	public AudioSampleStream createSampleStream(boolean loop) {return new PCMSampleStream(this, loop);}

	public void setActiveTrack(int tidx) {}
	public int countTracks() {return 1;}

	/*----- Output -----*/
	
	public void writeWAV(String path) throws IOException{
		WAVWriter wrt = new WAVWriter(createSampleStream(false), path);
		try{wrt.write(this.totalFrames());}
		catch(Exception e){e.printStackTrace();}
		wrt.complete();
	}
	
	public void writeTxt(String path) throws IOException{

		BufferedWriter bw = new BufferedWriter(new FileWriter(path));
		int fcount = totalFrames();
		for(int f = 0; f < fcount; f++){
			for(int c = 0; c < data.length; c++){
				int samp = data[c].getSample(f);
				if(c != 0) bw.write("\t");
				bw.write(Integer.toString(samp));
			}
			bw.write("\n");
		}
		
		bw.close();
	}
	

}
