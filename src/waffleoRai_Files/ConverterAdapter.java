package waffleoRai_Files;

public abstract class ConverterAdapter implements Converter{
	
	protected String from_desc;
	protected String to_desc;
	protected String from_ext;
	protected String to_ext;
	
	public ConverterAdapter(String init_from_ext, String init_from_str, String init_to_ext, String init_to_str){
		from_desc = init_from_str;
		to_desc = init_to_str;
		from_ext = init_from_ext;
		to_ext = init_to_ext;
	}
	
	public String getFromFormatDescription(){return from_desc;}
	public String getToFormatDescription(){return to_desc;}
	
	public void setFromFormatDescription(String s){from_desc = s;}
	public void setToFormatDescription(String s){to_desc = s;}
	
	public String changeExtension(String path){
		if(!path.contains(".")) return path + "." + to_ext;
		return path.replace("." + from_ext, "." + to_ext);
	}

}
