package waffleoRai_Image;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

public class ImageUtils {
	
	public static final int ALPHA_MODE_IGNORE = 0;
	public static final int ALPHA_MODE_8BITFULL = 1;
	public static final int ALPHA_MODE_FLAG = 2;
	
	public static int[] getDefaultPalette2() {
		int[] plt = new int[4];
		for(int i = 0; i < 4; i++) {
			int val = 0;
			for(int j = 0; j < 12; j++) {
				val <<= 2;
				val |= (i & 0x3);
			}
			val |= 0xff000000;
			plt[i] = val;
		}
		
		return plt;
	}

	public static int[] getDefaultPalette4() {
		int[] plt = new int[16];
		for(int i = 0; i < 16; i++) {
			int val = 0;
			for(int j = 0; j < 6; j++) {
				val <<= 4;
				val |= (i & 0xf);
			}
			val |= 0xff000000;
			plt[i] = val;
		}
		
		return plt;
	}
	
	public static int[] getDefaultPalette8() {
		int[] plt = new int[256];
		for(int i = 0; i < 256; i++) {
			int val = 0;
			for(int j = 0; j < 3; j++) {
				val <<= 8;
				val |= (i & 0xff);
			}
			val |= 0xff000000;
			plt[i] = val;
		}
		
		return plt;
	}
	
	public static int[] simplifyPalette(Palette plt) {
		if(plt == null) return null;
		int[] out = new int[1 << plt.getBitDepth()];
		for(int i = 0; i < out.length; i++) {
			int val = plt.getRGBA(i);
			int alpha = val & 0xff;
			val >>>= 8;
			val |= alpha << 24;
			out[i] = val;
		}
		return out;
	}
	
	public static int scale5BitColor(int val5){
		if (val5 < 0 || val5 >= 32) return val5;
		if (val5 == 0) return 0;
		if (val5 == 1) return 8;
		if (val5 == 2) return 16;
		if (val5 == 31) return 255;
		double l2 = Math.log10((double)val5 / 2.0) / Math.log10(2.0);
		l2 *= 8.0;
		//Now scale to 255
		double factor = 255.0/32.0;
		double temp = factor * l2;
		return (int)Math.round(temp);
	}
	
	public static int scale8Bit_to_5Bit(int eight){
		//I have no idea if this is updated...
		if (eight == 0) return 0;
		if (eight < 37) return 1;
		if (eight == 255) return 31;
		double PC = ((double)eight * 32.0) / 255.0;
		PC /= 8.0;
		PC = Math.pow(2.0, PC);
		PC *= 2;
		return (int)Math.round(PC);
	}
	
	public static int calculateRGBDistanceSquared(int rgb1, int rgb2) {
		//Alpha is ignored
		int r1 = (rgb1 >>> 16) & 0xff;
		int r2 = (rgb2 >>> 16) & 0xff;
		int rdist = r1 - r2;
		rdist *= rdist;
		
		int g1 = (rgb1 >>> 8) & 0xff;
		int g2 = (rgb2 >>> 8) & 0xff;
		int gdist = g1 - g2;
		gdist *= gdist;
		
		int b1 = rgb1 & 0xff;
		int b2 = rgb2 & 0xff;
		int bdist = b1 - b2;
		bdist *= bdist;
		
		return rdist + gdist + bdist;
	}
	
	public static double calculateRGBDistance(int rgb1, int rgb2) {
		int sq = calculateRGBDistanceSquared(rgb1, rgb2);
		return Math.sqrt((double)sq);
	}
	
	public static int calculateRGBADistanceSquared(int argb1, int argb2) {
		int a1 = (argb1 >>> 24) & 0xff;
		int a2 = (argb2 >>> 24) & 0xff;
		int adist = a1 - a2;
		adist *= adist;
		
		int r1 = (argb1 >>> 16) & 0xff;
		int r2 = (argb2 >>> 16) & 0xff;
		int rdist = r1 - r2;
		rdist *= rdist;
		
		int g1 = (argb1 >>> 8) & 0xff;
		int g2 = (argb2 >>> 8) & 0xff;
		int gdist = g1 - g2;
		gdist *= gdist;
		
		int b1 = argb1 & 0xff;
		int b2 = argb2 & 0xff;
		int bdist = b1 - b2;
		bdist *= bdist;
		
		return rdist + gdist + bdist;
	}
	
