package waffleoRai_Sound;

import javax.sound.sampled.AudioFormat;

import waffleoRai_SoundSynth.AudioSampleStream;

public abstract class SoundAdapter implements Sound{
	
	protected int sampleRate;
	protected int bitDepth;
	protected int chCount;
	
	public SoundAdapter(){
		this(44100, 16, 1);
	}
	
	public SoundAdapter(int sample_rate, int bit_depth, int channels){
		sampleRate = sample_rate;
		bitDepth = bit_depth;
		chCount = channels;
	}
	
	public AudioFormat getFormat() {
		AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, (float)sampleRate, 
				bitDepth, 
				chCount, (bitDepth/8) * chCount,
				(float)sampleRate, true);
		return format;
	}

	public AudioSampleStream createSampleStream() {return createSampleStream(true);}
	public void setActiveTrack(int tidx) {}
	public int countTracks() {return 0;}

	public int totalChannels() {return chCount;}
	public int getSampleRate() {return sampleRate;}
	public BitDepth getBitDepth() {
		switch(bitDepth){
		case 8: return BitDepth.EIGHT_BIT_UNSIGNED;
		case 16: return BitDepth.SIXTEEN_BIT_SIGNED;
		case 24: return BitDepth.TWENTYFOUR_BIT_SIGNED;
		}
		return null;
	}

	public boolean loops() {return false;}
	public int getLoopFrame() {return -1;}
	public int getLoopEndFrame() {return -1;}

	public int getUnityNote() {return 60;}
	public int getFineTune() {return 0;}

}
