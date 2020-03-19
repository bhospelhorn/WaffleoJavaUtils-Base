package waffleoRai_Utils;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import waffleoRai_Compression.definitions.AbstractCompDef;
import waffleoRai_Compression.definitions.CompDefNode;
import waffleoRai_Compression.definitions.CompressionInfoNode;
import waffleoRai_Files.EncryptionDefinition;
import waffleoRai_Files.FileTypeNode;

public class FileNode implements TreeNode, Comparable<FileNode>{

	private static boolean TOSTRING_FULLPATH = true;
	
	/* --- Instance Variables --- */
	
	protected DirectoryNode parent;
	
	private String sourcePath;
	
	private String fileName;
	private long offset;
	private long length;
	
	//Optional fields
	//private boolean compressedSource;
	//private long compStartOffset;
	private Map<String, String> metadata;
	
	//Compressed Container
	private List<CompressionInfoNode> comp_chain;
	
	//Type data on THIS file
	private FileTypeNode type_chain;
	
	private EncryptionDefinition encryption;
	
	//Scratch
	protected int scratch_field;
	
	/* --- Construction --- */
	
	public FileNode(DirectoryNode parent, String name)
	{
		this.parent = parent;
		fileName = name;
		offset = -1;
		length = 0;
		if(parent != null) parent.addChild(this);
	}
	
	/* --- Getters --- */
	
	public String getFileName(){return fileName;}
	public long getOffset(){return offset;}
	public long getLength(){return length;}
	public DirectoryNode getParent(){return parent;}
	public String getSourcePath(){return sourcePath;}
	public boolean isLink(){return false;}
	public EncryptionDefinition getEncryption(){return encryption;}
	public String getMetadataValue(String key){if(metadata == null){return null;}; return metadata.get(key);}
	
	public boolean hasMetadata()
	{
		if(metadata == null) return false;
		return (!metadata.isEmpty());
	}
	
	public List<String> getMetadataKeys()
	{
		List<String> keys = new LinkedList<String>();
		if(metadata != null) keys.addAll(metadata.keySet());
		return keys;
	}
	
	public boolean sourceDataCompressed(){return comp_chain != null && !comp_chain.isEmpty();}
	//public long getOffsetOfCompressionStart(){return compStartOffset;}
	public List<CompressionInfoNode> getCompressionChain()
	{
		int sz = 1;
		if(comp_chain != null) sz += comp_chain.size();
		List<CompressionInfoNode> list = new ArrayList<CompressionInfoNode>(sz);
		list.addAll(comp_chain);
		return list;
	}
	
	public FileTypeNode getTypeChainHead(){return type_chain;}
	
	public List<FileTypeNode> getTypeChainAsList()
	{
		List<FileTypeNode> list = new LinkedList<FileTypeNode>();
		if(type_chain != null)
		{
			FileTypeNode node = type_chain;
			while(node != null)
			{
				list.add(node);
				node = node.getChild();
			}
		}
		
		return list;
	}
	
	public String getFullPath()
	{
		LinkedList<String> names = new LinkedList<String>();
		DirectoryNode p = parent;
		names.add(getFileName());
		while(p != null)
		{
			String fn = p.getFileName();
			if(fn != null && !fn.isEmpty()) names.push(fn);
			p = p.getParent();
		}
		
		StringBuilder sb = new StringBuilder(2048);
		for(String s : names)
		{
			sb.append("/" + s);
		}
	
		return sb.toString();
	}
	
	public String toString()
	{
		if(FileNode.TOSTRING_FULLPATH) return getFullPath();
		else return getFileName();
	}
	
	/* --- Setters --- */
	
	public void setFileName(String name){fileName = name;}
	public void setOffset(long off){offset = off;}
	public void setLength(long len){length = len;}
	public void setSourcePath(String path){sourcePath = path;}
	public void setEncryption(EncryptionDefinition def){encryption = def;}
	
	public void setMetadataValue(String key, String value)
	{
		if(metadata == null) metadata = new HashMap<String, String>();
		metadata.put(key, value);
	}
	
	public void setParent(DirectoryNode p)
	{
		if(parent != null) parent.removeChild(this);
		
		parent = p; 
		if(p != null) p.addChild(this);
	}
	
	//public void setSourceDataDecompressed(){compressedSource = false;}
	//public void setSourceDataCompressed(long c_start){compressedSource = true; compStartOffset = c_start;}
	public void addCompressionChainNode(AbstractCompDef def, long stpos, long len)
	{
		if(comp_chain == null) comp_chain = new LinkedList<CompressionInfoNode>();
		comp_chain.add(new CompressionInfoNode(def, stpos, len));
	}
	
	public void clearCompressionChain(){comp_chain.clear();}
	
	public void setTypeChainHead(FileTypeNode head){type_chain = head;}
	public void clearTypeChain(){type_chain = null;}
	
	public static void setUseFullPathInToString(boolean b)
	{
		FileNode.TOSTRING_FULLPATH = b;
	}
	
	/* --- Comparable --- */
	
	public boolean isDirectory()
	{
		return false;
	}
	
