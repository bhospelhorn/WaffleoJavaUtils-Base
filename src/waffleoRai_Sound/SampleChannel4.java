package waffleoRai_Sound;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SampleChannel4 implements SampleChannel{
	
	private byte[] samples;
	private boolean s_odd; //if true then the sample count is odd
	private int len; //Marks array length
	
	private int used_bits;
	private boolean big_endian; //Higher nybble is first
	
	private SampleChannel4(){}
	
	public SampleChannel4(int initFrames){
		samples = new byte[initFrames/2 + 1];
		len = 0;
		used_bits = 4;
		big_endian = true;
	}
	
	public void setUsedBitDepth(int bits){
		if(bits < 0) bits = 0; if(bits > 4) bits = 4;
		used_bits = bits;
	}
	
	public int getUsedBitDepth(){return used_bits;}
	public int getStorageBitDepth(){return 4;}
	public void clearSamples(){len = 0; s_odd  = false;}
	public boolean isBigEndian(){return big_endian;}
	public void setNybbleOrder(boolean big_e){big_endian = big_e;}
	
	public int countSamples(){
		int scount = len << 1;
		if(s_odd) scount--; //Last byte not full
		return scount;
	}
	
	public void addSample(int sample){
		if(len >= samples.length){
			//Reallocate
			int cap = samples.length + (samples.length/2);
			byte[] old = samples;
			samples = new byte[cap];
			for(int i = 0; i < len; i++) samples[i] = old[i];
		}
		if(s_odd){
			int val = Byte.toUnsignedInt(samples[len-1]);
			if(big_endian){
				//put new value in lower nybble
				val |= (sample & 0xF);
			}
			else{
				//put new value in higher nybble
				val |= (sample & 0xF) << 4;
			}
			s_odd = false;
			samples[len-1] = (byte)val;
			//len NOT incremented
		}
		else{
			int val = sample & 0xF;
			if(big_endian) val = val << 4;
			samples[len++] = (byte)val;
			s_odd = true;
		}
	}
	
	public void setSample(int idx, int value){
		if(idx < 0 || idx >= countSamples()) throw new IndexOutOfBoundsException();
		int aidx = idx/2; if(aidx >= len) throw new IndexOutOfBoundsException();
		boolean odd = (idx % 2 != 0);
		int doublet = Byte.toUnsignedInt(samples[aidx]);
		if(big_endian){
			if(odd){doublet &= 0xF0; doublet |= (value & 0xF);}
			else {doublet &= 0xF; doublet |= ((value & 0xF) << 4);}
		}
		else{
			if(!odd){doublet &= 0xF0; doublet |= (value & 0xF);}
			else {doublet &= 0xF; doublet |= ((value & 0xF) << 4);}
		}
		samples[aidx] = (byte)doublet;
	}
	
	public void addByte(byte doublet){
		//Only does in frame.
		if(len >= samples.length){
			//Reallocate
			int cap = samples.length + (samples.length/2);
			byte[] old = samples;
			samples = new byte[cap];
			for(int i = 0; i < len; i++) samples[i] = old[i];
		}
		samples[len++] = doublet;
		s_odd = false;
	}
	
	public int getSample(int index){
		if(index < 0) throw new IndexOutOfBoundsException();
		//Get array index
		int aidx = index/2; if(aidx >= len) throw new IndexOutOfBoundsException();
		boolean odd = (index % 2 != 0);
		int doublet = Byte.toUnsignedInt(samples[aidx]);
		if(big_endian){
			if(odd) return doublet & 0xFF;
			else return (doublet >>> 4) & 0xFF;
		}
		else{
			if(!odd) return doublet & 0xFF;
			else return (doublet >>> 4) & 0xFF;
		}
	}
	
	public int[] toArray()
	{
		int scount = countSamples();
		int[] arr = new int[scount];
		int s = 0;
		for(int i = 0; i < len; i++){
			int doublet = Byte.toUnsignedInt(samples[i]);
			if(big_endian){
				arr[s++] = (doublet >>> 4) & 0xFF;
				if(s < arr.length) arr[s++] = doublet & 0xFF;
			}
			else{
				arr[s++] = doublet & 0xFF;
				if(s < arr.length) arr[s++] = (doublet >>> 4) & 0xFF;
			}
		}
		return arr;
	}

	public List<Integer> toList(){
		int scount = countSamples();
		ArrayList<Integer> copy = new ArrayList<Integer>(scount+1);
		int s = 0;
		for(int i = 0; i < len; i++){
			int doublet = Byte.toUnsignedInt(samples[i]);
			if(big_endian){
				copy.add((doublet >>> 4) & 0xFF); s++;
				if(s < scount) copy.add(doublet & 0xFF); s++;
			}
			else{
				copy.add(doublet & 0xFF); s++;
				if(s < scount) copy.add((doublet >>> 4) & 0xFF); s++;
			}
		}
		return copy;
	}

	private class MyIterator implements Iterator<Integer>{

		private int pos;
		
		public boolean hasNext() {
			return pos < countSamples();
		}
		
		public Integer next() {
			return getSample(pos++);
		}
		
	}
	
	public Iterator<Integer> iterator() {
		return new MyIterator();
	}

	public SampleChannel4 copyme(){
		SampleChannel4 copy = new SampleChannel4();
		copy.samples = new byte[samples.length];
		for(int i = 0; i < len; i++) copy.samples[i] = samples[i];
		
		copy.len = len;
		copy.used_bits = used_bits;
		copy.s_odd = s_odd;
		copy.big_endian = big_endian;
		return copy;
	}
	
	public SampleChannel copy(){return copyme();}
	
	
}
