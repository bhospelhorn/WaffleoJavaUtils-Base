package waffleoRai_Sound;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class SampleChannel32 implements SampleChannel{
	
	private List<Integer> samples;
	private int used_bits;
	
	private SampleChannel32(){used_bits = 32;}
	
	public SampleChannel32(SampleChannel src)
	{
		//samples = new ArrayList<Integer>(src.samples.size() + 2);
		//samples.addAll(src.samples);
		samples = new ArrayList<Integer>(src.countSamples() + 2);
		for(Integer s : src)samples.add(s);
		used_bits = 32;
	}
	
	public static SampleChannel32 createArrayChannel(int initSize)
	{
		SampleChannel32 c = new SampleChannel32();
		c.samples = new ArrayList<Integer>(initSize+1);
		return c;
	}
	
	public static SampleChannel32 createLinkedChannel()
	{
		SampleChannel32 c = new SampleChannel32();
		c.samples = new LinkedList<Integer>();
		return c;
	}
	
	@Override
	public Iterator<Integer> iterator() 
	{
		return samples.iterator();
	}
	
	public void setUsedBitDepth(int bits){
		if(bits < 0) bits = 0; if(bits > 32) bits = 32;
		used_bits = bits;
	}
	
	public int getUsedBitDepth(){return used_bits;}
	public int getStorageBitDepth(){return 32;}
	public void addSample(int sample){samples.add(sample);}
	public void clearSamples(){samples.clear();}
	public int countSamples(){return samples.size();}
	public int getSample(int index){return samples.get(index);}
	
	public void setSample(int idx, int value){
		if(idx < 0 || idx >= samples.size()) throw new IndexOutOfBoundsException();
		samples.set(idx, value);
	}
	
	public int[] toArray()
	{
		int[] arr = new int[samples.size()];
		int i = 0;
		for(Integer s : samples) {arr[i] = s; i++;}
		return arr;
	}

	public List<Integer> toList(){
		int sz = samples.size();
		ArrayList<Integer> copy = new ArrayList<Integer>(sz+1);
		copy.addAll(samples);
		
		return copy;
	}
	
	public SampleChannel32 copyme(){
		SampleChannel32 copy = new SampleChannel32();
		copy.samples = toList();
		copy.used_bits = used_bits;
		
		return copy;
	}
	
	public SampleChannel copy(){return copyme();}
	
}
