package waffleoRai_Sound;

import java.util.List;

public interface SampleChannel extends Iterable<Integer>{
	
	public void addSample(int sample);
	public void clearSamples();
	public int countSamples();
	public int getSample(int index);
	public int getStorageBitDepth();
	public int getUsedBitDepth();
	
	public void setUsedBitDepth(int bits);
	public void setSample(int idx, int value);
	
	public int[] toArray();
	public List<Integer> toList();
	
	public SampleChannel copy();
	
}
