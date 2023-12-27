package waffleoRai_Image;

import java.io.IOException;
import java.util.Arrays;

import waffleoRai_Utils.BufferReference;
import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.FileBuffer.UnsupportedFileTypeException;

public class BmpFile{
	
	//en.wikipedia.org/wiki/BMP_file_Format
	
	/*----- Constants -----*/

	private static final String MAGIC_WIN = "BM";
	
	//Numerical values represent header size for easy id
	public static final int DIBHDR_VER_BITMAPCORE = 12;
	public static final int DIBHDR_VER_BITMAPINFOHEADER = 40;
	public static final int DIBHDR_VER_BITMAPV2INFOHEADER = 52;
	public static final int DIBHDR_VER_BITMAPV3INFOHEADER = 56;
	public static final int DIBHDR_VER_BITMAPV4HEADER = 108;
	public static final int DIBHDR_VER_BITMAPV5HEADER = 124;
	
	public static final int BI_RGB = 0;
	public static final int BI_RLE8 = 1;
	public static final int BI_RLE4 = 2;
	public static final int BI_BITFIELDS = 3;
	public static final int BI_JPEG = 4;
	public static final int BI_PNG = 5;
	public static final int BI_ALPHABITFIELDS = 6;
	public static final int BI_CMYK = 11;
	public static final int BI_CMYKRLE8 = 12;
	public static final int BI_CMYKRLE4 = 13;
	
	public static final int COLORSPACE_SRGB = 0x73524742; //"BGRs" when printed LE
	
	/*----- Instance Variables -----*/
	
	private int width;
	private int height;
	private int planes;
	private int bitDepth;
	
	private int encoding;
	private int imageDataSize;
	private int xResolution; //ppm
	private int yResolution; //ppm
	private int paletteEntries;
	private int impClrCount;
	
	private int bitMaskR;
	private int bitMaskG;
	private int bitMaskB;
	private int bitMaskA;
	
	private int colorSpace; //This is a 4-byte ASCII really.
	private byte[] csEndpoints; //36 bytes
	private int gammaRed;
	private int gammaGreen;
	private int gammaBlue;
	
	private int intent;
	private int iccProfileOffset;
	private int iccProfileSize;
	
	private int[] palette; //ARGB
	private int[][] imageData;
	
	/*----- Init -----*/
	
	private BmpFile(){
		encoding = BI_RGB;
		colorSpace = COLORSPACE_SRGB;
	}
	
	/*----- Getters -----*/
	
	public int getWidth(){return width;}
	public int getHeight(){return height;}
	public int getPlanes(){return planes;}
	public int getBitDepth(){return bitDepth;}
	public int getRawImageDataSize(){return imageDataSize;}
	public int getHorizontalResolution(){return xResolution;}
	public int getVerticalResolution(){return yResolution;}
	public int getImportantColorCount(){return impClrCount;}
	public int getBitmaskRed(){return bitMaskR;}
	public int getBitmaskGreen(){return bitMaskG;}
	public int getBitmaskBlue(){return bitMaskB;}
	public int getBitmaskAlpha(){return bitMaskA;}
	public int getColorSpace(){return colorSpace;}
	public int getGammaRed(){return gammaRed;}
	public int getGammaGreen(){return gammaGreen;}
	public int getGammaBlue(){return gammaBlue;}
	public int getICCIntent(){return intent;}
	public int getICCProfileOffset(){return iccProfileOffset;}
	public int getICCProfileSize(){return iccProfileSize;}
	
	public int[] getPaletteRaw(){
		if(palette == null) return null;
		int[] copy = Arrays.copyOf(palette, palette.length);
		return copy;
	}
	
	public int[][] getImageDataRaw(){
		if(imageData == null) return null;
		int[][] copy = new int[imageData.length][];
		for(int i = 0; i < imageData.length; i++){
			copy[i] = Arrays.copyOf(imageData[i], imageData[i].length);
		}
		return copy;
	}
	
	/*----- Setters -----*/
	
	/*----- Read -----*/
	
	private void readDIBCORE(BufferReference data, boolean small){
		if(small){
			width = Short.toUnsignedInt(data.nextShort());
			height = Short.toUnsignedInt(data.nextShort());
		}
		else{
			width = data.nextInt();
			height = data.nextInt();
		}
		planes = Short.toUnsignedInt(data.nextShort());
		bitDepth = Short.toUnsignedInt(data.nextShort());
	}
	
	private void readDIBINFO(BufferReference data){
		readDIBCORE(data, false);
		encoding = data.nextInt();
		imageDataSize = data.nextInt();
		xResolution = data.nextInt();
		yResolution = data.nextInt();
		paletteEntries = data.nextInt();
		impClrCount = data.nextInt();
	}
	
	private void readDIBV2(BufferReference data){
		readDIBINFO(data);
		bitMaskR = data.nextInt();
		bitMaskG = data.nextInt();
		bitMaskB = data.nextInt();
	}
	
	private void readDIBV3(BufferReference data){
		readDIBV2(data);
		bitMaskA = data.nextInt();
	}
	
	private void readDIBV4(BufferReference data){
		readDIBV3(data);
		colorSpace = data.nextInt();
		
		if(csEndpoints == null) csEndpoints = new byte[36];
		for(int i = 0; i < 36; i++) csEndpoints[i] = data.nextByte();
		
		gammaRed = data.nextInt();
		gammaGreen = data.nextInt();
		gammaBlue = data.nextInt();
	}
	
