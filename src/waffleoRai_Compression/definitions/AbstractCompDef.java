package waffleoRai_Compression.definitions;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JPanel;

import waffleoRai_Files.FileClass;
import waffleoRai_Utils.BufferReference;
import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.StreamWrapper;

public abstract class AbstractCompDef {
	
	protected String name_str;
	protected Collection<String> extensions;
	
	protected AbstractCompDef(String defo_name_str){
		name_str = defo_name_str;
		ConcurrentHashMap<String, Boolean> dummy = new ConcurrentHashMap<String,Boolean>();
		extensions = dummy.keySet(true);
	}
	
	public Collection<String> getExtensions(){
		Set<String> sset = new HashSet<String>();
		sset.addAll(extensions);
		return sset;
	}
	
	public String getDescription(){return name_str;}
	public void setDescriptionString(String s){name_str = s;}
	
	public String toString(){
		StringBuilder sb = new StringBuilder(512);
		sb.append(name_str);
		if(!extensions.isEmpty()){
			sb.append(" (");
			
			List<String> elist = new ArrayList<String>(extensions.size()+1);
			elist.addAll(extensions);
			Collections.sort(elist);
			boolean first = true;
			for(String ex : elist){
				if(!first) sb.append(", ");
				else first = false;
				sb.append("." + ex);
			}
			
			sb.append(")");	
		}
		return sb.toString();
	}
	
	public boolean isCompression(){return true;}

	public JPanel getPreviewPanel(){return null;}
	
	public FileClass getFileClass(){return FileClass.COMPRESSED;}
	
	public abstract boolean decompressToDiskBuffer(StreamWrapper input, String bufferPath, int options);
	public abstract boolean decompressToDiskBuffer(InputStream input, String bufferPath, int options);
	public abstract boolean decompressToDiskBuffer(BufferReference input, String bufferPath, int options);
	public abstract FileBuffer decompressToMemory(StreamWrapper input, int allocAmount, int options);
	public abstract FileBuffer decompressToMemory(InputStream input, int allocAmount, int options);
	public abstract FileBuffer decompressToMemory(BufferReference input, int allocAmount, int options);
	
	public abstract int getDefinitionID();
	
}
