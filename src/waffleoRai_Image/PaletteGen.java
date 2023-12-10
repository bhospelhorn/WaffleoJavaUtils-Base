package waffleoRai_Image;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PaletteGen {
	
	/*----- Constants -----*/
	
	private static final int SORT_MODE_HSB = 0;
	private static final int SORT_MODE_FREQ = 1;
	private static final int SORT_MODE_CLOSEHITS = 2;
	private static final int SORT_MODE_AVGRANK = 3;
	
	/*----- Instance Variables -----*/
	
	private boolean includeAlpha;
	private int bitDepth;
	
	private CounterNode head;
	private CounterNode tail;
	private Map<Integer, CounterNode> ctrMap;
	
	private int sortMode = SORT_MODE_HSB;
	
	/*----- Inner Classes -----*/
	
	private class CounterNode implements Comparable<CounterNode>{
		public int argb;
		public int count = 0;
		public float[] hsb;
		
		public CounterNode next = null;
		public CounterNode prev = null;
		
		public double closestValue = Double.MAX_VALUE;
		public CounterNode closestFriend = null;
		public int friends = 0;
		
		public int freqRank = -1;
		public int friendRank = -1;
		public double avgRank = Double.NaN;
		
		public void updateHSB(){
			int r = (argb >>> 16) & 0xff;
			int g = (argb >>> 8) & 0xff;
			int b = argb & 0xff;
			hsb = Color.RGBtoHSB(r, g, b, hsb);
		}
		
		public double hsbDist(float[] other_hsb){
			if(other_hsb != null){
				double hdist = (double)hsb[0] - (double)other_hsb[0];
				double sdist = (double)hsb[1] - (double)other_hsb[1];
				double bdist = (double)hsb[2] - (double)other_hsb[2];
				hdist *= hdist;
				sdist *= sdist;
				bdist *= bdist;
				return Math.sqrt(hdist + sdist + bdist);
			}
			return 0.0;
		}

		public boolean equals(Object o){
			if(o == null) return false;
			if(o == this) return true;
			if(!(o instanceof CounterNode)) return false;
			
			CounterNode other = (CounterNode)o;
			if(hsb == null) updateHSB();
			if(other.hsb == null) other.updateHSB();
			if(hsb[0] != other.hsb[0]) return false;
			if(hsb[1] != other.hsb[1]) return false;
			if(hsb[2] != other.hsb[2]) return false;
			
			return true;
		}
		
		public int compareTo(CounterNode o) {
			if(o == null) return 1;
			
			switch(sortMode){
			case SORT_MODE_FREQ:
				//Higher goes first
				if(this.count < o.count) return 1;
				if(o.count < this.count) return -1;
				break; //Go to HSB sort
			case SORT_MODE_CLOSEHITS:
				//Higher goes first
				if(this.friends < o.friends) return 1;
				if(o.friends < this.friends) return -1;
				break; //Go to HSB sort
			case SORT_MODE_AVGRANK:
				//Lower goes first
				if(this.avgRank > o.avgRank) return 1;
				if(o.avgRank > this.avgRank) return -1;
				break; //Go to HSB sort
			}
			
			//Default sort mode
			if(hsb == null) updateHSB();
			if(o.hsb == null) o.updateHSB();
			
			if(this.argb == o.argb) return 0;
			
			if(this.hsb[0] > o.hsb[0]) return 1;
			if(this.hsb[0] < o.hsb[0]) return -1;
			if(this.hsb[1] > o.hsb[1]) return 1;
			if(this.hsb[1] < o.hsb[1]) return -1;
			if(this.hsb[2] > o.hsb[2]) return 1;
			if(this.hsb[2] < o.hsb[2]) return -1;

			return 0;
		}
		
	}
	
	/*----- Init -----*/
	
	public PaletteGen(int bitDepth, boolean inclAlpha){
		this.bitDepth = bitDepth;
		this.includeAlpha = inclAlpha;
		head = null;
		tail = null;
		ctrMap = new HashMap<Integer, CounterNode>();
	}
	
	/*----- Internal -----*/
	
	private void tallyPixelValue(int argb){
		if(!includeAlpha) argb |= 0xff000000;
		
		CounterNode cn = ctrMap.get(argb);
		if(cn == null){
			cn = new CounterNode();
			cn.argb = argb;
			cn.count = 1;
			ctrMap.put(argb, cn);
			cn.updateHSB();
			
			//Plunk on tail.
			if(head == null){
				head = cn;
				tail = cn;
			}
			else{
				tail.next = cn;
				cn.prev = tail;
				tail = cn;
			}
		}
		else{
			cn.count++;
			//See how far up to move.
			CounterNode newprev = cn.prev;
			while(newprev != null){
				if(newprev.count >= cn.count){
					break;
				}
				newprev = newprev.prev;
			}
			
			CounterNode p = cn.prev;
			CounterNode n = cn.next;
			if(newprev != null){
				cn.next = newprev.next;
				cn.prev = newprev;
				if(cn.next != null){
					cn.next.prev = cn;
				}
				else tail = cn;
				
				newprev.next = cn;
			}
			else{
				//This node is new head.
				cn.prev = null;
				cn.next = head;
				
				if(head != null){
					head.prev = cn;
				}
				else{
					tail = cn;
				}
				
				head = cn;
			}
			if(p != null) {
				p.next = n;
				if(n == null) tail = p;
			}
			if(n != null){
				n.prev = p;
			}
		}
		
	}
	
	/*----- Getters -----*/
	
	public int[] generatePalette(){
		int ccount = 1 << bitDepth;
		ArrayList<CounterNode> selected = new ArrayList<CounterNode>(ccount);

		if(ctrMap.size() > ccount){
			//Rank by frequency...
			ArrayList<CounterNode> all = new ArrayList<CounterNode>(ctrMap.size());
			all.addAll(ctrMap.values());
			sortMode = SORT_MODE_FREQ;
			Collections.sort(all);
			
			int i = 0;
			for(CounterNode cn : all){
				cn.freqRank = i++;
				
				cn.closestFriend = null;
				cn.closestValue = Double.MAX_VALUE;
			}
			
			//Build graph of closest values
			i = 0;
			int sz = all.size();
			for(CounterNode cn : all){
				for(int j = i+1; j < sz; j++){
					CounterNode other = all.get(j);
					double dist = cn.hsbDist(other.hsb);
					if(dist < cn.closestValue){
						cn.closestFriend = other;
						cn.closestValue = dist;
					}
					if(dist < other.closestValue){
						other.closestFriend = cn;
						other.closestValue = dist;
					}
				}
				cn.friends = 0;
				i++;
			}
			
			for(CounterNode cn : all){
				if(cn.closestFriend != null){
					cn.closestFriend.friends++;
				}
			}
			
			sortMode = SORT_MODE_CLOSEHITS;
			Collections.sort(all);
			i = 0;
			for(CounterNode cn : all){
				cn.friendRank = i++;
				cn.avgRank = ((double)cn.freqRank + (double)cn.friendRank) / 2.0;
			}
			
			sortMode = SORT_MODE_AVGRANK;
			Collections.sort(all);
			i = 0;
			for(CounterNode cn : all){
				if(i >= ccount) break;
				selected.add(cn);
				i++;
			}
		}
		else{
			//Just return all, ig
			selected.addAll(ctrMap.values());
		}
		sortMode = SORT_MODE_HSB;
		Collections.sort(selected);
		
		int[] out = new int[ccount];
		for(int i = 0; i < selected.size(); i++){
			out[i] = selected.get(i).argb;
		}
		
		return out;
	}
	
	/*----- Setters -----*/
	
	public void processImage(int[][] imgData){
		if(imgData == null) return;
		for(int i = 0; i < imgData.length; i++){
			if(imgData[i] == null) continue;
			for(int j = 0; j < imgData[i].length; j++){
				tallyPixelValue(imgData[i][j]);
			}
		}
	}
	
	public void processImage(BufferedImage img){
		if(img == null) return;
		int w = img.getWidth();
		int h = img.getHeight();
		for(int y = 0; y < h; y++){
			for(int x = 0; x < w; x++){
				tallyPixelValue(img.getRGB(x, y));
			}
		}
	}
	
	public void flush(){
		head = null; tail = null;
		ctrMap.clear();
	}

}
