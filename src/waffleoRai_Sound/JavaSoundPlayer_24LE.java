package waffleoRai_Sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;

import waffleoRai_SoundSynth.AudioSampleStream;

public class JavaSoundPlayer_24LE extends JavaSoundPlayer{
	
	private int ch_count;
	private int b_per_frame;
	
	public JavaSoundPlayer_24LE(AudioSampleStream input){
		super(input);
		ch_count = input.getChannelCount();
		b_per_frame = ch_count*3;
	}
	
	protected byte[] frame2Bytes(int[] samples) {
		byte[] bytes = new byte[b_per_frame];
		if(samples == null) return bytes;
		
		int i = 0;
		for(int c = 0; c < ch_count; c++){
			if(c >= samples.length)break;
			
			int s = samples[c];
			int lo = s & 0xFF;
			int mid = (s >>> 8) & 0xFF;
			int hi = (s >>> 16) & 0xFF;
			
			bytes[i++] = (byte)lo;
			bytes[i++] = (byte)mid;
			bytes[i++] = (byte)hi;
		}
		
		return bytes;
	}

	protected int bytesPerSample(){return 3;}
	
	protected AudioFormat getOutputFormat() {
		return new AudioFormat(Encoding.PCM_SIGNED, source.getSampleRate(), 
				24, ch_count, b_per_frame, source.getSampleRate(), false);
	}

}
