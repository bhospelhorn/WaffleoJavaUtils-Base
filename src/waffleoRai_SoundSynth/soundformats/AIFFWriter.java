package waffleoRai_SoundSynth.soundformats;

import java.io.IOException;

import waffleoRai_Sound.AiffFile;
import waffleoRai_SoundSynth.AudioSampleStream;
import waffleoRai_Utils.FileBuffer;

public class AIFFWriter {
	
	private AudioSampleStream src;
	private String output_path;
	
	private AiffFile aiff;
	
	public AIFFWriter(AudioSampleStream input, String outpath) throws IOException{
		src = input;
		output_path = outpath;
		aiff = AiffFile.emptyAiffFile(input.getChannelCount());
		aiff.setBitDepth((short)input.getBitDepth());
		aiff.setSampleRate(input.getSampleRate());
	}
	
	public void setLoopPointsMetadata(int start, int end){
		aiff.setSustainLoop(AiffFile.LOOPMODE_FWD, start, end);
	}
	
	public void write(int frames) throws InterruptedException, IOException{
		if(frames < 1) return;
		aiff.allocateFrames(frames);
		for(int f = 0; f < frames; f++){
			int[] smpls = src.nextSample();
			aiff.addFrame(smpls);
		}
	}
	
	public void complete() throws IOException{
		FileBuffer aiff_ser = aiff.serializeAiff();
		aiff_ser.writeFile(output_path);
		aiff.dispose();
	}

}
