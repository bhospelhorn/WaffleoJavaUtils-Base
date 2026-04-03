package waffleoRai_Image;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

public class ImageDither {
	
	public static final int[][] BAYER_MTX_2 = {{0,2},{3,1}};
	public static final int[][] BAYER_MTX_4 = {{0,8,2,10},{12,4,14,6},{3,11,1,9},{15,7,13,5}};
	public static final int[][] BAYER_MTX_8 = {
												{ 0, 32,  8, 40,  2, 34, 10, 42},
												{48, 16, 56, 24, 50, 18, 58, 26},
												{12, 44,  4, 36, 14, 46,  6, 38},
												{60, 28, 52, 20, 62, 30, 54, 22},
												{ 3, 35, 11, 43,  1, 33,  9, 41},
												{51, 19, 59, 27, 49, 17, 57, 25},
												{15, 47,  7, 39, 13, 45,  5, 37},
												{63, 31, 55, 23, 61, 29, 53, 21}};
	
	public static final int DITHER_ALGO_FLOYD_STEINBERG = 0;
	public static final int DITHER_ALGO_ATKINSON = 1;
	public static final int DITHER_ALGO_BURKES = 2;
	public static final int DITHER_ALGO_SIERRA = 3;
	public static final int DITHER_ALGO_BAYER2 = 4;
	public static final int DITHER_ALGO_BAYER4 = 5;
	public static final int DITHER_ALGO_BAYER8 = 6;
	
	private static int addColor(int base, int[] diffVec, int multiplier, int shamt, boolean includeAlpha) {
		int[] baseVec = ImageUtils.argb2Vector(base);
		int i0 = includeAlpha ? 0 : 1;
		for(int i = i0; i < 4; i++) {
			baseVec[i] += (diffVec[i] * multiplier) >> shamt;
		}
		
		return ImageUtils.vector2ARGB(baseVec);
	}
	
	private static int[][] ditherRGBFloydSteinberg(BufferedImage input, int[] palette, int alphaMode, int alphaThreshold){
		int w = input.getWidth();
		int h = input.getHeight();
		int[][] err = new int[h][w];
		int[][] output = new int[h][w];
		boolean addAlpha = (alphaMode == ImageUtils.ALPHA_MODE_8BITFULL);
		
		//Copy to err
		for(int y = 0; y < h; y++) {
			for(int x = 0; x < w; x++) {
				err[y][x] = input.getRGB(x, y);
			}
		}
		
		for(int y = 0; y < h; y++) {
			for(int x = 0; x < w; x++) {
				output[y][x] = ImageUtils.getNearestPaletteColor(err[y][x], palette, alphaMode, alphaThreshold);
				int oldColor = err[y][x];
				int newColor = palette[output[y][x]];
				int[] distvec = ImageUtils.getColorDistanceVector(newColor, oldColor);
				
				if(x < (w-1)) {
					err[y][x+1] = addColor(err[y][x+1], distvec, 7, 4, addAlpha);
				}
				if(y < (h-1)) {
					err[y+1][x] = addColor(err[y+1][x], distvec, 5, 4, addAlpha);
					if(x < (w-1)) {
						err[y+1][x+1] = addColor(err[y+1][x+1], distvec, 1, 4, addAlpha);
					}
					if(x > 0) {
						err[y+1][x-1] = addColor(err[y+1][x-1], distvec, 3, 4, addAlpha);
					}
				}
			}
		}
		
		return output;
	}
	
