package waffleoRai_Sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;

import waffleoRai_SoundSynth.AudioSampleStream;

public class JavaSoundPlayer_16LE extends JavaSoundPlayer{

	private int ch_count;
	
	public JavaSoundPlayer_16LE(AudioSampleStream input){
		super(input);
		ch_count = input.getChannelCount();
	}
	
	protected byte[] frame2Bytes(int[] samples) {
		byte[] bytes = new byte[ch_count << 1];
		if(samples == null) return bytes;
		
		int i = 0;
		for(int c = 0; c < ch_count; c++){
			if(c >= samples.length)break;
			
			int s = samples[c];
			int lo = s & 0xFF;
			int hi = (s >>> 8) & 0xFF;
			
			bytes[i++] = (byte)lo;
			bytes[i++] = (byte)hi;
		}
		
		return bytes;
	}

	protected int bytesPerSample(){return 2;}
	
	protected AudioFormat getOutputFormat() {
		return new AudioFormat(Encoding.PCM_SIGNED, source.getSampleRate(), 
				16, ch_count, ch_count << 1, source.getSampleRate(), false);
	}

}
