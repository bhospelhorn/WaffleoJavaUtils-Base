package waffleoRai_Files;

import java.io.IOException;

import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.FileBuffer.UnsupportedFileTypeException;

public interface Converter {

	public String getFromFormatDescription();
	public String getToFormatDescription();
	
	public void setFromFormatDescription(String s);
	public void setToFormatDescription(String s);
	
	public void writeAsTargetFormat(String inpath, String outpath) throws IOException, UnsupportedFileTypeException;
	public void writeAsTargetFormat(FileBuffer input, String outpath) throws IOException, UnsupportedFileTypeException;
	
	public String changeExtension(String path);
	
}