	private static int[][] ditherRGBAtkinson(BufferedImage input, int[] palette, int alphaMode, int alphaThreshold){
		int w = input.getWidth();
		int h = input.getHeight();
		int[][] err = new int[h][w];
		int[][] output = new int[h][w];
		boolean addAlpha = (alphaMode == ImageUtils.ALPHA_MODE_8BITFULL);
		
		//Copy to err
		for(int y = 0; y < h; y++) {
			for(int x = 0; x < w; x++) {
				err[y][x] = input.getRGB(x, y);
			}
		}
		
		for(int y = 0; y < h; y++) {
			for(int x = 0; x < w; x++) {
				output[y][x] = ImageUtils.getNearestPaletteColor(err[y][x], palette, alphaMode, alphaThreshold);
				int oldColor = err[y][x];
				int newColor = palette[output[y][x]];
				int[] distvec = ImageUtils.getColorDistanceVector(newColor, oldColor);
				
				if(x < (w-1)) {
					err[y][x+1] = addColor(err[y][x+1], distvec, 1, 3, addAlpha);
				}
				if(x < (w-2)) {
					err[y][x+2] = addColor(err[y][x+2], distvec, 1, 3, addAlpha);
				}
				if(y < (h-1)) {
					err[y+1][x] = addColor(err[y+1][x], distvec, 1, 3, addAlpha);
					if(x < (w-1)) {
						err[y+1][x+1] = addColor(err[y+1][x+1], distvec, 1, 3, addAlpha);
					}
					if(x > 0) {
						err[y+1][x-1] = addColor(err[y+1][x-1], distvec, 1, 3, addAlpha);
					}
				}
				if(y < (h-2)) {
					err[y+2][x] = addColor(err[y+2][x], distvec, 1, 3, addAlpha);
				}
			}
		}
		
		return output;
	}
	
	private static int[][] ditherRGBBurkes(BufferedImage input, int[] palette, int alphaMode, int alphaThreshold){
		int w = input.getWidth();
		int h = input.getHeight();
		int[][] err = new int[h][w];
		int[][] output = new int[h][w];
		boolean addAlpha = (alphaMode == ImageUtils.ALPHA_MODE_8BITFULL);
		
		//Copy to err
		for(int y = 0; y < h; y++) {
			for(int x = 0; x < w; x++) {
				err[y][x] = input.getRGB(x, y);
			}
		}
		
		for(int y = 0; y < h; y++) {
			for(int x = 0; x < w; x++) {
				output[y][x] = ImageUtils.getNearestPaletteColor(err[y][x], palette, alphaMode, alphaThreshold);
				int oldColor = err[y][x];
				int newColor = palette[output[y][x]];
				int[] distvec = ImageUtils.getColorDistanceVector(newColor, oldColor);
				
				if(x < (w-1)) {
					err[y][x+1] = addColor(err[y][x+1], distvec, 8, 5, addAlpha);
				}
				if(x < (w-2)) {
					err[y][x+2] = addColor(err[y][x+2], distvec, 4, 5, addAlpha);
				}
				if(y < (h-1)) {
					err[y+1][x] = addColor(err[y+1][x], distvec, 8, 5, addAlpha);
					if(x < (w-1)) {
						err[y+1][x+1] = addColor(err[y+1][x+1], distvec, 4, 5, addAlpha);
					}
					if(x < (w-2)) {
						err[y+1][x+2] = addColor(err[y+1][x+2], distvec, 2, 5, addAlpha);
					}
					if(x > 0) {
						err[y+1][x-1] = addColor(err[y+1][x-1], distvec, 4, 5, addAlpha);
					}
					if(x > 1) {
						err[y+1][x-2] = addColor(err[y+1][x-2], distvec, 2, 5, addAlpha);
					}
				}
			}
		}
		
		return output;
	}

