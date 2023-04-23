package waffleoRai_Sound.wav;

import waffleoRai_Utils.BufferReference;
import waffleoRai_Utils.FileBuffer;

public class WAVFmtSpecPCM implements WAVFormatSpecific{

	private short wBitsPerSample;
	
	public WAVFmtSpecPCM(){
		wBitsPerSample = 16;
	}

	public int getCodecEnum(){return WAVFormat.WAV_CODEC_PCM;}
	
	public short getBitsPerSample(){return wBitsPerSample;}
	public void setBitsPerSample(short val){wBitsPerSample = val;}
	
	public boolean readIn(BufferReference input) {
		if(input == null) return false;
		wBitsPerSample = input.nextShort();
		return true;
	}

	public FileBuffer serializeMe() {
		FileBuffer buff = new FileBuffer(2, false);
		buff.addToFile(wBitsPerSample);
		return buff;
	}
	
}