	public static double calculateRGBADistance(int argb1, int argb2) {
		int sq = calculateRGBADistanceSquared(argb1, argb2);
		return Math.sqrt((double)sq);
	}
	
	public static int getNearestPaletteColor(int color, int[] palette, int alphaMode) {
		return getNearestPaletteColor(color, palette, alphaMode, 0x7f);
	}
	
	public static int getNearestPaletteColor(int color, int[] palette, int alphaMode, int alphaThreshold) {
		if(palette == null) return -1;
		
		//Returns palette index of nearest color
		int minVal = Integer.MAX_VALUE;
		int minIdx = -1;
		
		if(alphaMode == ImageUtils.ALPHA_MODE_8BITFULL) {
			for(int i = 0; i < palette.length; i++) {
				int dist = calculateRGBADistanceSquared(color, palette[i]);
				if(dist < minVal) {
					minVal = dist;
					minIdx = i;
				}
			}
		}
		else if(alphaMode == ImageUtils.ALPHA_MODE_IGNORE){
			for(int i = 0; i < palette.length; i++) {
				int dist = calculateRGBDistanceSquared(color, palette[i]);
				if(dist < minVal) {
					minVal = dist;
					minIdx = i;
				}
			}
		}
		else if(alphaMode == ImageUtils.ALPHA_MODE_FLAG){
			int alpha = (color >>> 24);
			if(alpha > alphaThreshold) {
				for(int i = 0; i < palette.length; i++) {
					int pa = (palette[i] >>> 24);
					if(pa <= alphaThreshold) continue;
					int dist = calculateRGBDistanceSquared(color, palette[i]);
					if(dist < minVal) {
						minVal = dist;
						minIdx = i;
					}
				}	
			}
			else {
				for(int i = (palette.length - 1); i >= 0; i--) {
					int pa = (palette[i] >>> 24);
					if(pa <= alphaThreshold) {
						minIdx = i;
						break;
					}
				}
			}
		}
		
		return minIdx;
	}
	
	public static int[] getColorDistanceVector(int argb1, int argb2) {
		int[] argb_vec = new int[4];
		
		int a1 = (argb1 >>> 24) & 0xff;
		int a2 = (argb2 >>> 24) & 0xff;
		argb_vec[0] = a2 - a1;
		
		int r1 = (argb1 >>> 16) & 0xff;
		int r2 = (argb2 >>> 16) & 0xff;
		argb_vec[1] = r2 - r1;
		
		int g1 = (argb1 >>> 8) & 0xff;
		int g2 = (argb2 >>> 8) & 0xff;
		argb_vec[2] = g2 - g1;
		
		int b1 = argb1 & 0xff;
		int b2 = argb2 & 0xff;
		argb_vec[3] = b2 - b1;
		return argb_vec;
	}
	
	public static int[] argb2Vector(int argb) {
		int[] argb_vec = new int[4];
		argb_vec[0] = (argb >>> 24) & 0xff;
		argb_vec[1] = (argb >>> 16) & 0xff;
		argb_vec[2] = (argb >>> 8) & 0xff;
		argb_vec[3] = argb & 0xff;
		return argb_vec;
	}
	
	public static int vector2ARGB(int[] argb_vec) {
		if(argb_vec == null) return 0;
		if(argb_vec.length < 4) return 0;
		
		int argb = 0;
		for(int i = 0; i < 4; i++) {
			argb <<= 8;
			if(argb_vec[i] < 0) argb_vec[i] = 0;
			if(argb_vec[i] > 255) argb_vec[i] = 255;
			argb |= argb_vec[i];
		}
		
		return argb;
	}
	
