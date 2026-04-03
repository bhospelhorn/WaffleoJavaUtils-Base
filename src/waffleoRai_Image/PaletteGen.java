package waffleoRai_Image;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import waffleoRai_Utils.KMeansClustering;
import waffleoRai_Utils.KMeansClustering.KMeansResults;

public class PaletteGen {
	
	/*----- Constants -----*/
	
	private static final int SORT_MODE_HSB = 0;
	private static final int SORT_MODE_FREQ = 1;
	private static final int SORT_MODE_CLOSEHITS = 2;
	private static final int SORT_MODE_AVGRANK = 3;
	
	private static final long RANDOM_SEED = 0x6cc9fe22c330df5eL;
	
	/*----- Instance Variables -----*/
	
	//private boolean includeAlpha;
	private int alphaInclMode;
	private int alphaReplaceValue = 0x00ffffff;
	private int alphaThreshold = 0xaf;
	private boolean hasTransparent;
	private int bitDepth;
	
	private CounterNode head;
	private CounterNode tail;
	private Map<Integer, CounterNode> ctrMap;
	
	private int sortMode = SORT_MODE_HSB;
	
	/*----- Inner Classes -----*/
	
	private class CounterNode implements Comparable<CounterNode>{
		public int argb;
		public double alphaF;
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
			alphaF = (double)((argb >>> 24) & 0xff) / 255.0;
		}
		
		public double hsbDist(float[] other_hsb, double other_alpha){
			if(other_hsb != null){
				double hdist = (double)hsb[0] - (double)other_hsb[0];
				double sdist = (double)hsb[1] - (double)other_hsb[1];
				double bdist = (double)hsb[2] - (double)other_hsb[2];
				double adist = alphaF - other_alpha;
				hdist *= hdist;
				sdist *= sdist;
				bdist *= bdist;
				adist *= adist;
				return Math.sqrt(hdist + sdist + bdist + adist);
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
	
	public PaletteGen(int bitDepth, int alphaMode){
		this.bitDepth = bitDepth;
		//this.includeAlpha = inclAlpha;
		alphaInclMode = alphaMode;
		head = null;
		tail = null;
		ctrMap = new HashMap<Integer, CounterNode>();
	}
	
	/*----- Internal -----*/
	
	private void tallyPixelValue(int argb){
		int a = (argb >>> 24) & 0xff;
		switch(alphaInclMode) {
		case ImageUtils.ALPHA_MODE_IGNORE:
			argb |= 0xff000000;
			break;
		case ImageUtils.ALPHA_MODE_FLAG:
			if(a <= alphaThreshold) {
				argb = alphaReplaceValue;
				hasTransparent = true;
				return;
			}
			else argb |= 0xff000000;
			break;
		case ImageUtils.ALPHA_MODE_8BITFULL:
			if(a <= alphaThreshold) {
				hasTransparent = true;
			}
			break;
		}
		
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
	
	public int[] generatePalette() {
		final int MIN_ITER = 3;
		final int MAX_ITER = 10;
		
		int ccount = 1 << bitDepth;
		int k = ccount;
		if((alphaInclMode == ImageUtils.ALPHA_MODE_FLAG) && hasTransparent) k--;
		
		ArrayList<CounterNode> allNodes = new ArrayList<CounterNode>(ctrMap.size());
		allNodes.addAll(ctrMap.values());
		int nodeCount = allNodes.size();
		int[][] allColors = new int[nodeCount][];
		int i = 0;
		for(CounterNode n : allNodes) {
			/*int[] vec = ImageUtils.argb2Vector(n.argb);
			allColors[i] = new int[4];
			allColors[i][0] = vec[0];
			for(int j = 0; j < 3; j++) {
				float hsbRaw = n.hsb[j];
				allColors[i][j+1] = Math.round(hsbRaw * 255.0f);
			}
			i++;*/
			allColors[i++] = ImageUtils.argb2Vector(n.argb);
		}
		
		KMeansResults kmeans = KMeansClustering.cluster(allColors, k, MIN_ITER, MAX_ITER, RANDOM_SEED);
		
		//Go with centroid? Or nearest value? Let's try nearest value...
		ArrayList<CounterNode> pltNodes = new ArrayList<CounterNode>(k);
		for(int c = 0; c < k; c++) {
			double minDist = Double.MAX_VALUE;
			int minIdx = -1;
			for(int j = 0; j < nodeCount; j++) {
				if(kmeans.clusterAssignments[j] == c) {
					if(kmeans.centroidDists[j] < minDist) {
						minIdx = j;
						minDist = kmeans.centroidDists[j];
					}
				}
			}
			CounterNode n = allNodes.get(minIdx);
			pltNodes.add(n);
		}
		
		//Copy to output
		sortMode = SORT_MODE_HSB;
		Collections.sort(pltNodes);
		
		int[] plt = new int[ccount];
		i = 0;
		for(CounterNode n : pltNodes) {
			plt[i++] = n.argb;
		}
		
		if((alphaInclMode == ImageUtils.ALPHA_MODE_FLAG) && hasTransparent) {
			plt[k] = alphaReplaceValue;
		}
		
		return plt;
	}
	
	/*----- Setters -----*/
	
	public void setAlphaThreshold(int value) {alphaThreshold = value;}
	public void setAlphaReplacement(int argb) {alphaReplaceValue = argb;}
	
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
