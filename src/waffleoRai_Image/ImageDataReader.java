package waffleoRai_Image;

import java.awt.image.BufferedImage;

import waffleoRai_Utils.BitStreamer;
import waffleoRai_Utils.BufferReference;

public class ImageDataReader {
	
	//Instance variables hold options
	//Use this to parse raw image data
	
	private int bitDepth = 32; //Defaults to ARGB, but can provide callback
	private ColorConversionCallback clrCallback;
	private int[] palette;
	
	private int tileWidth = 0;
	private int tileHeight = 0;
	
	private int tColSuggest = 8;
	private int widthSuggest = 64;
	
	private boolean bottomUp = false;
	private boolean rightOver = false;
	private boolean colsFirst = false; //Read rows or cols first?
	private boolean tileColsFirst = false; //Same but for tiles (cols OF tiles, not within)
	private boolean hiNibbleFirst = true; //For < 8 bit
	//Probably need some kind of padding options...
	
	public static interface ColorConversionCallback{
		public int toARGB(int rawValue);
	}
	
	/*--- Setters ---*/
	
	public void setBitDepth(int val) {bitDepth = val;}
	public void setColorConversionCallback(ColorConversionCallback func) {clrCallback = func;}
	public void setPalette(int[] plt) {palette = plt;}
	public void setTileSize(int width, int height) {tileWidth = width; tileHeight = height;}
	public void setTileColumnSuggestion(int count) {tColSuggest = count;}
	public void setWidthSuggestion(int val) {widthSuggest = val;}
	public void setReadBottomUp(boolean b) {bottomUp = b;}
	public void setReadRightToLeft(boolean b) {rightOver = b;}
	public void setReadPixelColumnsFirst(boolean b) {colsFirst = b;}
	public void setReadTileColumnsFirst(boolean b) {tileColsFirst = b;}
	public void setReadBitsMSBFirst(boolean b) {hiNibbleFirst = b;}
	
	/*--- Interface Methods ---*/
	
	private int rgb555BE_to_ARGB_defo(int rawValue) {
		boolean aBit = BitStreamer.readABit(rawValue, 16);
		int b = (rawValue) & 0x1F;
		int g = (rawValue >>> 5) & 0x1F;
		int r = (rawValue >>> 10) & 0x1F;
		
		int red = ImageUtils.scale5BitColor(r);
		int green = ImageUtils.scale5BitColor(g);
		int blue = ImageUtils.scale5BitColor(b);
		int alpha = 255;
		if ((rawValue & 0x7FFF) == 0){
			if(!aBit) alpha = 0;
		}
		
		int RGBA = 0;
		int R = red << 24;
		int G = green << 16;
		int B = blue << 8;
		int A = alpha;
		RGBA |= R | G | B | A;
		
		return RGBA;
	}
	
	//TODO On nextPix2 and 4, when byte gets split between tiles or rows, it's just being thrown out. We don't want that.
	
	private void nextPix2(BufferReference src, BufferedImage dst, int m, int n, int nMax, boolean nflip) {
		if(!src.hasRemaining()) return;
		int bb = Byte.toUnsignedInt(src.nextByte());
		
		int nn = n;
		for(int i = 0; i < 4; i++) {
			int val = 0;
			if(hiNibbleFirst) {
				//Read leftmost two
				val = bb >>> 6;
				bb <<= 2;
			}
			else {
				//Read rightmost two
				val = bb & 0x3;
				bb >>>= 2;
			}
			
			if(colsFirst) {
				dst.setRGB(m, nn, palette[val]);
			}
			else {
				dst.setRGB(nn, m, palette[val]);
			}
			
			if(nflip)n--;
			else n++;
			if(n >= nMax || n < 0) return;
		}
	}
	
	private void nextPix4(BufferReference src, BufferedImage dst, int m, int n, int nMax, boolean nflip) {
		if(!src.hasRemaining()) return;
		int bb = Byte.toUnsignedInt(src.nextByte());
		
		int p0 = 0;
		int p1 = 0;
		if(hiNibbleFirst) {
			p0 = (bb >>> 4) & 0xf;
			p1 = bb & 0xf;
		}
		else {
			p1 = (bb >>> 4) & 0xf;
			p0 = bb & 0xf;
		}
		
		int nn = n+1;
		if(nflip) nn = n-1;
		if(colsFirst) {
			dst.setRGB(m, n, palette[p0]);
			if(nn >= 0 && nn < nMax) dst.setRGB(m, nn, palette[p1]);
		}
		else {
			dst.setRGB(n, m, palette[p0]);
			if(nn >= 0 && nn < nMax) dst.setRGB(nn, m, palette[p1]);
		}
	}
	
