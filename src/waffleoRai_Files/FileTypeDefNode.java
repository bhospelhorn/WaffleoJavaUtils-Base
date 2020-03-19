package waffleoRai_Files;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FileTypeDefNode implements FileTypeNode{

	private FileTypeDefinition def;
	
	public FileTypeDefNode(FileTypeDefinition type)
	{
		def = type;
	}
	
	@Override
	public boolean isCompression() {return false;}

	@Override
	public FileTypeNode getChild() {return null;}

	@Override
	public int getTypeID() {return def.getTypeID();}
	
	public FileTypeDefinition getDefinition(){return def;}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder(512);
		sb.append(def.getDescription());
		Collection<String> extensions = def.getExtensions();
		if(!extensions.isEmpty())
		{
			sb.append(" (");
			
			List<String> elist = new ArrayList<String>(extensions.size()+1);
			elist.addAll(extensions);
			Collections.sort(elist);
			boolean first = true;
			for(String ex : elist)
			{
				if(!first) sb.append(", ");
				else first = false;
				sb.append("." + ex);
			}
			
			sb.append(")");	
		}
		return sb.toString();
	}
	
	public void setChild(FileTypeNode node){}

	public FileClass getFileClass(){return def.getFileClass();}
	
}
