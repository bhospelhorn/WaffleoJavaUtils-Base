package waffleoRai_Sound.wav;

import waffleoRai_Utils.BufferReference;
import waffleoRai_Utils.FileBuffer;

public interface WAVFormatSpecific {

	public int getCodecEnum();
	public boolean readIn(BufferReference input);
	public FileBuffer serializeMe();
	
}
