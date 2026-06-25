package waffleoRai_SoundSynth.soundformats;

import java.io.IOException;

public interface PCMFileWriter {
	
	public void write(int frames) throws InterruptedException, IOException;
	public void complete() throws IOException;

}