	private void nextPix8(BufferReference src, BufferedImage dst, int m, int n, int nMax, boolean nflip) {
		if(!src.hasRemaining()) return;
		int bb = Byte.toUnsignedInt(src.nextByte());
		
		if(colsFirst) {
			dst.setRGB(m, n, palette[bb]);
		}
		else {
			dst.setRGB(n, m, palette[bb]);
		}
	}
	
	private void nextPix16(BufferReference src, BufferedImage dst, int m, int n, int nMax, boolean nflip) {
		int val = 0;
		for(int i = 0; i < 2; i++) {
			if(!src.hasRemaining()) break;
			val <<= 8;
			val |= Byte.toUnsignedInt(src.nextByte());
		}
		
		int argb = 0;
		if(clrCallback != null) {
			argb = clrCallback.toARGB(val);
		}
		else {
			argb = rgb555BE_to_ARGB_defo(val);
		}
		
		if(colsFirst) {
			dst.setRGB(m, n, argb);
		}
		else {
			dst.setRGB(n, m, argb);
		}
	}
	
	private void nextPix24(BufferReference src, BufferedImage dst, int m, int n, int nMax, boolean nflip) {
		int val = 0;
		for(int i = 0; i < 3; i++) {
			if(!src.hasRemaining()) break;
			val <<= 8;
			val |= Byte.toUnsignedInt(src.nextByte());
		}
		
		if(clrCallback != null) {
			val = clrCallback.toARGB(val);
		}
		
		if(colsFirst) {
			dst.setRGB(m, n, val);
		}
		else {
			dst.setRGB(n, m, val);
		}
	}
	
	private void nextPix32(BufferReference src, BufferedImage dst, int m, int n, int nMax, boolean nflip) {
		int val = 0;
		for(int i = 0; i < 4; i++) {
			if(!src.hasRemaining()) break;
			val <<= 8;
			val |= Byte.toUnsignedInt(src.nextByte());
		}
		
		if(clrCallback != null) {
			val = clrCallback.toARGB(val);
		}
		
		if(colsFirst) {
			dst.setRGB(m, n, val);
		}
		else {
			dst.setRGB(n, m, val);
		}
	}
	
	private BufferedImage parseTiledImageData(BufferReference data, int tileCols, int tileRows, int overhangX, int overhangY) {
		//TODO overhang (for when total height and width aren't exact multiples of file size)
		int pDim = tileColsFirst?tileCols:tileRows;
		int qDim = tileColsFirst?tileRows:tileCols;
		int ptDim = tileColsFirst?tileWidth:tileHeight;
		int qtDim = tileColsFirst?tileHeight:tileWidth;
		int mDim = colsFirst?tileWidth:tileHeight;
		int nDim = colsFirst?tileHeight:tileWidth;
		
		int fullWidth = tileCols * tileWidth;
		int fullHeight = tileRows * tileHeight;
		BufferedImage img = new BufferedImage(fullWidth, fullHeight, BufferedImage.TYPE_INT_ARGB);
		int nMax = colsFirst?fullHeight:fullWidth;
		
		boolean pflip = false;
		boolean qflip = false;
		if(tileColsFirst && rightOver) pflip = true;
		if(!tileColsFirst && bottomUp) pflip = true;
		if(tileColsFirst && bottomUp) qflip = true;
		if(!tileColsFirst && rightOver) qflip = true;
		
		boolean mflip = false;
		boolean nflip = false;
		if(colsFirst && rightOver) mflip = true;
		if(!colsFirst && bottomUp) mflip = true;
		if(colsFirst && bottomUp) nflip = true;
		if(!colsFirst && rightOver) nflip = true;
		
		int incrAmt = 1;
		if(bitDepth == 2) incrAmt = 4;
		if(bitDepth == 4) incrAmt = 2;
		
		int p = 0, q = 0; //Tile base
		int m = 0, n = 0; //Coord within tile
		if(pflip) p = (pDim * ptDim) - 1;
		for(int pp = 0; pp < pDim; pp++) {
			q = qflip?((qDim*qtDim)-1):0;
			for(int qq = 0; qq < qDim; qq++) {
				m = mflip?(mDim-1):0;
				for(int mm = 0; mm < mDim; mm++) {
					n = nflip?(nDim-1):0;
					for(int nn = 0; nn < nDim; nn+=incrAmt) {
						int mBase = m + p;
						int nBase = n + q;
						switch(bitDepth) {
						case 2:
							nextPix2(data, img, mBase, nBase, nMax, nflip);
							break;
						case 4:
							nextPix4(data, img, mBase, nBase, nMax, nflip);
							break;
						case 8:
							nextPix8(data, img, mBase, nBase, nMax, nflip);
							break;
						case 16:
							nextPix16(data, img, mBase, nBase, nMax, nflip);
							break;
						case 24:
							nextPix24(data, img, mBase, nBase, nMax, nflip);
							break;
						case 32:
							nextPix32(data, img, mBase, nBase, nMax, nflip);
							break;
						}
						
						if(nflip) n-=incrAmt;
						else n+=incrAmt;
					}
					if(mflip) m--;
					else m++;
				}
				if(qflip) q -= qtDim;
				else q += qtDim;
			}
			
			if(pflip) p -= ptDim;
			else p += ptDim;
		}
		
		
		return img;
	}
	
