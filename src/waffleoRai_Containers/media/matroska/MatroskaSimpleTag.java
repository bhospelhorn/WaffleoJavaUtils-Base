package waffleoRai_Containers.media.matroska;

public class MatroskaSimpleTag {

	private String tagName;
	private String tagLang;
	private String tagLangBCP47;
	private int tagDefault;
	private String tagString;
	private byte[] tagBinary;
	
	/*----- Getters -----*/

	public String getTagName(){return tagName;}
	public String getTagLang(){return tagLang;}
	public String getTagLangBCP47(){return tagLangBCP47;}
	public int getTagDefault(){return tagDefault;}
	public String getTagString(){return tagString;}
	public byte[] getTagBinary(){return tagBinary;}

	/*----- Setters -----*/

	public void setTagName(String value){tagName = value;}
	public void setTagLang(String value){tagLang = value;}
	public void setTagLangBCP47(String value){tagLangBCP47 = value;}
	public void setTagDefault(int value){tagDefault = value;}
	public void setTagString(String value){tagString = value;}
	public void setTagBinary(byte[] value){tagBinary = value;}
	
}
