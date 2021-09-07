package waffleoRai_Utils;

import java.util.ArrayList;
import java.util.Collections;

public class CoverageMap1D {
	
	private static class Block implements Comparable<Block>{
		public int start;
		public int end;
		
		public Block(int s, int e){
			start = s;
			end = e;
		}
		
		public boolean equals(Object o){
			if(o == null) return false;
			if(o == this) return true;
			if(!(o instanceof Block)) return false;
			
			Block other = (Block)o;
			if(this.start != other.start) return false;
			if(this.end != other.end) return false;
			
			return true;
		}
		
		public int hashCode(){
			return start ^ end;
		}
		
		public int compareTo(Block o) {
			if(o == null) return 1;
			if(this.start < o.start) return -1;
			else if (this.start > o.start) return 1;
			
			if(this.end < o.end) return -1;
			else if (this.end > o.end) return 1;
			
			return 0;
		}
	}
	
	private ArrayList<Block> blocks;
	
	public CoverageMap1D(){
		blocks = new ArrayList<Block>();
	}
	
	public CoverageMap1D(int blockAlloc){
		blocks = new ArrayList<Block>(blockAlloc);
	}
	
	public boolean addBlock(int start, int end){
		if(isCovered(start, end)) return false;
		Block b = new Block(start, end);
		blocks.add(b);
		Collections.sort(blocks);
		return true;
	}
	
	public boolean isCovered(int location){
		if(blocks.isEmpty()) return false;
		int idx = blocks.size()/2;
		int left = 0;
		int right = blocks.size();
		
		while(left < right && idx >= left && idx < right){
			Block check = blocks.get(idx);
			if(location < check.start){
				//Check before this one.
				right = idx;
			}
			else{
				if(location < check.end) return true;
				left = idx+1;
			}
			int space = right-left;
			if(space <= 0) break;
			idx = left + (space/2);
		}
		
		return false;
	}
	
	public boolean isCovered(int start, int end){
		if(blocks.isEmpty()) return false;
		int idx = blocks.size()/2;
		int left = 0;
		int right = blocks.size();
		
		while(left < right && idx >= left && idx < right){
			Block check = blocks.get(idx);
			if(end <= check.start){
				right = idx;
			}
			else{
				if(start >= check.end){
					left = idx+1;
				}
				else return true;
			}
			int space = right-left;
			if(space <= 0) break;
			idx = left + (space/2);
		}
		
		return false;
	}
	
	public void mergeBlocks(){
		ArrayList<Block> newlist = new ArrayList<Block>(Math.min(blocks.size(), 16));
		int i = 0;
		while(i < blocks.size()){
			Block b = blocks.get(i);
			int j = i+1;
			while(j < blocks.size()){
				Block b2 = blocks.get(j);
				if(b2.start <= b.end){
					//Merge
					b.end = b2.end;
					j++;
				}
				else break;
			}
			newlist.add(b);
			i = j;
		}
	}
	
	public int[][] getBlocks(){
		int bcount = blocks.size();
		if(bcount < 1) return null;
		int[][] out = new int[bcount][2];
		int i = 0;
		for(Block b : blocks){
			out[i][0] = b.start;
			out[i][1] = b.end;
			i++;
		}
		
		return out;
	}

}
