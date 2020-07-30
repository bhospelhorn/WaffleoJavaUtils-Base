package waffleoRai_Image.files;

import java.awt.image.BufferedImage;
import java.io.IOException;

import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.FileNode;

public class TGAFile {
	//https://en.wikipedia.org/wiki/Truevision_TGA
	//http://www.dca.fee.unicamp.br/~martino/disciplinas/ea978/tgaffs.pdf
	//Little-Endian

	/*----- Constants -----*/
	
	public static final int IMG_TYPE_NONE = 0;
	public static final int IMG_TYPE_PLT = 1;
	public static final int IMG_TYPE_TRUECOLOR = 2;
	public static final int IMG_TYPE_BW = 3;
	public static final int IMG_TYPE_PLT_RLE = 9;
	public static final int IMG_TYPE_TRUECOLOR_RLE = 10;
	public static final int IMG_TYPE_BW_RLE = 11;
	
	/*----- Instance Variables -----*/
	
	private int image_type;
	
	private int[] palette; //If present
	private int[][] bitmap;
	
	private int img_x;
	private int img_y;
	
	/*----- Construction/Parsing -----*/
	
	private TGAFile(){}
	
	public static TGAFile readTGA(String path) throws IOException{
		FileBuffer file = FileBuffer.createBuffer(path, false);
		return readTGA(file);
	}
	
	public static TGAFile readTGA(FileNode fn) throws IOException{
		return readTGA(fn.loadDecompressedData());
	}
	
	public static TGAFile readTGA(FileBuffer file) throws IOException{
		file.setEndian(false);
		TGAFile tga = new TGAFile();
		
		//System.err.println("checkpoint 1");
		file.setCurrentPosition(0L);
		int idlen = Byte.toUnsignedInt(file.nextByte());
		int cmtype = Byte.toUnsignedInt(file.nextByte());
		int imgtype = Byte.toUnsignedInt(file.nextByte());
		
		int cm_first_idx = Short.toUnsignedInt(file.nextShort());
		int cmlen = Short.toUnsignedInt(file.nextShort());
		int cm_esize = Byte.toUnsignedInt(file.nextByte());
		//System.err.println("checkpoint 2");
		
		tga.img_x = Short.toUnsignedInt(file.nextShort());
		tga.img_y = Short.toUnsignedInt(file.nextShort());
		int width = Short.toUnsignedInt(file.nextShort());
		int height = Short.toUnsignedInt(file.nextShort());
		int bitdepth = Byte.toUnsignedInt(file.nextByte());
		int desc = Byte.toUnsignedInt(file.nextByte());
		int alpha_depth = desc & 0x4;
		boolean flip_x = (desc & 0x10) != 0;
		boolean flip_y = (desc & 0x20) != 0;
		//System.err.println("checkpoint 3");
		
		//Skip id field if present
		file.skipBytes(idlen);
		
		if(cmtype != 0) tga.readColorMap(file, cm_first_idx, cmlen, cm_esize);
		tga.bitmap = new int[width][height];
		tga.readBitmap(file, imgtype, bitdepth, alpha_depth, flip_x, flip_y);
		
		return tga;
	}
	
	private void readColorMap(FileBuffer file, int first_idx, int len, int bd){

		int ecount = len/bd;
		int ecount_2 = first_idx + ecount;
		
		palette = new int[ecount_2];
		for(int i = 0; i < ecount; i++){
			int argb = 0;
			
			if(bd == 16){
				short val = file.nextShort();
				argb = RGB565_to_ARGB(val);
			}
			else if (bd == 24){
				argb = file.nextShortish();
				argb |= 0xFF000000;
			}
			else if(bd == 32){
				argb = file.nextInt();
			}
			
			palette[first_idx + i] = argb;
		}
		
	}
	