	private void readDIBV5(BufferReference data){
		readDIBV4(data);
		intent = data.nextInt();
		iccProfileOffset = data.nextInt();
		iccProfileSize = data.nextInt();
		data.add(4L); //Reserved.
	}
	
	public static BmpFile readBMP(String filepath) throws IOException, UnsupportedFileTypeException{
		FileBuffer data = FileBuffer.createBuffer(filepath, false);
		return readBMP(data.getReferenceAt(0L));
	}
	
	public static BmpFile readBMP(FileBuffer data) throws UnsupportedFileTypeException{
		if(data == null) return null;
		data.setEndian(false);
		return readBMP(data.getReferenceAt(0L));
	}
	
	public static BmpFile readBMP(BufferReference data) throws UnsupportedFileTypeException{
		long stpos = data.getBufferPosition();
		String mcheck = data.nextASCIIString(2);
		if(mcheck == null || mcheck.isEmpty() || !mcheck.equals(MAGIC_WIN)){
			throw new FileBuffer.UnsupportedFileTypeException(
					"BmpFile.readBMP || BMP identifier not found!");
		}
		
		//Skip file size & reserved
		data.add(8L); 
		long pixdataPos = data.nextInt() + stpos;
		
		//DIB Header
		BmpFile bmp = new BmpFile();
		int dibSize = data.nextInt();
		switch(dibSize){
		case DIBHDR_VER_BITMAPCORE:
			bmp.readDIBCORE(data, true);
			break;
		case DIBHDR_VER_BITMAPINFOHEADER:
			bmp.readDIBINFO(data);
			break;
		case DIBHDR_VER_BITMAPV2INFOHEADER:
			bmp.readDIBV2(data);
			break;
		case DIBHDR_VER_BITMAPV3INFOHEADER:
			bmp.readDIBV3(data);
			break;
		case DIBHDR_VER_BITMAPV4HEADER:
			bmp.readDIBV4(data);
			break;
		case DIBHDR_VER_BITMAPV5HEADER:
			bmp.readDIBV5(data);
			break;
		default:
			throw new FileBuffer.UnsupportedFileTypeException(
					"BmpFile.readBMP || DIB header version not recognized!");
		}
		
		//Right now I can only be bothered to read default encoding/color space.
		if(bmp.encoding != BI_RGB){
			throw new FileBuffer.UnsupportedFileTypeException(
					"BmpFile.readBMP || Non-default encodings not supported at this time.");
		}
		
		//Read palette, if applicable
		if(bmp.bitDepth <= 8){
			if(bmp.paletteEntries <= 0){
				throw new FileBuffer.UnsupportedFileTypeException(
						"BmpFile.readBMP || Palette required for bit depths at or below 8!");
			}
			
			bmp.palette = new int[bmp.paletteEntries];
			for(int i = 0; i < bmp.paletteEntries; i++){
				bmp.palette[i] = data.nextInt();
			}
		}
		
		//Read image data
		while(data.getBufferPosition() < pixdataPos) data.add(1L);
		bmp.imageData = new int[bmp.height][bmp.width]; //YX or Row Column
		
		for(int r = bmp.height - 1; r >= 0; r--){
			
			switch(bmp.bitDepth){
			case 1:
				for(int l = 0; l < bmp.width; l+=8){
					int b = Byte.toUnsignedInt(data.nextByte());
					int shamt = 7;
					for(int j = 0; j < 8; j++){
						int jj = l+j;
						if(jj >= bmp.width) break;
						bmp.imageData[r][jj] = (b >>> shamt) & 0x1;
						shamt--;
					}
				}
				break;
			case 2:
				for(int l = 0; l < bmp.width; l+=4){
					int b = Byte.toUnsignedInt(data.nextByte());
					int shamt = 6;
					for(int j = 0; j < 4; j++){
						int jj = l+j;
						if(jj >= bmp.width) break;
						bmp.imageData[r][jj] = (b >>> shamt) & 0x3;
						shamt -= 2;
					}
				}
				break;
			case 4:
				for(int l = 0; l < bmp.width; l+=2){
					int b = Byte.toUnsignedInt(data.nextByte());
					bmp.imageData[r][l] = (b >>> 4) & 0xf;
					if(l+1 < bmp.width){
						bmp.imageData[r][l+1] = b & 0xf;
					}
				}
				break;
			case 8:
				for(int l = 0; l < bmp.width; l++){
					bmp.imageData[r][l] = Byte.toUnsignedInt(data.nextByte());
				}
				break;
			case 16:
				for(int l = 0; l < bmp.width; l++){
					bmp.imageData[r][l] = Short.toUnsignedInt(data.nextShort());
				}
				break;
			case 24:
				for(int l = 0; l < bmp.width; l++){
					bmp.imageData[r][l] = data.next24Bits();
				}
				break;
			case 32:
				for(int l = 0; l < bmp.width; l++){
					bmp.imageData[r][l] = data.nextInt();
				}
				break;
			}
			
			//Skip row padding (Position is relative to image data start)
			long nowpos = data.getBufferPosition() - pixdataPos;
			long padding = (4 - (nowpos & 0x3L)) & 0x3L;
			data.add(padding);
		}
		
		return bmp;
	}
	
	/*----- Write -----*/
	
}