	private static int[][] ditherRGBSierra(BufferedImage input, int[] palette, int alphaMode, int alphaThreshold){
		int w = input.getWidth();
		int h = input.getHeight();
		int[][] err = new int[h][w];
		int[][] output = new int[h][w];
		boolean addAlpha = (alphaMode == ImageUtils.ALPHA_MODE_8BITFULL);
		
		//Copy to err
		for(int y = 0; y < h; y++) {
			for(int x = 0; x < w; x++) {
				err[y][x] = input.getRGB(x, y);
			}
		}
		
		for(int y = 0; y < h; y++) {
			for(int x = 0; x < w; x++) {
				output[y][x] = ImageUtils.getNearestPaletteColor(err[y][x], palette, alphaMode, alphaThreshold);
				int oldColor = err[y][x];
				int newColor = palette[output[y][x]];
				int[] distvec = ImageUtils.getColorDistanceVector(newColor, oldColor);
				
				if(x < (w-1)) {
					err[y][x+1] = addColor(err[y][x+1], distvec, 5, 5, addAlpha);
				}
				if(x < (w-2)) {
					err[y][x+2] = addColor(err[y][x+2], distvec, 3, 5, addAlpha);
				}
				if(y < (h-1)) {
					err[y+1][x] = addColor(err[y+1][x], distvec, 5, 5, addAlpha);
					if(x < (w-1)) {
						err[y+1][x+1] = addColor(err[y+1][x+1], distvec, 4, 5, addAlpha);
					}
					if(x < (w-2)) {
						err[y+1][x+2] = addColor(err[y+1][x+2], distvec, 2, 5, addAlpha);
					}
					if(x > 0) {
						err[y+1][x-1] = addColor(err[y+1][x-1], distvec, 4, 5, addAlpha);
					}
					if(x > 1) {
						err[y+1][x-2] = addColor(err[y+1][x-2], distvec, 2, 5, addAlpha);
					}
				}
				if(y < (h-2)) {
					err[y+2][x] = addColor(err[y+2][x], distvec, 3, 5, addAlpha);
					if(x < (w-1)) {
						err[y+2][x+1] = addColor(err[y+2][x+1], distvec, 2, 5, addAlpha);
					}
					if(x > 0) {
						err[y+2][x-1] = addColor(err[y+2][x-1], distvec, 2, 5, addAlpha);
					}
				}
			}
		}
		
		return output;
	}
	
	private static int[][] ditherRGBBayer2(BufferedImage input, int[] palette, int alphaMode, int alphaThreshold){
		int w = input.getWidth();
		int h = input.getHeight();
		int[][] output = new int[h][w];
		
		for(int y = 0; y < h; y++) {
			for(int x = 0; x < w; x++) {
				int xi = x & 0x1;
				int yi = y & 0x1;
				int bf = BAYER_MTX_2[yi][xi];
				int p = input.getRGB(x, y);
				
				double addend = (double)bf - 0.5;
				addend /= 4.0;
				int addendi = (int)Math.round(addend);
				
				int[] baseVec = ImageUtils.argb2Vector(p);
				for(int i = 1; i < 4; i++) {
					baseVec[i] += addendi;
				}
				switch(alphaMode) {
				case ImageUtils.ALPHA_MODE_8BITFULL:
					baseVec[0] += addendi;
					break;
				case ImageUtils.ALPHA_MODE_FLAG:
					if(baseVec[0] < alphaThreshold) baseVec[0] = 0;
					else baseVec[0] = 0xff;
					break;
				case ImageUtils.ALPHA_MODE_IGNORE:
					baseVec[0] = 0xff;
					break;
				}
				
				p = ImageUtils.vector2ARGB(baseVec);
				output[y][x] = ImageUtils.getNearestPaletteColor(p, palette, alphaMode);
			}
		}
		
		return output;
	}

	private static int[][] ditherRGBBayer4(BufferedImage input, int[] palette, int alphaMode, int alphaThreshold){
		int w = input.getWidth();
		int h = input.getHeight();
		int[][] output = new int[h][w];
		
		for(int y = 0; y < h; y++) {
			for(int x = 0; x < w; x++) {
				int xi = x & 0x3;
				int yi = y & 0x3;
				int bf = BAYER_MTX_4[yi][xi];
				int p = input.getRGB(x, y);
				
				double addend = (double)bf - 0.5;
				addend /= 16.0;
				int addendi = (int)Math.round(addend);
				
				int[] baseVec = ImageUtils.argb2Vector(p);
				for(int i = 1; i < 4; i++) {
					baseVec[i] += addendi;
				}
				switch(alphaMode) {
				case ImageUtils.ALPHA_MODE_8BITFULL:
					baseVec[0] += addendi;
					break;
				case ImageUtils.ALPHA_MODE_FLAG:
					if(baseVec[0] < alphaThreshold) baseVec[0] = 0;
					else baseVec[0] = 0xff;
					break;
				case ImageUtils.ALPHA_MODE_IGNORE:
					baseVec[0] = 0xff;
					break;
				}
				p = ImageUtils.vector2ARGB(baseVec);
				output[y][x] = ImageUtils.getNearestPaletteColor(p, palette, alphaMode);
			}
		}
		
		return output;
	}
	
