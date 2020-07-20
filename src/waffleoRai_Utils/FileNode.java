package waffleoRai_Utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import waffleoRai_Compression.definitions.AbstractCompDef;
import waffleoRai_Compression.definitions.CompDefNode;
import waffleoRai_Compression.definitions.CompressionInfoNode;
import waffleoRai_Encryption.StaticDecryption;
import waffleoRai_Encryption.StaticDecryptor;
import waffleoRai_Files.EncryptionDefinition;
import waffleoRai_Files.FileTypeNode;
import waffleoRai_Files.NodeMatchCallback;

public class FileNode implements TreeNode, Comparable<FileNode>{
	
	public static final int SORT_ORDER_NORMAL = 0; //Dir alpha, then file alpha
	public static final int SORT_ORDER_LOCATION = 1; //Dir alpha, file src alpha, file offset

	private static boolean TOSTRING_FULLPATH = true;
	private static int SORT_BY = SORT_ORDER_NORMAL;
	
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
	private LinkedList<CompressionInfoNode> comp_chain;
	
	//Type data on THIS file
	private FileTypeNode type_chain;
	
	private EncryptionDefinition encryption;
	private long enc_start;
	private long enc_len;
	
	//Scratch
	protected int scratch_field;
	protected long uid;
	
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
	public long getEncryptionOffset(){return enc_start;}
	public long getEncryptionLength(){return enc_len;}
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
		if(comp_chain != null) list.addAll(comp_chain);
		return list;
	}
	
	public FileTypeNode getTypeChainHead(){return type_chain;}
	
	public FileTypeNode getTypeChainTail(){
		if(type_chain == null) return null;
		FileTypeNode child = type_chain;
		while(child.getChild() != null)child = child.getChild();
		
		return child;
	}
	
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
	
	public boolean hasTypingMark(){
		return (type_chain != null);
	}
	
	public long getUID(){return uid;}
	
	/* --- Setters --- */
	
	public void setUID(long id){uid = id;}
	
	public void setFileName(String name){
		String oldname = fileName;
		fileName = name;
		if(parent != null) parent.changeChildName(this, oldname);
	}
	
	public void setOffset(long off){offset = off;}
	public void setLength(long len){length = len;}
	public void setSourcePath(String path){sourcePath = path;}
	
	public void setEncryption(EncryptionDefinition def){
		encryption = def;
		enc_start = 0;
		enc_len = length;
	}
	
	public void setEncryption(EncryptionDefinition def, long offset, long length){
		encryption = def;
		enc_start = offset;
		enc_len = length;
	}
	
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
		//System.err.println("addCompressionChainNode: 0x" + Long.toHexString(stpos));
		comp_chain.add(new CompressionInfoNode(def, stpos, len));
	}
	
	public void pushCompressionChainNode(AbstractCompDef def, long stpos, long len)
	{
		if(comp_chain == null) comp_chain = new LinkedList<CompressionInfoNode>();
		comp_chain.push(new CompressionInfoNode(def, stpos, len));
	}
	
	public void clearCompressionChain(){comp_chain.clear();}
	
	public void setTypeChainHead(FileTypeNode head){type_chain = head;}
	public void clearTypeChain(){type_chain = null;}
	
	public static void setUseFullPathInToString(boolean b)
	{
		FileNode.TOSTRING_FULLPATH = b;
	}
	
	public static void setSortOrder(int sort_order){
		SORT_BY = sort_order;
	}
	
	
	/* --- Comparable --- */
	
	public boolean isDirectory()
	{
		return false;
	}
	
	public boolean equals(Object o)
	{
		if(o == this) {return true;}
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
		
		if(this.isDirectory() && other.isDirectory()){
			//If one has type marking and other doesn't, nonmarked comes first
			if(this.hasTypingMark() && !other.hasTypingMark()) return 1;
			if(!this.hasTypingMark() && other.hasTypingMark()) return -1;
		}
		
		if(SORT_BY == SORT_ORDER_LOCATION){
			String tsrcpath = this.getSourcePath();
			String osrcpath = other.getSourcePath();
			
			if(tsrcpath == null){
				if(osrcpath != null) return -1;
				long offdiff = this.getOffset() - other.getOffset();
				if(offdiff >= 0L) return 1;
				else return -1;
			}
			else{
				if(!tsrcpath.equals(osrcpath)) return tsrcpath.compareTo(osrcpath);
				long offdiff = this.getOffset() - other.getOffset();
				if(offdiff >= 0L) return 1;
				else return -1;
			}
		}
		
		//Default: normal sort order
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
	
	protected void copyDataTo(FileNode copy){
		
		copy.length = this.length;
		copy.offset = this.offset;
		copy.sourcePath = this.sourcePath;
		
		copy.encryption = encryption;
		copy.enc_start = enc_start;
		copy.enc_len = enc_len;
		
		copy.comp_chain = new LinkedList<CompressionInfoNode>();
		if(this.comp_chain != null) copy.comp_chain.addAll(comp_chain);
		
		if(metadata != null){
			Set<String> metakeys = metadata.keySet();
			for(String k : metakeys) copy.setMetadataValue(k, metadata.get(k));	
		}
		
		//Copy type chain
		if(type_chain != null) copy.type_chain = type_chain.copyChain();
		else copy.type_chain = null;
		
	}
	
	public FileNode copy(DirectoryNode parent_copy)
	{
		FileNode copy = new FileNode(parent_copy, this.fileName);
		copyDataTo(copy);
		
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
		
		copyDataTo(n1);
		copyDataTo(n2);
		
		n1.offset = offset;
		n1.setLength(l1);

		n2.offset = rel_off;
		n2.setLength(l2);
		
		//Splitting not recommended if encrypted, but hey you do you
		if(encryption != null){
			long enc_end = enc_start + enc_len;
			if(enc_start < rel_off){
				//Covers n1
				if(enc_end < rel_off){
					//Ends before end of n1
					n1.enc_len = rel_off - enc_start;
				}
				else n1.enc_len = n1.length - enc_start;
			}
			else{
				n1.encryption = null;
				n1.enc_start = 0;
				n1.enc_len = 0;
			}
			
			if(enc_end > rel_off){
				//Covers n2
				if(enc_start < rel_off) n2.enc_start = 0;
				else n2.enc_start = enc_start - rel_off;
				
				if(enc_start < rel_off) n2.enc_len = enc_end - rel_off;
				else n2.enc_len = enc_end - enc_start;
			}
			else{
				n2.encryption = null;
				n2.enc_start = 0;
				n2.enc_len = 0;
			}
		}

		setParent(null);
		
		return true;
	}
	
	public String getLocationString(){
		if(this.isDirectory()) return "Directory";
		StringBuilder sb = new StringBuilder(1024);
		
		if(sourceDataCompressed()){
			for(CompressionInfoNode c : comp_chain){
				sb.append("Decomp From: 0x" + Long.toHexString(c.getStartOffset()) + " -> ");
			}
			sb.append("0x" + Long.toHexString(getOffset()));
			sb.append(" - 0x" + Long.toHexString(getOffset() + getLength()));
		}
		else{
			sb.append("0x" + Long.toHexString(getOffset()));
			sb.append(" - 0x" + Long.toHexString(getOffset() + getLength()));
		}
	
		return sb.toString();
	}
	
	/* --- Scan --- */
	
	public String findNodeThat(NodeMatchCallback cond){
		if(parent != null) return parent.findNodeThat(cond);
		else return null;
	}
	
	/* --- Load --- */
	
	public FileBuffer loadData(long stpos, long len) throws IOException{
		String path = getSourcePath();
		long stoff = getOffset() + stpos;
		long maxed = getOffset() + getLength();
		long edoff = stoff + len;
		if(edoff > maxed) edoff = maxed;
		
		//Handle encryption if present, and possible
		if(encryption != null){
			StaticDecryptor decryptor = StaticDecryption.getDecryptorState();
			if(decryptor != null){
				File f = decryptor.decrypt(this);
				if(f != null) path = f.getAbsolutePath();
			}
		}
		
		if(comp_chain != null)
		{
			//System.err.println("Non-null compression chain!");
			for(CompressionInfoNode comp : comp_chain)
			{
				FileBuffer file = null;
				
				stoff = comp.getStartOffset();
				if(comp.getLength() > 0)
				{
					edoff = stoff + comp.getLength();
					file = FileBuffer.createBuffer(path, stoff, edoff);	
					//System.err.println("Source compressed region: 0x" + Long.toHexString(stoff) + " - 0x" + Long.toHexString(edoff));
				}
				else file = FileBuffer.createBuffer(path, stoff);
				FileBufferStreamer streamer = new FileBufferStreamer(file);
				AbstractCompDef def = comp.getDefinition();
				if(def == null) return null;
				path = def.decompressToDiskBuffer(streamer);
				//System.err.println("Decompressed to: " + path);
				stoff = this.getOffset();
				edoff = stoff + this.getLength();
			}
		}
		
		//System.err.println("Loading from " + path + ": 0x" + Long.toHexString(stoff) + " - 0x" + Long.toHexString(edoff));
		FileBuffer file = FileBuffer.createBuffer(path, stoff, edoff);
		//System.err.println("File loaded! Size = 0x" + Long.toHexString(file.getFileSize()));

		return file;
	}
	
 	public FileBuffer loadData() throws IOException{
		return loadData(0, getLength());
	}
	
	public FileBuffer loadDecompressedData() throws IOException
	{
		FileBuffer buffer = loadData();
		
		FileTypeNode typechain = getTypeChainHead();
		while(typechain != null)
		{
			//System.err.println("Type: " + typechain.toString());
			if(typechain.isCompression())
			{
				//System.err.println("Compression type!");
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
		
		//System.err.println("Returning buffer of size 0x" + Long.toHexString(buffer.getFileSize()));
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
