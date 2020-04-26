package waffleoRai_Sound;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SampleChannel16 implements SampleChannel{
	
	private short[] samples;
	private int len;
	private int used_bits;
	
	public SampleChannel16(int initFrames){
		samples = new short[initFrames];
		len = 0;
		used_bits = 16;
	}
	
	public void setUsedBitDepth(int bits){
		if(bits < 0) bits = 0; if(bits > 16) bits = 16;
		used_bits = bits;
	}
	
	public int getUsedBitDepth(){return used_bits;}
	public int getStorageBitDepth(){return 16;}
	public void clearSamples(){len = 0;}
	public int countSamples(){return len;}
	
	public void addSample(int sample){
		if(len >= samples.length){
			//Reallocate
			int cap = samples.length + (samples.length/2);
			short[] old = samples;
			samples = new short[cap];
			for(int i = 0; i < len; i++) samples[i] = old[i];
		}
		samples[len++] = (short)sample;
	}
	
	public void setSample(int idx, int value){
		if(idx < 0 || idx >= len) throw new IndexOutOfBoundsException();
		samples[idx] = (short)value;
	}
	
	public int getSample(int index){
		if(index < 0 || index >= len) throw new IndexOutOfBoundsException();
		return (int)samples[index];
	}
	
	public short getSample16(int index){
		if(index < 0 || index >= len) throw new IndexOutOfBoundsException();
		return samples[index];
	}
	
	public int[] toArray()
	{
		int[] arr = new int[len];
		for(int i = 0; i < len; i++) arr[i] = (int)samples[i];
		return arr;
	}

	public List<Integer> toList(){
		ArrayList<Integer> copy = new ArrayList<Integer>(len+1);
		for(int i = 0; i < len; i++) copy.add((int)samples[i]);
		return copy;
	}

	private class MyIterator implements Iterator<Integer>{

		private int pos;
		
		public boolean hasNext() {return pos < len;}
		public Integer next() {return (int)samples[pos++];}
		
	}
	
	public Iterator<Integer> iterator() {
		return new MyIterator();
	}
	
	public SampleChannel16 copyme(){
		SampleChannel16 copy = new SampleChannel16(len);
		for(int i = 0; i < len; i++) copy.samples[i] = samples[i];
		
		copy.len = len;
		copy.used_bits = used_bits;
		return copy;
	}
	
	public SampleChannel copy(){return copyme();}
	
	
}