	private void readBitmap(FileBuffer file, int imgtype, int bd, int ad, boolean flipx, boolean flipy){

		if((imgtype & 0x8) != 0) file = decompRLE(file);
		image_type = imgtype & 0x7; //So can look at raw type.
		
		int h = bitmap[0].length;
		int w = bitmap.length;
		
		int x = flipx?(w-1):0;
		int y = flipy?0:(h-1);
		int x_inc = flipx?-1:1;
		int y_inc = flipy?1:-1;
		
		int pixcount = h * w;
		int p = 0;
		while(p < pixcount){
			switch(bd){
			case 1:
				int val1 = Byte.toUnsignedInt(file.nextByte());
				int shift1 = 0;
				for(int i = 0; i < 8; i++){
					int mask = 1 << shift1;
					bitmap[x][y] = (val1 & mask) >>> shift1;
					shift1++;
					
					x += x_inc;
					if(x < 0){x = w-1; y += y_inc;}
					else if (x >= w){x = 0; y += y_inc;}
				}
				p+=8;
				break;
			case 2:
				int val2 = Byte.toUnsignedInt(file.nextByte());
				int shift2 = 0;
				for(int i = 0; i < 4; i++){
					int mask = 0x3 << shift2;
					bitmap[x][y] = (val2 & mask) >>> shift2;
					shift2+=2;
					
					x += x_inc;
					if(x < 0){x = w-1; y += y_inc;}
					else if (x >= w){x = 0; y += y_inc;}
				}
				p+=4;
				break;
			case 4:
				int val4 = Byte.toUnsignedInt(file.nextByte());
				bitmap[x][y] = val4 & 0xF; x += x_inc;
				bitmap[x][y] = (val4 >>> 4) & 0xF;
				
				if(x < 0){x = w-1; y += y_inc;}
				else if (x >= w){x = 0; y += y_inc;}
				p+=2;
				break;
			case 8:
				bitmap[x][y] = Byte.toUnsignedInt(file.nextByte());
				x += x_inc;
				if(x < 0){x = w-1; y += y_inc;}
				else if (x >= w){x = 0; y += y_inc;}
				p++;
				break;
			case 16:
				short val16 = file.nextShort();
				int argb16 = 0;
				if(ad > 0) argb16 = ARGB1555_to_ARGB(val16);
				else argb16 = RGB565_to_ARGB(val16);
				
				bitmap[x][y] = argb16;
				x += x_inc;
				if(x < 0){x = w-1; y += y_inc;}
				else if (x >= w){x = 0; y += y_inc;}
				p++;
				break;
			case 24:
				int argb24 = file.nextShortish();
				argb24 |= 0xFF;
				bitmap[x][y] = argb24;
				x += x_inc;
				if(x < 0){x = w-1; y += y_inc;}
				else if (x >= w){x = 0; y += y_inc;}
				p++;
				break;
			case 32:
				//System.err.println("Pixel at " + x + "," + y);
				int argb32 = file.nextInt();
				bitmap[x][y] = argb32;
				x += x_inc;
				if(x < 0){x = w-1; y += y_inc;}
				else if (x >= w){x = 0; y += y_inc;}
				p++;
				break;
			}
		}
		
	}
	
	private FileBuffer decompRLE(FileBuffer in){
		//TODO
		return null;
	}
	
	/*----- Colors -----*/
	
	public static int ARGB1555_to_ARGB(short in){
		int val = Short.toUnsignedInt(in);
		int a = 255;
		if((val & 0x8000) != 0) a = 0;
		
		int r = upscale5((val >>> 10) & 0x1F);
		int g = upscale5((val >>> 5) & 0x1F);
		int b = upscale5(val & 0x1F);
		
		return (a << 24) | (r << 16) | (g << 8) | b;
	}
	
	public static int RGB565_to_ARGB(short in){
		int val = Short.toUnsignedInt(in);
		int a = 255;

		int r = upscale5((val >>> 11) & 0x1F);
		int g = upscale6((val >>> 5) & 0x3F);
		int b = upscale5(val & 0x1F);
		
		return (a << 24) | (r << 16) | (g << 8) | b;
	}
	
	public static int upscale5(int val)
	{
		//PSX to PC Scaling formula: y = 8* log2(x/2)
		if (val < 0 || val >= 32) return val;
		if (val == 0) return 0;
		if (val == 1) return 8;
		if (val == 2) return 16;
		if (val == 31) return 255;
		double l2 = Math.log10((double)val / 2.0) / Math.log10(2.0);
		l2 *= 8.0;
		//Now scale to 255
		double factor = 256.0/32.0;
		double temp = factor * l2;
		return (int)Math.round(temp);
	}
	
	public static int upscale6(int val)
	{
		//Equation: y = 16 * log2(x/4)
		if (val < 0 || val >= 64) return val;
		if (val == 0) return 0;
		if (val == 1) return 4;
		if (val == 2) return 8;
		if (val == 3) return 12;
		if (val == 4) return 16;
		if (val == 63) return 255;
		double l2 = Math.log10((double)val / 4.0) / Math.log10(2.0);
		l2 *= 16.0;
		//Now scale to 255
		double factor = 256.0/64.0;
		double temp = factor * l2;
		return (int)Math.round(temp);
	}
	
	/*----- Getters -----*/
	
	public BufferedImage getImage(){
		int h = bitmap[0].length;
		int w = bitmap.length;
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		
		for(int y = 0; y < h; y++){
			for(int x = 0; x < w; x++){
				switch(image_type){
				case IMG_TYPE_PLT: 
					img.setRGB(x, y, palette[bitmap[x][y]]);
					break;
				case IMG_TYPE_TRUECOLOR: 
					img.setRGB(x, y, bitmap[x][y]);
					break;
				case IMG_TYPE_BW: 
					//TODO
					break;
				}
			}
		}
		
		return img;
	}
	
	public int getXOffset(){return this.img_x;}
	public int getYOffset(){return this.img_y;}
	
	/*----- Setters -----*/
	
	/*----- Definition -----*/
	
}