	private BufferedImage parseUntiledImageData(BufferReference data, int widthPix, int heightPix) {
		int mDim = colsFirst?widthPix:heightPix;
		int nDim = colsFirst?heightPix:widthPix;
		
		BufferedImage img = new BufferedImage(widthPix, heightPix, BufferedImage.TYPE_INT_ARGB);
		int m = 0, n = 0;
		boolean mflip = false;
		boolean nflip = false;
		if(colsFirst && rightOver) mflip = true;
		if(!colsFirst && bottomUp) mflip = true;
		if(colsFirst && bottomUp) nflip = true;
		if(!colsFirst && rightOver) nflip = true;
		
		int incrAmt = 1;
		if(bitDepth == 2) incrAmt = 4;
		if(bitDepth == 4) incrAmt = 2;

		if(mflip) m = mDim - 1;
		for(int i = 0; i < mDim; i++) {
			//Reset n
			n = nflip?(nDim-1):0;
			for(int j = 0; j < nDim; j += incrAmt) {
				switch(bitDepth) {
				case 2:
					nextPix2(data, img, m, n, nDim, nflip);
					break;
				case 4:
					nextPix4(data, img, m, n, nDim, nflip);
					break;
				case 8:
					nextPix8(data, img, m, n, nDim, nflip);
					break;
				case 16:
					nextPix16(data, img, m, n, nDim, nflip);
					break;
				case 24:
					nextPix24(data, img, m, n, nDim, nflip);
					break;
				case 32:
					nextPix32(data, img, m, n, nDim, nflip);
					break;
				}
				
				if(nflip)n -= incrAmt;
				else n += incrAmt;
			}
			if(mflip)m--;
			else m++;
		}
		
		return img;
	}
	
	//Parse method variants allow different inputs knowing different dimension information
	public BufferedImage parseImageData(BufferReference data, int width, int height) {
		//Gen default palette if one not provided.
		if(bitDepth < 16 && palette == null) {
			switch(bitDepth) {
			case 2: palette = ImageUtils.getDefaultPalette2(); break;
			case 4: palette = ImageUtils.getDefaultPalette4(); break;
			case 8: palette = ImageUtils.getDefaultPalette8(); break;
			}
		}
		
		BufferedImage result = null;
		if(tileWidth > 0 && tileHeight > 0) {
			int tCols = width/tileWidth;
			int tRows = height/tileHeight;
			int overX = width - (tCols * tileWidth);
			int overY = height - (tRows * tileHeight);
			result = parseTiledImageData(data, tCols, tRows, overX, overY);
		}
		else {
			result = parseUntiledImageData(data, width, height);
		}
		
		return result;
	}
	
	public BufferedImage parseImageData(BufferReference data, int dataSize) {
		//Gen default palette if one not provided.
		if(bitDepth < 16 && palette == null) {
			switch(bitDepth) {
			case 2: palette = ImageUtils.getDefaultPalette2(); break;
			case 4: palette = ImageUtils.getDefaultPalette4(); break;
			case 8: palette = ImageUtils.getDefaultPalette8(); break;
			}
		}
		
		int pixcount = 0;
		switch(bitDepth) {
		case 2: pixcount = dataSize << 2; break;
		case 4: pixcount = dataSize << 1; break;
		case 8: pixcount = dataSize; break;
		case 16: pixcount = dataSize >>> 1; break;
		case 24: pixcount = dataSize/3; break;
		case 32: pixcount = dataSize >>> 2; break;
		}
		
		BufferedImage result = null;
		if(tileWidth > 0 && tileHeight > 0) {
			int tArea = tileHeight * tileWidth;
			int tCount = pixcount / tArea;
			int tCols = tColSuggest;
			if(tCols > tCount) tCols = tCount;
			int tRows = tCount/tCols;
			result = parseTiledImageData(data, tCols, tRows, 0, 0);
		}
		else {
			int heightSuggest = pixcount/widthSuggest;
			result = parseUntiledImageData(data, widthSuggest, heightSuggest);
		}
		
		return result;
	}

	
}
