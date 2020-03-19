package waffleoRai_Compression.definitions;

public class CompressionInfoNode {
	
	private AbstractCompDef def;
	private long stOff;
	private long len;
	
	public CompressionInfoNode(AbstractCompDef definition, long start, long length)
	{
		def = definition;
		stOff = start;
		len = length;
	}
	
	public AbstractCompDef getDefinition(){return def;}
	public long getStartOffset(){return stOff;}
	public long getLength(){return len;}

}
