package waffleoRai_Containers.media.matroska;

public class MatroskaTrackVideo {
	
	private boolean flagInterlaced;
	private int fieldOrder;
	private int stereoMode;
	private int alphaMode;
	private int pixelWidth;
	private int pixelHeight;
	private int pixelCropBottom;
	private int pixelCropTop;
	private int pixelCropLeft;
	private int pixelCropRight;
	private int displayWidth;
	private int displayHeight;
	private int displayUnit;
	private byte[] ucFourCC;
	
	private MatroskaVideoColorInfo color;
	private MatroskaProjectionInfo projection;
	
	/*----- Getters -----*/

	public boolean getFlagInterlaced(){return flagInterlaced;}
	public int getFieldOrder(){return fieldOrder;}
	public int getStereoMode(){return stereoMode;}
	public int getAlphaMode(){return alphaMode;}
	public int getPixelWidth(){return pixelWidth;}
	public int getPixelHeight(){return pixelHeight;}
	public int getPixelCropBottom(){return pixelCropBottom;}
	public int getPixelCropTop(){return pixelCropTop;}
	public int getPixelCropLeft(){return pixelCropLeft;}
	public int getPixelCropRight(){return pixelCropRight;}
	public int getDisplayWidth(){return displayWidth;}
	public int getDisplayHeight(){return displayHeight;}
	public int getDisplayUnit(){return displayUnit;}
	public byte[] getUcFourCC(){return ucFourCC;}
	public MatroskaVideoColorInfo getColor(){return color;}
	public MatroskaProjectionInfo getProjection(){return projection;}

	/*----- Setters -----*/

	public void setFlagInterlaced(boolean value){flagInterlaced = value;}
	public void setFieldOrder(int value){fieldOrder = value;}
	public void setStereoMode(int value){stereoMode = value;}
	public void setAlphaMode(int value){alphaMode = value;}
	public void setPixelWidth(int value){pixelWidth = value;}
	public void setPixelHeight(int value){pixelHeight = value;}
	public void setPixelCropBottom(int value){pixelCropBottom = value;}
	public void setPixelCropTop(int value){pixelCropTop = value;}
	public void setPixelCropLeft(int value){pixelCropLeft = value;}
	public void setPixelCropRight(int value){pixelCropRight = value;}
	public void setDisplayWidth(int value){displayWidth = value;}
	public void setDisplayHeight(int value){displayHeight = value;}
	public void setDisplayUnit(int value){displayUnit = value;}
	public void setUcFourCC(byte[] value){ucFourCC = value;}
	public void setColor(MatroskaVideoColorInfo value){color = value;}
	public void setProjection(MatroskaProjectionInfo value){projection = value;}


}