	public boolean equals(Object o)
	{
		if(o == this) return true;
		if(o == null) return false;
		if(!(o instanceof FileNode)) return false;
		FileNode fn = (FileNode)o;
		if(this.isDirectory() != fn.isDirectory()) return false;
		return fileName.equals(fn.fileName);
	}
	
	public int hashCode()
	{
		return fileName.hashCode() ^ (int)offset;
	}
	
	public int compareTo(FileNode other)
	{
		if(other == this) return 0;
		if(other == null) return 1;
		
		if(this.isDirectory() && !other.isDirectory()) return -1;
		if(!this.isDirectory() && other.isDirectory()) return 1;
		
		return this.fileName.compareTo(other.fileName);
	}
	
	/* --- TreeNode --- */
	
	@Override
	public TreeNode getChildAt(int childIndex) {return null;}

	@Override
	public int getChildCount() {return 0;}

	@Override
	public int getIndex(TreeNode node) 
	{
		return -1;
	}

	@Override
	public boolean getAllowsChildren() {return false;}

	@Override
	public boolean isLeaf() {return true;}

	@Override
	public Enumeration<TreeNode> children() 
	{
		TreeNode[] n = null;
		return new Treenumeration(n);
	}
	
	public static String readTreePath(TreePath treepath)
	{
		if(treepath == null) return null;
		Object lasty = treepath.getLastPathComponent();
		if(lasty instanceof FileNode) return ((FileNode)lasty).getFullPath();
		return lasty.toString();
	}
	
	/* --- Other --- */
	
	public FileNode copy(DirectoryNode parent_copy)
	{
		FileNode copy = new FileNode(parent_copy, this.fileName);
		copy.length = this.length;
		copy.offset = this.offset;
		copy.sourcePath = this.sourcePath;
		
		return copy;
	}
	
	public FileNode copy(DirectoryNode parent_copy, FileNode link_copy)
	{
		return copy(parent_copy);
	}
	
	public boolean splitNodeAt(long off)
	{
		if(off >= this.length) return false;
		if(off < 0) return false;
		
		long rel_off = off + this.offset;
		long l1 = rel_off - off;
		long l2 = (offset + length) - rel_off;
		
		String myname = getFileName();
		FileNode n1 = new FileNode(parent, myname + ".front");
		FileNode n2 = new FileNode(parent, myname + ".back");
		
		n1.offset = offset;
		n1.setLength(l1);
		n1.setSourcePath(sourcePath);
		n1.encryption = encryption;
		n1.comp_chain = this.getCompressionChain();
		
		n2.offset = rel_off;
		n2.setLength(l2);
		n2.setSourcePath(sourcePath);
		n2.encryption = encryption;
		n2.comp_chain = this.getCompressionChain();
		
		setParent(null);
		
		return true;
	}
	
	/* --- Load --- */
	
	public FileBuffer loadData() throws IOException
	{
		String path = getSourcePath();
		long stoff = getOffset();
		long edoff = stoff + getLength();
		
		if(comp_chain != null)
		{
			for(CompressionInfoNode comp : comp_chain)
			{
				FileBuffer file = null;
				
				stoff = comp.getStartOffset();
				if(comp.getLength() > 0)
				{
					edoff = stoff + comp.getLength();
					file = FileBuffer.createBuffer(path, stoff, edoff);	
				}
				else file = FileBuffer.createBuffer(path, stoff);
				FileBufferStreamer streamer = new FileBufferStreamer(file);
				AbstractCompDef def = comp.getDefinition();
				if(def == null) return null;
				path = def.decompressToDiskBuffer(streamer);
			}
		}
		
		FileBuffer file = FileBuffer.createBuffer(path, stoff, edoff);

		return file;
	}
	
	public FileBuffer loadDecompressedData() throws IOException
	{
		FileBuffer buffer = loadData();
		
		FileTypeNode typechain = getTypeChainHead();
		while(typechain != null)
		{
			if(typechain.isCompression())
			{
				if(typechain instanceof CompDefNode)
				{
					AbstractCompDef def = ((CompDefNode)typechain).getDefinition();
					
					String tpath = def.decompressToDiskBuffer(new FileBufferStreamer(buffer));
					buffer = FileBuffer.createBuffer(tpath);
				}
				
				typechain = typechain.getChild();
			}
			else break;
		}
		
		return buffer;
	}
	
	public boolean copyDataTo(String path, boolean decompress) throws IOException
	{
		if(path == null || path.isEmpty()) return false;
		
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path));
		copyDataTo(bos, decompress);
		bos.close();
		
		return true;
	}
	
	public boolean copyDataTo(OutputStream out, boolean decompress) throws IOException
	{
		if(out == null) return false;
		
		FileBuffer dat = null;
		if(decompress) dat = loadDecompressedData();
		else dat = loadData();
		dat.writeToStream(out);
		
		return true;
	}
	
	/* --- Debug --- */
	
	public void printMeToStdErr(int indents)
	{
		StringBuilder sb = new StringBuilder(128);
		for(int i = 0; i < indents; i++) sb.append("\t");
		String tabs = sb.toString();
		
		String off = "0x" + Long.toHexString(offset);
		String end = "0x" + Long.toHexString(offset + length);
		
		System.err.println(tabs + "->" + this.fileName + " (" + off + " - " + end + ")");
	}
	
}