	private static int[][] ditherRGBBayer8(BufferedImage input, int[] palette, int alphaMode, int alphaThreshold){
		int w = input.getWidth();
		int h = input.getHeight();
		int[][] output = new int[h][w];
		
		for(int y = 0; y < h; y++) {
			for(int x = 0; x < w; x++) {
				int xi = x & 0x7;
				int yi = y & 0x7;
				int bf = BAYER_MTX_8[yi][xi];
				int p = input.getRGB(x, y);
				
				double addend = (double)bf - 0.5;
				addend /= 64.0;
				int addendi = (int)Math.round(addend);
				
				int[] baseVec = ImageUtils.argb2Vector(p);
				for(int i = 1; i < 4; i++) {
					baseVec[i] += addendi;
				}
				switch(alphaMode) {
				case ImageUtils.ALPHA_MODE_8BITFULL:
					baseVec[0] += addendi;
					break;
				case ImageUtils.ALPHA_MODE_FLAG:
					if(baseVec[0] < alphaThreshold) baseVec[0] = 0;
					else baseVec[0] = 0xff;
					break;
				case ImageUtils.ALPHA_MODE_IGNORE:
					baseVec[0] = 0xff;
					break;
				}
				p = ImageUtils.vector2ARGB(baseVec);
				output[y][x] = ImageUtils.getNearestPaletteColor(p, palette, alphaMode);
			}
		}
		
		return output;
	}

	public static int[][] quantizeRGBWithDither(BufferedImage input, int[] palette, int algo, int alphaMode){
		return quantizeRGBWithDither(input, palette, algo, alphaMode, 0x7f);
	}
	
	public static int[][] quantizeRGBWithDither(BufferedImage input, int[] palette, int algo, int alphaMode, int alphaThreshold){
		switch(algo) {
		case DITHER_ALGO_FLOYD_STEINBERG:
			return ditherRGBFloydSteinberg(input, palette, alphaMode, alphaThreshold);
		case DITHER_ALGO_ATKINSON:
			return ditherRGBAtkinson(input, palette, alphaMode, alphaThreshold);
		case DITHER_ALGO_BURKES:
			return ditherRGBBurkes(input, palette, alphaMode, alphaThreshold);
		case DITHER_ALGO_SIERRA:
			return ditherRGBSierra(input, palette, alphaMode, alphaThreshold);
		case DITHER_ALGO_BAYER2:
			return ditherRGBBayer2(input, palette, alphaMode, alphaThreshold);
		case DITHER_ALGO_BAYER4:
			return ditherRGBBayer4(input, palette, alphaMode, alphaThreshold);
		case DITHER_ALGO_BAYER8:
			return ditherRGBBayer8(input, palette, alphaMode, alphaThreshold);
		}
		
		return null;
	}
	
	public static void main(String[] args) {
		//Test
		
		String inpath = args[0];
		String outpath = args[1];
		
		final int ALGO = DITHER_ALGO_FLOYD_STEINBERG;
		final int ALPHA_MODE = ImageUtils.ALPHA_MODE_FLAG;
		final int ALPHA_TH = 0x7f;
		
		try {
			BufferedImage img = ImageIO.read(new File(inpath));
			ImageUtils.setUniformTransparency(img, 0x00ffffff, ALPHA_TH);
			System.out.println("Generating palette...");
			int[] plt = ImageUtils.generatePalette8(img, ALPHA_MODE);

			System.out.println("Downscaling...");
			int w = img.getWidth();
			int h = img.getHeight();
			double aspectRatio = (double)w/(double)h;
			int htarg = 240;
			int wtarg = (int)Math.round(aspectRatio * (double)htarg);
			img = ImageUtils.rescaleImage(img, wtarg, htarg, BufferedImage.SCALE_DEFAULT);
			
			System.out.println("Quantizing bitmap...");
			int[][] bitmap = quantizeRGBWithDither(img, plt, ALGO, ALPHA_MODE, ALPHA_TH);
			System.out.println("Writing file...");
			BmpFile.writeRawToBMP(bitmap, plt, outpath);
		}
		catch(Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
		
		
	}
	
}