	public static int[] generatePalette4(BufferedImage input, int alphaMode) {
		PaletteGen pg = new PaletteGen(4, alphaMode);
		pg.processImage(input);
		return pg.generatePalette();
	}
	
	public static int[] generatePalette8(BufferedImage input, int alphaMode) {
		PaletteGen pg = new PaletteGen(8, alphaMode);
		pg.processImage(input);
		return pg.generatePalette();
	}
	
	public static void setUniformTransparency(BufferedImage input, int defoTransColor, int alphaThreshold) {
		int w = input.getWidth();
		int h = input.getHeight();
		for(int y = 0; y < h; y++) {
			for(int x = 0; x < w; x++) {
				int p = input.getRGB(x, y);
				int a = (p >>> 24) & 0xff;
				if(a <= alphaThreshold) {
					input.setRGB(x, y, defoTransColor);
				}
			}
		}
	}
	
	public static void setUniformTransparency(BufferedImage input, int defoTransColor) {
		int w = input.getWidth();
		int h = input.getHeight();
		for(int y = 0; y < h; y++) {
			for(int x = 0; x < w; x++) {
				int p = input.getRGB(x, y);
				int a = (p >>> 24) & 0xff;
				if(a == 0x00) {
					input.setRGB(x, y, defoTransColor);
				}
			}
		}
	}
	
	public static BufferedImage rescaleImage(BufferedImage input, int w, int h, int algo) {
		Image simg = input.getScaledInstance(w, h, algo);
		BufferedImage output = new BufferedImage(simg.getWidth(null), simg.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = output.createGraphics();
		g2.drawImage(simg, 0, 0, null);
		g2.dispose();
		return output;
	}
	
	public static int detectLeftAlphaTrim(BufferedImage input, int alphaThreshold) {
		if(input == null) return 0;
		int w = input.getWidth();
		int h = input.getHeight();
		
		//Pixels from left that are all transparent according to alpha threshold
		int n = 0;
		for(int x = 0; x < w; x++) {
			boolean colTrans = true;
			for(int y = 0; y < h; y++) {
				int p = input.getRGB(x, y);
				int a = p >>> 24;
				if(a > alphaThreshold) {
					colTrans = false;
					break;
				}
			}
			if(colTrans) n++;
			else break;
		}
		
		return n;
	}
	
	public static int detectRightAlphaTrim(BufferedImage input, int alphaThreshold) {
		if(input == null) return 0;
		int w = input.getWidth();
		int h = input.getHeight();
		
		int n = 0;
		for(int x = (w-1); x >= 0; x--) {
			boolean colTrans = true;
			for(int y = 0; y < h; y++) {
				int p = input.getRGB(x, y);
				int a = p >>> 24;
				if(a > alphaThreshold) {
					colTrans = false;
					break;
				}
			}
			if(colTrans) n++;
			else break;
		}
		
		return n;
	}
	
	public static int detectTopAlphaTrim(BufferedImage input, int alphaThreshold) {
		if(input == null) return 0;
		int w = input.getWidth();
		int h = input.getHeight();
		
		int n = 0;
		for(int y = 0; y < h; y++) {
			boolean rowTrans = true;
			for(int x = 0; x < w; x++) {
				int p = input.getRGB(x, y);
				int a = p >>> 24;
				if(a > alphaThreshold) {
					rowTrans = false;
					break;
				}
			}
			if(rowTrans) n++;
			else break;
		}
		
		return n;
	}
	
	public static int detectBottomAlphaTrim(BufferedImage input, int alphaThreshold) {
		if(input == null) return 0;
		int w = input.getWidth();
		int h = input.getHeight();
		
		int n = 0;
		for(int y = (h-1); y >= 0; y--) {
			boolean rowTrans = true;
			for(int x = 0; x < w; x++) {
				int p = input.getRGB(x, y);
				int a = p >>> 24;
				if(a > alphaThreshold) {
					rowTrans = false;
					break;
				}
			}
			if(rowTrans) n++;
			else break;
		}
		
		return n;
	}
	
}
