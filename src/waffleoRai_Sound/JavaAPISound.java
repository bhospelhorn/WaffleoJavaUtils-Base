package waffleoRai_Sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import waffleoRai_SoundSynth.AudioSampleStream;

public class JavaAPISound implements Sound{
//TODO Had an idea to basically feed that AudioInputStream back into my shitty API
	//Will do eventually, but right now don't feel like it :D
	
	@Override
	public AudioFormat getFormat() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AudioInputStream getStream() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AudioSampleStream createSampleStream() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AudioSampleStream createSampleStream(boolean loop) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setActiveTrack(int tidx) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int countTracks() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int totalFrames() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int totalChannels() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Sound getSingleChannel(int channel) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] getRawSamples(int channel) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] getSamples_16Signed(int channel) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] getSamples_24Signed(int channel) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BitDepth getBitDepth() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getSampleRate() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean loops() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getLoopFrame() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getLoopEndFrame() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getUnityNote() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getFineTune() {
		// TODO Auto-generated method stub
		return 0;
	}

}
