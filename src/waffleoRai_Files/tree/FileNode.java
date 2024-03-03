package waffleoRai_Files.tree;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import waffleoRai_Compression.definitions.AbstractCompDef;
import waffleoRai_Compression.definitions.CompDefNode;
import waffleoRai_Compression.definitions.CompressionDefs;
import waffleoRai_DataContainers.Treenumeration;
import waffleoRai_Encryption.DecryptorMethod;
import waffleoRai_Encryption.StaticDecryption;
import waffleoRai_Encryption.StaticDecryptor;
import waffleoRai_Files.EncryptionDefinition;
import waffleoRai_Files.FileTypeNode;
import waffleoRai_Files.NodeMatchCallback;
import waffleoRai_Utils.CacheFileBuffer;
import waffleoRai_Utils.EncryptedFileBuffer;
import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.FileUtils;
import waffleoRai_Utils.MultiFileBuffer;

/*
 * UPDATES
 * 
 * Version 3.0.0 | Doc'd
 * 	("Initial" version number chosen arbitrarily)
 * 	Added virtual sourcing
 * 	Overhauled container and encryption linking
 *
 * 2020.09.08 | 3.0.0 -> 3.1.0
 * 	Move decryption handling to after file data is loaded
 * 		(to use EncryptedFileBuffer instead of writing to disk)
 *
 * 2020.09.19 | 3.1.0 -> 3.2.0
 * 	Added some methods for hiding parent (for temp dismounting)
 * 
 * 2020.09.30 | 3.2.0 -> 3.2.1
 * 	Added internal protected flag to allow for decryption in subclass loadDirect()
 * 
 * 2020.10.26 | 3.2.1 -> 3.3.0
 * 	Added ability to snap blocks for load
 * 
 * 2020.10.27 | 3.3.0 -> 3.3.1
 * 	Split into input & output block sizes
 * 
 * 2020.10.30 | 3.3.1 -> 3.3.2
 * 	Blocked load debugging
 * 
 * 2020.11.04 | 3.3.2 -> 3.3.3
 * 	Fixed an issue with sign extension in int -> long conversions in file loading
 * 
 * 2020.12.26 | 3.3.3 -> 3.4.0
 * 	Created a static method that generates a FileNode for a file on disk.
 * 
 * 2021.01.28 | 3.4.0 -> 3.5.0
 * 	Ref trace methods
 * 
 * 2023.11.06 | 3.5.0 -> 3.6.0
 * 	Added timestamp field
 *  Data load interface modifications. Switched to use of options flags.
 *  	Allowed ability to control if containers/decompression/decryption buffers
 *  	use disk or memory.
 *  
 *  2023.11.08 | 3.6.0 -> 3.6.1
 * 	 Cleanup
 * 
 *  2024.02.21 | 3.6.1 -> 3.6.2
 * 	 Fixed very stupid NPEs for container on load
 * 
 */

/**
 * A container that abstractifies a "file". Contains information and methods for reading
 * and loading a chunk of data from disk, even with the target data chunk is not a single
 * discrete disk file. It can be a full file, part of a file (defined by offset and length),
 * or several pieces of a single or multiple files. 
 * <br> <code>FileNode</code>s can be linked to create a virtual file tree.
 * <br> This class was designed for use reading and managing file systems contained
 * within files on disk (such as archives or device images).
 * <br> This class replaces the deprecated <code>VirFile</code> class.
 * @author Blythe Hospelhorn
 * @version 3.6.2
 * @since February 21, 2024
 */
public class FileNode implements TreeNode, Comparable<FileNode>{
	
	public static final int SORT_ORDER_NORMAL = 0; //Dir alpha, then file alpha
	public static final int SORT_ORDER_LOCATION = 1; //Dir alpha, file src alpha, file offset
	
	public static final int LOADFAIL_FLAG_CNTNR_DECOMP = 0x1;
	public static final int LOADFAIL_FLAG_CNTNR_DECRYPT = 0x2;
	public static final int LOADFAIL_FLAG_DATA_DECOMP = 0x4;
	public static final int LOADFAIL_FLAG_DATA_DECRYPT = 0x8;
	public static final int LOADFAIL_FLAG_SRCFILE_DNE = 0x10;
	public static final int LOADFAIL_FLAG_BADOFF = 0x20;
	public static final int LOADFAIL_FLAG_ISDIR = 0x40;
	public static final int LOADFAIL_FLAG_NO_DISKBUFF_PATH = 0x80;
	public static final int LOADFAIL_FLAG_MEMBUFF_TOOBIG = 0x100;
	
	/**
	 * Value to represent no load options (zero, all flags unset).
	 * @since 3.6.0
	 */
	public static final int LOADOP_NONE = 0;
	
	/**
	 * Load option flag. Whether or not to instantiate all <code>FileBuffer</code> containers
	 * as <code>CacheFileBuffer</code> regardless of size. This can be useful in the case of
	 * patchwork/fragmented nodes where loaded data can consist of many smaller pieces that
	 * may add up to a very large total.
	 * @since 3.6.0
	 */
	public static final int LOADOP_FORCE_CACHE = 1 << 0;
	
	/**
	 * Load option flag. Whether or not to use disk (as opposed to memory)
	 * to buffer loading intermediates such as decompressed or
	 * decrypted containers.
	 * @since 3.6.0
	 */
	public static final int LOADOP_DISK_BUFFER = 1 << 1;
	
	/**
	 * Load option flag. Whether or not to attempt decompression of node data. If not, the
	 * returned node will either be this node or reference the open container.
	 * @since 3.6.0
	 */
	public static final int LOADOP_DECOMPRESS = 1 << 2;
	
	/**
	 * Load option flag. Whether or not to attempt decryption of node data. If not, the
	 * returned node will either be this node or reference the open container.
	 * @since 3.6.0
	 */
	public static final int LOADOP_DECRYPT = 1 << 3;

	/* --- Static Variables --- */
	
	private static boolean TOSTRING_FULLPATH = true;
	private static int SORT_BY = SORT_ORDER_NORMAL;
	
	private static String TEMP_DIR;
	
	/* --- Instance Variables --- */
	
	protected DirectoryNode parent;
	private DirectoryNode hidden_parent;
	
	private String sourcePath;
	private boolean src_virtual;
	private FileNode sourceNode;
	//If source is virtual, path can either be used to specify source node's GUID ("VN(HEXUID)")
	//Or the path to the FileTreeSaver list file containing the node (ie. (path)/(HEXUID))
	
	private String fileName;
	private long offset;
	private long length;
	
	private int blocksize_in = -1;
	private int blocksize_out = -1; //Used to force it to load in even blocks
	
	private long guid;
	
	//Optional fields
	private ZonedDateTime timestamp;
	private Map<String, String> metadata;
	
	//Compressed Container
	//private LinkedList<FileNode> container_chain;
	private FileNode container;
	
	//Type data on THIS file
	private LinkedList<EncryInfoNode> encryption_chain;
	private FileTypeNode type_chain;
	
	//Error flags
	//For when load fails... use these to figure out why
	protected int lfail_flags;
	
	//Temp files
	private String cont_temp; //If this node is used as a container, here's its temp path...
	private List<String> temp_paths;
	
	//Scratch
	protected int scratch_field;
	protected long scratch_long;
	protected boolean load_flag_decwrap_direct;
	
	/* --- Construction --- */
	
	/**
	 * Instantiate a new basic <code>FileNode</code> with the provided <code>String</code> as its local name,
	 * and <code>DirectoryNode</code> as its parent.
	 * @param parent Initial parent <code>DirectoryNode</code>. If null, can be set later using <code>setParent</code>.
	 * @param name Initial node name. If null or empty, can be set later using <code>setFileName</code>. Note that there
	 * may be issues with <code>String</code> based tree retrieval if the name is left null or empty.
	 */
	public FileNode(DirectoryNode parent, String name){
		this.parent = parent;
		fileName = name;
		offset = -1;
		length = 0;
		if(parent != null) parent.addChild(this);
		guid = -1L;
	}
	
	/* --- Getters --- */
	
	/**
	 * Get the local name of this node. This is its individual name, it does
	 * not reflect its path or position.
	 * @return A <code>String</code> containing the node's local name. If unset, this will
	 * return <code>null</code> or an empty <code>String</code>.
	 */
	public String getFileName(){return fileName;}
	
	/**
	 * Get the full tree root relative path (delimited by <code>'/'</code>)
	 * to this node in its virtual tree.
	 * @return <code>String</code> representing this node's virtual tree location.
	 */
	public String getFullPath(){
		LinkedList<String> names = new LinkedList<String>();
		DirectoryNode p = parent;
		names.add(getFileName());
		while(p != null){
			String fn = p.getFileName();
			if(fn != null && !fn.isEmpty()) names.push(fn);
			p = p.getParent();
		}
		
		StringBuilder sb = new StringBuilder(2048);
		for(String s : names) sb.append("/" + s);
	
		return sb.toString();
	}
	
	/**
	 * Get the path on the local file system pointing to the file
	 * containing the primary data this node references.
	 * <br>Note that this method only returns one <code>String</code>. If the node
	 * is a patchwork referencing multiple disk files, this will only return the first
	 * source path.
	 * <br>Additionally, if the data source is another node (ie. is virtual), this path
	 * may be a path to the file specifying the source node, or may be something else altogether.
	 * In other words, the path's meaning is unpredictable.
	 * <br>While this method is good for gleaning information about the node, 
	 * if the goal is loading the file data, it is better to call <code>loadData</code> or
	 * <code>loadDecompressedData</code>.
	 * @return Currently set path to source data as a <code>String</code>, or null if unset.
	 */
	public String getSourcePath(){return sourcePath;}
	
	/**
	 * Get all source data paths associated with this node. In most cases,
	 * this method will return a size 1 list containing the only source path.
	 * <br>However, patchwork nodes that reference multiple disk files will have
	 * multiple source paths. This method will retrieve all of them in no predictable
	 * order.
	 * @return <code>Collection</code> containing all source paths referenced by this
	 * node.
	 */
	public Collection<String> getAllSourcePaths(){
		List<String> col = new ArrayList<String>(1);
		if(this.sourcePath != null) col.add(this.sourcePath);
		return col;
	}
	
	/**
	 * Get whether this node is flagged as having a virtual source - ie. the data source
	 * is another <code>FileNode</code> rather than a file on disk.
	 * @return True if the virtual source flag is set, false if not.
	 */
	public boolean hasVirtualSource(){return src_virtual;}
	
	/**
	 * Get the <code>FileNode</code> serving as a data source for a virtually sourced node.
	 * @return Virtual source node for this node, or null if unset or this node's source is physical.
	 */
	public FileNode getVirtualSource(){
		if(!src_virtual) return null;
		return sourceNode;
	}
	
	/**
	 * Get the offset in bytes from the beginning of this node's data source where this node's
	 * data begins. If this node has a container chain, this offset should be relative to the
	 * beginning of the processed data from the most interior container (last in chain).
	 * @return <code>FileNode</code> data offset relative to source.
	 */
	public long getOffset(){return offset;}
	
	/**
	 * Get the size in bytes of this node's data.
	 * @return <code>FileNode</code> data size.
	 */
	public long getLength(){return length;}
	
	/**
	 * Get the size of a loading block, if set.
	 * @return Loading block size, or -1 if none.
	 * @since 3.3.0
	 */
	public int getBlockSize(){
		return blocksize_out;
	}
	
	/**
	 * Get the size of a loading block relative
	 * to the output data, if set.
	 * @return Loading block size, or -1 if none.
	 * @since 3.3.1
	 */
	public int getOutputBlockSize(){
		return blocksize_out;
	}
	
	/**
	 * Get the size of a loading block relative
	 * to the input data, if set.
	 * @return Loading block size, or -1 if none.
	 * @since 3.3.1
	 */
	public int getInputBlockSize(){
		return blocksize_in;
	}
	
	/**
	 * Get timestamp for file node, if present.
	 * @return Node timestamp, or <code>null</code> if none.
	 * @since 3.6.0
	 */
	public ZonedDateTime getTimestamp(){return timestamp;}
	
	public DirectoryNode getParent(){return parent;}
	
	/**
	 * Get whether this <code>FileNode</code> is a link to another <code>FileNode</code>.
	 * @return True if node is a link, false if not.
	 */
	public boolean isLink(){return false;}
	
	/**
	 * Get the metadata value associated with the provided metadata key. 
	 * @param key <code>String</code> representing metadata field key.
	 * @return <code>String</code> metadata value associated with provided key, if value has been set for that
	 * key, or null if none mapped.
	 */
	public String getMetadataValue(String key){if(metadata == null){return null;}; return metadata.get(key);}
	
	/**
	 * Get whether this node has any metadata key/value pairs associated with it.
	 * @return True if this node has metadata, false if this node has no metadata.
	 */
	public boolean hasMetadata(){
		if(metadata == null) return false;
		return (!metadata.isEmpty());
	}
	
	/**
	 * Get an unordered list of all metadata keys associated with this node.
	 * @return List of node's metadata keys. List will be empty if this node has no metadata.
	 */
	public List<String> getMetadataKeys(){
		List<String> keys = new LinkedList<String>();
		if(metadata != null) keys.addAll(metadata.keySet());
		return keys;
	}
	
	/**
	 * Get whether this node is marked as having any encrypted regions at its source.
	 * @return True if this node is marked as encrypted, false otherwise.
	 */
	public boolean hasEncryption(){
		if(encryption_chain == null || encryption_chain.isEmpty()) return false;
		return true;
	}
	
	/**
	 * Get this node's <code>EncryptionDefinition</code> chain, if present.
	 * @return List of node's <code>EncryptionDefinition</code>s in processing order,
	 * or an empty list if none. Returned list is a copy, adding and removing nodes will not
	 * affect the node's encryption chain.
	 */
	public List<EncryptionDefinition> getEncryptionDefChain(){
		List<EncryptionDefinition> list = new LinkedList<EncryptionDefinition>();
		if(encryption_chain != null){
			for(EncryInfoNode n : encryption_chain) list.add(n.def);
		}
		return list;
	}
	
	/**
	 * Get the positions of the encrypted regions in the source data for this node,
	 * if present. 
	 * @return Matrix of offsets and lengths, containing position pair for each
	 * region (match w/ definition chain). First array index is region, second is
	 * offset/length (0 for offset, 1 for length. This method returns <code>null</node>
	 * if node has no encrypted regions marked.
	 */
	public long[][] getEncryptedRegions(){
		if(encryption_chain == null || encryption_chain.isEmpty()) return null;
		long[][] table = new long[encryption_chain.size()][2];
		int i = 0;
		for(EncryInfoNode n : encryption_chain){
			table[i][0] = n.offset;
			table[i][1] = n.length;
			i++;
		}
		return table;
	}
	
	/**
	 * Get whether this node has any compression definitions in its type chain
	 * (ie. has been marked as having a compression layer of formatting)
	 * @return Whether this node has a layer of compression formatting noted.
	 */
	public boolean hasCompression(){
		if(type_chain == null) return false;
		FileTypeNode tn = type_chain;
		while(tn != null){
			if(tn.isCompression()) return true;
			tn = tn.getChild();
		}
		return false;
	}
	
	/** Get whether the source data associated with this node is part of a compressed container 
	 * that needs to be decompressed before the node data can be accessed.
	 * @return True if node data are located in a compressed container, false otherwise.
	 */
	public boolean sourceDataCompressed(){
		return (container != null);
	}
	
	/**
	 * Get the container for this node. The container chain describes the
	 * wrapping data containers (usually compressed) that must be processed before
	 * this node's data can be accessed.
	 * @return FileNode describing container data location, if present.
	 * If this node has no container, <code>null</code> is returned.
	 */
	public FileNode getContainer(){return container;}
	
	/**
	 * Get the head of the type chain linked list associated with this node. The type chain
	 * defines the types associated with the linked data, types earlier in the chain wrap
	 * types later in the chain. Generally, types near the head of the chain are compression
	 * definitions, and the chain tail is the true format definition of the internal data.
	 * <br>The type chain, in contrast to the compression chain, applies to the data themselves
	 * as opposed to a wrapping container.
	 * @return The type chain head node, or null if no type definitions have been assigned to this node.
	 */
	public FileTypeNode getTypeChainHead(){return type_chain;}
	
	/**
	 * Get the tail of the type chain linked list associated with this node. The type chain
	 * defines the types associated with the linked data, types earlier in the chain wrap
	 * types later in the chain. Generally, types near the head of the chain are compression
	 * definitions, and the chain tail is the true format definition of the internal data.
	 * <br>The type chain, in contrast to the compression chain, applies to the data themselves
	 * as opposed to a wrapping container.
	 * @return The type chain tail node, or null if no type definitions have been assigned to this node.
	 */
	public FileTypeNode getTypeChainTail(){
		if(type_chain == null) return null;
		FileTypeNode child = type_chain;
		while(child.getChild() != null) child = child.getChild();
		
		return child;
	}
	
	/**
	 * Get the full type chain as a Java <code>LinkedList</code>. The type chain
	 * defines the types associated with the linked data, types earlier in the chain wrap
	 * types later in the chain. Generally, types near the head of the chain are compression
	 * definitions, and the chain tail is the true format definition of the internal data.
	 * <br>The type chain, in contrast to the compression chain, applies to the data themselves
	 * as opposed to a wrapping container.
	 * @return The full type chain contained in a <code>LinkedList</code>. If the type chain
	 * is empty, this method returns an empty list.
	 */
	public List<FileTypeNode> getTypeChainAsList(){
		List<FileTypeNode> list = new LinkedList<FileTypeNode>();
		if(type_chain != null){
			FileTypeNode node = type_chain;
			while(node != null){
				list.add(node);
				node = node.getChild();
			}
		}
		
		return list;
	}
	
	/**
	 * Get whether this node has any type definitions associated with it.
	 * @return True if this node has any type markings. False if not.
	 */
	public boolean hasTypingMark(){
		return (type_chain != null);
	}
	
	/**
	 * Get the current contents of the long scratch field. This field in a <code>FileNode</code>
	 * is meant to be used as a scratch field and is free to be used by any calling code. However, its
	 * contents should be assumed unpredictable unless explicitly set by the user.
	 * @return Contents of 8-byte scratch value.
	 */
	public long getScratchLong(){return scratch_long;}
	
	/**
	 * Get this node's GUID. It may or may not have or use one, as generation/setting
	 * must be done explicitly and isn't done upon construction.
	 * @return Node GUID, or -1L if unset.
	 */
	public long getGUID(){return guid;}
	
	/**
	 * Get the flags describing warnings or errors that arose on the last loadData() attempt
	 * for this node.
	 * @return Load failure flag field as an int. If this is 0, then the last load succeeded
	 * with no errors.
	 */
	public int getLoadFailureFlags(){
		return this.lfail_flags;
	}
	
	/**
	 * Get the string path to the temp file containing this node's data in a decrypted,
	 * decompressed state for use by nodes referencing wrapped data.
	 * @return The path to the temp file containing data for this container node.
	 */
	protected String getContainerTempPath(){
		return this.cont_temp;
	}
	
	public String toString(){
		if(FileNode.TOSTRING_FULLPATH) return getFullPath();
		else return getFileName();
	}
	
	/**
	 * Get the path on the local file system to the directory set for use
	 * as the temporary files directory for <code>FileNode</code> data loading
	 * and manipulation.
	 * @return <code>FileNode</code> temp files directory.
	 */
	public static String getTemporaryFilesDirectoryPath(){
		if(TEMP_DIR == null){
			try{
				TEMP_DIR = FileBuffer.getTempDir();
			}
			catch(Exception x){x.printStackTrace();}
		}
		return TEMP_DIR;
	}
	
	/* --- Setters --- */
	
	/**
	 * Set the local name of this node. Note that because <code>DirectoryNode</code>s map children
	 * by local name, this will cause remapping in the parent. As a result, any sibling nodes
	 * that have names identical to the new name will be have their mapping in the parent overwritten.
	 * @param name New name for node.
	 */
	public void setFileName(String name){
		String oldname = fileName;
		fileName = name;
		if(parent != null) parent.changeChildName(this, oldname);
	}
	
	/**
	 * Set the primary source data path for this node. 
	 * <br>If this node is a patchwork, this method will only alter the <code>FileNode</code>
	 * superclass field; pieces must be remapped individually.
	 * <br>If this node uses a virtual source, the path variable will be changed, but the linked
	 * source node (if set) will not. This field can then instead be used to save information
	 * about where to locate an unlinked or unloaded source node.
	 * @param path Path to set. Null and empty strings are accepted.
	 */
	public void setSourcePath(String path){sourcePath = path;}
	
	/**
	 * Set the virtual source flag for this node. If this flag is set, the data
	 * loader assumes the the source data is referenced by another <code>FileNode</code>
	 * and defers to that node. If the flag is unset, it uses this node's source path string
	 * to load data directly from a file on disk.
	 * @param b Flag value to set.
	 */
	public void setUseVirtualSource(boolean b){
		this.src_virtual = b;
		if(!b) sourceNode = null;
	}
	
	/**
	 * Set the <code>FileNode</code> to use as a virtual data source for this node.
	 * Calling this method and providing a non-null argument automatically sets the virtual
	 * source flag.
	 * @param vsource <code>FileNode</code> to set as a data source for this node.
	 */
	public void setVirtualSourceNode(FileNode vsource){
		sourceNode = vsource;
		if(sourceNode != null) src_virtual = true;
	}
	
	/**
	 * Set the offset value, in bytes, at which the data for this node begins relative
	 * to its data source.
	 * @param off Offset value, in bytes.
	 */
	public void setOffset(long off){offset = off;}
	
	/**
	 * Set the length in bytes of the data chunk referenced by this node.
	 * @param len Data length in bytes.
	 */
	public void setLength(long len){length = len;}
	
	/**
	 * Set a size for a loading block unit for these data. This can be used
	 * in cases of compression or encryption when data must be loaded in even
	 * blocks in order to properly read.
	 * <br>This only affects loading procedure. The end result of loadData()
	 * should remain the same.
	 * @param blockSize Size of desired load block, or -1 to unset block loading.
	 * @since 3.3.0
	 */
	public void setBlockSize(int blockSize){
		blocksize_out = blockSize;
		blocksize_in = blockSize;
	}
	
	/**
	 * Set sizes for input and output loading block units for these data.
	 * This can be used in cases of compression or encryption when data 
	 * must be loaded in even blocks in order to properly read.
	 * <br>This only affects loading procedure. The end result of loadData()
	 * should remain the same.
	 * @param in_block Size of a block from the data source.
	 * @param out_block Size of a block in file output.
	 * @since 3.3.1
	 */
	public void setBlockSize(int in_block, int out_block){
		blocksize_in = in_block;
		blocksize_out = out_block;
	}
	
	/**
	 * Generate a 64-bit GUID for this file node from a SHA-1 hash of the root-relative
	 * full file path.
	 */
	public void generateGUID(){
		String fullpath = getFullPath();
		byte[] sha1 = FileUtils.getSHA1Hash(fullpath.getBytes());
		guid = 0L;
		for(int i = 0; i < 8; i++){
			long b = Byte.toUnsignedLong(sha1[i]);
			guid |= (b << (i << 3));
		}
	}
	
	/**
	 * Directly set a 64-bit GUID for this file node. This method is intended for use
	 * by <code>FileTreeSaver</code> or any class that saves and loads <code>FileNode</code>
	 * information from disk.
	 * @param val Value to set as node GUID.
	 */
	protected void setGUID(long val){guid = val;}
	
	/**
	 * Timestamp this node to reflect the current time.
	 * @since 3.6.0
	 */
	public void timestamp(){
		timestamp = ZonedDateTime.now();
	}
	
	/**
	 * Set the timestamp field for this node to the provided parameter.
	 * @param time Time to set as timestamp for node.
	 * @since 3.6.0
	 */
	public void setTimestamp(ZonedDateTime time){
		timestamp = time;
	}
	
	/**
	 * Set an encryption definition describing the encryption type for this node's source data.
	 * @param def <code>EncryptionDefinition</code> describing source data encryption.
	 */
	public void addEncryption(EncryptionDefinition def){
		if(encryption_chain == null) encryption_chain = new LinkedList<EncryInfoNode>();
		EncryInfoNode n = new EncryInfoNode(def, 0L, getLength());
		encryption_chain.add(n);
	}
	
	/**
	 * Mark an encrypted region in the node by provided the encryption definition and location.
	 * @param def <code>EncryptionDefinition</code> describing source data encryption.
	 * @param offset The offset in bytes, relative to the start of the node, where the encrypted
	 * region beings.
	 * @param length The length in bytes of the encrypted region.
	 */
	public void addEncryption(EncryptionDefinition def, long offset, long length){
		if(encryption_chain == null) encryption_chain = new LinkedList<EncryInfoNode>();
		EncryInfoNode n = new EncryInfoNode(def, offset, length);
		encryption_chain.add(n);
	}
	
	/**
	 * Set a metadata value for the provided key to the provided value. This method
	 * is also used to map new metadata key-value pairs.
	 * @param key Metadata key <code>String</code>.
	 * @param value Metadata value to be associated with the provided key. Can be any <code>String</code>.
	 */
	public void setMetadataValue(String key, String value){
		if(metadata == null) metadata = new HashMap<String, String>();
		metadata.put(key, value);
	}
	
	/**
	 * Remove a metadata key/value pair from this node's local metadata map.
	 * @param key Key of metadata entry to remove.
	 * @since 3.0.0
	 */
	public void clearMetadataValue(String key){
		if(metadata == null) return;
		metadata.remove(key);
	}
	
	/**
	 * Erase all metadata key-value mappings from this node. Keys and values
	 * themselves will also be erased, so be careful.
	 */
	public void clearMetadata(){
		metadata.clear();
		metadata = null;
	}
	
	/**
	 * Set the node parent. As <code>FileNode</code> trees are double-linked, this
	 * not only sets the parent reference within this node to the argument <code>DirectoryNode</code>,
	 * but adds this node as a child to the argument node. The previous parent, if there is one,
	 * is unlinked.
	 * @param p <code>DirectoryNode</code> to set as new parent.
	 */
	public void setParent(DirectoryNode p) {
		if(parent != null) parent.removeChild(this);
		parent = p; 
		if(p != null) p.addChild(this);
	}
	
	/**
	 * Hide this node from its parent node. This effectively allows for
	 * a temporary tree dismount of this node.
	 * <br>Internally, the parent link is moved to another field and the parent
	 * field is set to null (removing this node from the parent's child map).
	 * @since 3.2.0
	 */
	public void hideFromParent(){
		hidden_parent = parent;
		this.setParent(null);
	}
	
	/**
	 * Restore the link between this node and a previously hidden parent. If no
	 * hidden parent is found, this method does nothing. If a hidden parent is found,
	 * the parent of this node will be set to that parent (and any existing parent will
	 * be delinked).
	 * <br>This method can be used to remount a node that has been temporarily dismounted
	 * from its tree.
	 * @since 3.2.0
	 */
	public void restoreToParent(){
		if(hidden_parent == null) return;
		this.setParent(hidden_parent);
		hidden_parent = null;
	}
	
	/**
	 * Add a container node to the tail of the container chain, describing
	 * how to open and read a container wrapping the source data for this node.
	 * <br><b>!! IMPORTANT !! </b> When any variant of the data loading method
	 * is called on this node, any wrapping containers as marked by the container chain
	 * will be decompressed, decrypted, and copied to a temporary file first. 
	 * This process assumes that the entire container needs to be "opened" for the
	 * target data to be accessed and will thus copy & decompress the <i>full data chunk</i>
	 * specified by the container node. If this container is large, and only part of it
	 * needs to be processed to access the data referenced by this node, that needs to be
	 * specified by the container node itself, ie. by adjusting the offset/length values
	 * to only specify the required data chunk.
	 * @param container_node <code>FileNode</code> describing wrapping container.
	 */
	public void setContainerNode(FileNode container_node){
		container = container_node;
	}
		
	/**
	 * Set the head of this node's type chain (describing format(s) of data
	 * referenced by node as a wrapper chain).
	 * <br><b>WARNING: </b> This method DOES NOT push the new node to the existing
	 * type chain, it REPLACES the existing type chain head.
	 * @param head New node to set as type chain head. Null value is acceptable.
	 */
	public void setTypeChainHead(FileTypeNode head){type_chain = head;}
	
	/**
	 * Push a type definition node to the head of this node's type chain. The previous
	 * head becomes the child of the parameter type node.
	 * @param type New type node to push to head. If null, this method returns without
	 * doing anything.
	 */
	public void pushTypeChainHead(FileTypeNode type){
		if(type == null) return;
		type.setChild(type_chain);
		type_chain = type;
	}
	
	/**
	 * Add a type definition node to the tail of this node's type chain. If there is
	 * no existing type chain, this node also becomes the head.
	 * <br>Note that some type nodes cannot link children. If that is the case, the new node
	 * will not be added.
	 * @param type New type node to add to the type chain tail. If null, this method returns without
	 * doing anything.
	 */
	public void addTypeChainNode(FileTypeNode type){
		if(type == null) return;
		FileTypeNode tail = this.getTypeChainTail();
		if(tail != null) tail.setChild(type);
		else type_chain = type;
	}
	
	/**
	 * Clear and remove the type definition chain from this node.
	 */
	public void clearTypeChain(){type_chain = null;}
	
	/**
	 * Set value for the 64bit long scratch field. This field in a <code>FileNode</code>
	 * is meant to be used as a scratch field and is free to be used by any calling code. However, its
	 * contents should be assumed unpredictable unless explicitly set by the user.
	 * @param val Value to set for scratch field.
	 */
	public void setScratchLong(long val){scratch_long = val;}
	
	/**
	 * Set the string path to the temp file containing this node's data in a decrypted,
	 * decompressed state for use by nodes referencing wrapped data.
	 * @param path Path to set as temp path.
	 */
	protected void setContainerTempPath(String path){
		this.cont_temp = path;
	}
	
	/**
	 * Set whether <code>FileNode.toString()</code> should return full paths for
	 * file nodes (root relative) or just the local node names.
	 * <br>By default, root relative paths are returned.
	 * @param b True to use full paths, false to use only local names.
	 */
	public static void setUseFullPathInToString(boolean b){
		FileNode.TOSTRING_FULLPATH = b;
	}
	
	/**
	 * Set the <code>FileNode</code> sort order. Sort order options are available as constant
	 * ints in the <code>FileNode</code> class. The default sort order is standard file system sort
	 * order (directories alphabetical, then leaves alphabetical).
	 * @param sort_order Sort order to set.
	 * @see <code>waffeoRai_Files.tree.FileNode</code>
	 */
	public static void setSortOrder(int sort_order){
		SORT_BY = sort_order;
	}

	/**
	 * Set the path on the local file system to the directory set for use
	 * as the temporary files directory for <code>FileNode</code> data loading
	 * and manipulation.
	 * @param path Local FS relative path to use as temporary files directory
	 * for <code>FileNode</code> methods.
	 */
	public static void setTemporaryFilesDirectoryPath(String path){
		TEMP_DIR = path;
	}
	
	/* --- Cleanup --- */
	
	/**
	 * Release all resources and memory used by this file node, any virtual sources,
	 * and containers. This includes data buffers held in memory and on disk.
	 * @since 3.6.0
	 */
	public void dispose(){
		clearTempFiles(); //Automatically takes care of container.
		if(sourceNode != null) sourceNode.clearTempFiles();
	}
	
	/**
	 * Clear any temp files this node has used as intermediates for loading
	 * source data.
	 * @return The number of files successfully deleted.
	 */
	public int clearTempFiles(){
		if(temp_paths == null) return 0;
		int count = 0;
		for(String p : temp_paths){
			try{
				Files.deleteIfExists(Paths.get(p));
				count++;
			}
			catch(IOException x){x.printStackTrace();}
		}
		temp_paths.clear();
		temp_paths = null; //Throw to GC
		
		//Container temp
		if(cont_temp != null){
			if(FileBuffer.fileExists(cont_temp)){
				try{
					Files.delete(Paths.get(cont_temp));
					count++;
				}
				catch(IOException x){x.printStackTrace();}
			}
		}
		
		if(container != null){
			count += container.clearTempFiles();
		}
		
		return count;
	}
	
	/* --- Comparable --- */
	
	/**
	 * Get whether this node is a directory node. While some leaf <code>FileNode</code> subtypes
	 * can link to other <code>FileNode</code>s, a true directory contains other nodes as children
	 * and references no external data - it is solely an organizational element.
	 * <br>This <i>should</i> return the same value as <code>(this instanceof DirectoryNode)</code>, but is quicker
	 * to type if only querying.
	 * @return True if this node is a true directory. False if this node is a leaf.
	 */
	public boolean isDirectory(){return false;}
	
	public boolean equals(Object o){
		if(o == this) {return true;}
		if(o == null) return false;
		if(!(o instanceof FileNode)) return false;
		FileNode fn = (FileNode)o;
		if(this.isDirectory() != fn.isDirectory()) return false;
		return fileName.equals(fn.fileName);
	}
	
	public int hashCode(){
		return fileName.hashCode() ^ (int)offset;
	}
	
	public int compareTo(FileNode other){
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
	
	public TreeNode getChildAt(int childIndex) {return null;}

	public int getChildCount() {return 0;}

	public int getIndex(TreeNode node) {return -1;}

	public boolean getAllowsChildren() {return false;}

	public boolean isLeaf() {return true;}

	public Enumeration<TreeNode> children() {
		TreeNode[] n = null;
		return new Treenumeration(n);
	}
	
	/**
	 * Convert a Swing <code>TreePath</code> to a String describing the location
	 * of a node relative to the tree root. The returned string should be the same
	 * as what would be returned by the target's <code>getFullPath()</code> method,
	 * and can be easily used by <code>DirectoryNode.getNodeAt</code>.
	 * @param treepath Swing <code>TreePath</code> to convert.
	 * @return String representing the path in a <code>FileNode</code> tree pointing
	 * to the same target node as input <code>TreePath</code>.
	 */
	public static String readTreePath(TreePath treepath){
		if(treepath == null) return null;
		Object lasty = treepath.getLastPathComponent();
		if(lasty instanceof FileNode) return ((FileNode)lasty).getFullPath();
		return lasty.toString();
	}
	
	/**
	 * Generate a <code>FileNode</code> referencing a file on disk. 
	 * The generated <code>FileNode</code> by default has no parent <code>DirectoryNode</code>. 
	 * @param diskFilePath Path to file on disk to reference.
	 * @return Generated <code>FileNode</code> referring to file at the provided path, or 
	 * <code>null</code> if there is no file at the path or the path refers to a directory.
	 * @since 3.4.0
	 */
	public static FileNode createFileNodeOf(String diskFilePath){
		if(diskFilePath == null) return null;
		if(!FileBuffer.fileExists(diskFilePath)) return null;
		
		//Extract name.
		int seppos = diskFilePath.lastIndexOf(File.separatorChar);
		String fname = diskFilePath;
		if(seppos >= 0) fname = diskFilePath.substring(seppos+1);
		
		//Node
		FileNode node = new FileNode(null, fname);
		node.generateGUID();
		node.setSourcePath(diskFilePath);
		node.setOffset(0x0);
		node.setLength(FileBuffer.fileSize(diskFilePath));
		
		return node;
	}
	
	/* --- Other --- */
	
	/**
	 * Copy the instance variable data in this node to a fresh instance.
	 * The original and the copy should share no object references; the copy
	 * needs to be completely independent of the original.
	 * @param copy Node to copy instance data to.
	 */
	protected void copyDataTo(FileNode copy){
		
		copy.length = this.length;
		copy.offset = this.offset;
		copy.sourcePath = this.sourcePath;
		
		copy.src_virtual = this.src_virtual;
		copy.sourceNode = this.sourceNode;
		
		if(encryption_chain != null){
			copy.encryption_chain = new LinkedList<EncryInfoNode>();
			for(EncryInfoNode n : encryption_chain){
				copy.encryption_chain.add(new EncryInfoNode(n.def, n.offset, n.length));
			}
		}
		
		copy.container = this.container;
		
		if(metadata != null){
			Set<String> metakeys = metadata.keySet();
			for(String k : metakeys) copy.setMetadataValue(k, metadata.get(k));	
		}
		
		//Copy type chain
		if(type_chain != null) copy.type_chain = type_chain.copyChain();
		else copy.type_chain = null;
		
	}
	
	/**
	 * Copy the instance data in this node to a fresh node preserving no common
	 * object references and return the copy.
	 * @param parent_copy Copy of parent to link to new node.
	 * @return A copy of this node with no common references.
	 */
	public FileNode copy(DirectoryNode parent_copy){
		FileNode copy = new FileNode(parent_copy, this.fileName);
		copyDataTo(copy);
		return copy;
	}
	
	/**
	 * Copy the instance data in this node to a fresh node preserving no common
	 * object references and return the copy.
	 * @param parent_copy Copy of parent to link to new node.
	 * @param link_copy A copy of the node this node is linked to (if <code>LinkNode</code>)
	 * @return A copy of this node with no common references.
	 */
	public FileNode copy(DirectoryNode parent_copy, FileNode link_copy){
		return copy(parent_copy);
	}
	
	/**
	 * Cull the encryption regions for this node to those that fall between
	 * the specified positions. This is used for generating split/ sub nodes.
	 * @param stpos Start coordinate of region relative to prior start position.
	 * @param len Length of region.
	 */
	protected void subsetEncryptionRegions(long stpos, long len){
		if(encryption_chain == null) return;
		
		List<EncryInfoNode> refchain = encryption_chain;
		encryption_chain = new LinkedList<EncryInfoNode>();
		long edpos = stpos + len;
		
		for(EncryInfoNode en : refchain){
			//If starts after region...
			if(en.offset >= edpos) continue;
			//If ends before region...
			long eend = en.offset + en.length;
			if(eend < stpos) continue;
			
			//Trim ends to fit.
			long nst = en.offset - stpos;
			if(nst < 0L) nst = 0L;
			long ed = edpos<eend?edpos:eend;
			long ned = ed - stpos;
			long rlen = ned - nst;
			
			encryption_chain.add(new EncryInfoNode(en.def, nst, rlen));
		}
		
	}
	
	/**
	 * Split this node at the specified offset (in bytes, relative to node data start)
	 * into two fresh nodes and replace this node in the tree with the two new nodes.
	 * @param off Offset relative to node start, in bytes, at which split should occur.
	 * @return True if split was successful, false if not. If split is successful, new
	 * nodes will be mounted to parent node in place of this node.
	 */
	public boolean splitNodeAt(long off){
		if(off >= this.length) return false;
		if(off < 0) return false;
		
		long rel_off = off + this.offset;
		long l1 = off;
		long l2 = length - off;
		
		String myname = getFileName();
		FileNode n1 = new FileNode(parent, myname + ".front");
		FileNode n2 = new FileNode(parent, myname + ".back");
		
		copyDataTo(n1);
		copyDataTo(n2);
		
		n1.offset = offset;
		n1.setLength(l1);

		n2.offset = rel_off;
		n2.setLength(l2);
		
		if(encryption_chain != null){
			
			//Front node
			n1.encryption_chain.clear();
			for(EncryInfoNode en : encryption_chain){
				if(en.offset >= off) continue;
				long ed = en.offset + en.length;
				long l = en.length;
				if(ed > off){
					//Ends after end of node.
					l = ed - off;
				}
				n1.encryption_chain.add(new EncryInfoNode(en.def, en.offset, l));
			}
			
			//Back node
			n2.encryption_chain.clear();
			for(EncryInfoNode en : encryption_chain){
				long ed = en.offset + en.length;
				if(ed < off) continue; //Ends before this half
				long o = 0;
				long l = en.length;
				if(en.offset > off){
					o = en.offset - off;
				}
				else l -= off - en.offset;
				n2.encryption_chain.add(new EncryInfoNode(en.def, o, l));
			}
			
		}

		setParent(null);
		
		return true;
	}
	
	/**
	 * Get a new <code>FileNode</code> that references a subset of the data
	 * referenced by this <code>FileNode</code>. The returned node is a copy
	 * will a <code>null</code> parent.
	 * @param stoff Start offset of new node relative to the start of this node.
	 * @param len Length of new node.
	 * @return Newly generated sub-node.
	 */
	public FileNode getSubFile(long stoff, long len){
		FileNode child = copy(null);
		long num = stoff ^ len;
		child.setFileName(getFileName() + "-subnode-" + Long.toHexString(num));
		if(stoff < 0) stoff = 0;
		if(len < 0) len = 0;
		
		long ed = stoff + len;
		if(ed > getLength()) ed = getLength();
		child.setOffset(this.getOffset() + stoff);
		child.setLength(ed - stoff);
		
		if(encryption_chain != null){
			child.encryption_chain.clear();
			for(EncryInfoNode en : encryption_chain){
				if(en.offset >= ed) continue;
				long ened = en.offset + en.length;
				if(ened <= stoff) continue;
				
				long eoff = en.offset - stoff;
				if(eoff < 0) eoff = 0;
				ened -= eoff;
				if(ened > len) ened = len;
				long elen = ened - eoff;
				EncryInfoNode enew = new EncryInfoNode(en.def, eoff, elen);
				child.encryption_chain.add(enew);
			}
		}
		
		return child;
	}
	
	/**
	 * Get a <code>String</code> describing the location of the node
	 * relative to its data source. Usually, this will just be the start
	 * and end offsets, but for more complicated nodes, this may be longer.
	 * @return A short <code>String</code> describing node location.
	 */
	public String getLocationString(){
		if(this.isDirectory()) return "Directory";
		StringBuilder sb = new StringBuilder(1024);
		
		if(sourceDataCompressed()){
			if(container != null){
				sb.append("Decomp From: 0x" + Long.toHexString(container.getOffset()) + " -> ");
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
	
	/**
	 * Search the tree this node is linked to for a node meeting a specific condition
	 * starting with this node. If found, return the path as a <code>String</code>
	 * relative to this node to the first node that meets the condition.
	 * <br>This method starts by looking at the siblings of this node, and if it finds
	 * none that meet the condition, it steps back one parental level. In essence, it searches
	 * for the most <i>closely related</i> or <i>nearest</i> node to this one that meets the
	 * condition.
	 * @param cond <code>NodeMatchCallback</code> wrapping method that determines whether
	 * a node passes the filter.
	 * @return Path relative to this node to first node meeting condition, if there is one.
	 * <code>null</code> if no matches are found.
	 */
	public String findNodeThat(NodeMatchCallback cond){
		if(parent != null) return parent.findNodeThat(cond);
		else return null;
	}
	
	/* --- Load --- */
	
	private void noteTempPath(String path){
		if(temp_paths == null) temp_paths = new LinkedList<String>();
		temp_paths.add(path);
	}
	
	private String genTempPathForContainer(){
		if(container == null) return null;
		String temppath = container.getContainerTempPath();
		if(temppath == null){
			Random r = new Random(getGUID()); //For temp file names
			temppath = getTemporaryFilesDirectoryPath() + File.separator + Long.toHexString(r.nextLong()) + ".tmp";
		}
		noteTempPath(temppath);
		return temppath;
	}
	
	/**
	 * Process any wrapping containers to generate a new temporary
	 * file node referencing the decompressed data for this node. If this node
	 * has no wrapping, this method will simply return this node.
	 * <br>This method always uses the provided path on the local file system (usually
	 * referencing disk) as a data buffer, essentially creating a temp file.
	 * @param path Path to use for disk buffer file.
	 * @param options Load option flags. <code>LOADOP_FORCE_CACHE</code> and
	 * <code>LOADOP_DISK_BUFFER</code> are overridden to true in this method.
	 * @return <code>FileNode</code> for loading data source (eg. decompressed container) for this
	 * file.
	 * @throws IOException If there is an error reading any source files or creating
	 * any temp files.
	 * @since 3.6.1
	 */
	protected FileNode generateSourceUsingDiskBuffer(String path, int options) throws IOException{
		if(container == null) return this;
		if(path == null){
			lfail_flags |= LOADFAIL_FLAG_NO_DISKBUFF_PATH;
			return null;
		}
		
		FileNode node = new FileNode(null, "");
		node.setOffset(this.getOffset());
		node.setLength(this.getLength());
		node.setSourcePath(path);
		
		//If this file has not already been generated, try to load.
		if(!FileBuffer.fileExists(path)){
			FileBuffer buff = container.loadDecompressedData(path, options | LOADOP_FORCE_CACHE | LOADOP_DISK_BUFFER);
			if(buff != null) buff.dispose();
			if(FileBuffer.fileExists(path)){
				container.setContainerTempPath(path);
				noteTempPath(path);
			}
		}
		
		return node;
	}
	
	/**
	 * Process any wrapping containers to generate a new temporary
	 * file node referencing the decompressed data for this node. If this node
	 * has no wrapping, this method will simply return this node.
	 * @param options Load option flags
	 * @return Node referencing plaintext or decompressed data from which this node's
	 * data can be accessed.
	 * @throws IOException If there is an error reading any source files or creating
	 * any temp files.
	 * @see LOADOP_DISK_BUFFER
	 */
	protected FileNode generateSource(int options) throws IOException{
		if((options & LOADOP_DISK_BUFFER) != 0){
			return generateSource(genTempPathForContainer(), options);
		}
		return generateSource(null, options);
	}

	/**
	 * Process any wrapping containers to generate a new temporary
	 * file node referencing the decompressed data for this node. If this node
	 * has no wrapping, this method will simply return this node.
	 * @param bufferPath Path on local file system to use as buffer in case disk buffer is requested.
	 * @param options Load option flags
	 * @return Node referencing plaintext or decompressed data from which this node's
	 * data can be accessed.
	 * @throws IOException If there is an error reading any source files or creating
	 * any temp files.
	 * @see LOADOP_DISK_BUFFER
	 */
	protected FileNode generateSource(String bufferPath, int options) throws IOException{
		FileNode node = this;
		
		//Handle containers
		if(container != null){
			if((options & LOADOP_DISK_BUFFER) != 0){
				//Buffer to disk
				return generateSourceUsingDiskBuffer(bufferPath, options);
			}
			else{
				//Buffer to memory
				FileBuffer buff = container.loadDecompressedData(null, options);
				if(buff != null){
					MemFileNode cont = new MemFileNode(null, getFileName() + "_container");
					cont.setData(buff);
					node = cont;
				}
			}
			
			//Flags
			if((container.lfail_flags & FileNode.LOADFAIL_FLAG_DATA_DECRYPT) != 0){
				lfail_flags |= FileNode.LOADFAIL_FLAG_CNTNR_DECRYPT;
			}
			if((container.lfail_flags & FileNode.LOADFAIL_FLAG_DATA_DECOMP) != 0){
				lfail_flags |= FileNode.LOADFAIL_FLAG_CNTNR_DECOMP;
			}
		}
		
		return node;
	}
	
	/**
	 * Load the data referenced by the node directly as-is with no fancy processing. This
	 * method should only be called by <code>loadData</code>, and should be overridden by
	 * all subclasses that reference source data differently.
	 * This is the method that actually loads the target data into a <code>FileBuffer</code>.
	 * @param stpos Position in bytes, relative to start of node, to start data load. 
	 * @param len Number of bytes from node to load.
	 * @param forceCache Whether or not to instantiate all <code>FileBuffer</code> containers
	 * as <code>CacheFileBuffer</code> regardless of size. This can be useful in the case of
	 * patchwork/fragmented nodes where loaded data can consist of many smaller pieces that
	 * may add up to a very large total.
	 * @param decrypt Whether or not to attempt decryption of node data. If not, the
	 * returned node will either be this node or reference the open container.
	 * @return <code>FileBuffer</code> wrapping the resulting loaded data.
	 * @throws IOException If there is an error reading from/writing to disk during the load attempt
	 * or temp file creation.
	 * @deprecated As of 3.6.0 -- Use <code>loadDirect(long stpos, long len, int options)</code>
	 */
	protected FileBuffer loadDirect(long stpos, long len, boolean forceCache, boolean decrypt) throws IOException{
		int options = LOADOP_NONE;
		if(forceCache) options |= LOADOP_FORCE_CACHE;
		if(decrypt) options |= LOADOP_DECRYPT;
		return loadDirect(stpos, len, options);
	}
	
	/**
	 * Load the data referenced by the node directly as-is with no fancy processing. This
	 * method should only be called by <code>loadData</code>, and should be overridden by
	 * all subclasses that reference source data differently.
	 * This is the method that actually loads the target data into a <code>FileBuffer</code>.
	 * @param stpos Position in bytes, relative to start of node, to start data load. 
	 * @param len Number of bytes from node to load.
	 * @param options Option flags
	 * @return <code>FileBuffer</code> wrapping the resulting loaded data.
	 * @throws IOException If there is an error reading from/writing to disk during the load attempt
	 * or temp file creation.
	 * @since 3.6.0
	 * @see LOADOP_FORCE_CACHE
	 * @see LOADOP_DECRYPT
	 */
	protected FileBuffer loadDirect(long stpos, long len, int options) throws IOException{
		//This is the function that should be overridden by child classes.
		long stoff = getOffset() + stpos;
		long maxed = getOffset() + getLength();
		long edoff = stoff + len;
		if(blocksize_out <= 1 && edoff > maxed) edoff = maxed;
		//if(edoff > maxed) edoff = maxed;
		
		if(hasVirtualSource()){
			return sourceNode.loadData(stoff, edoff-stoff, options);
		}
		else{
			String path = getSourcePath();
			if((options & LOADOP_FORCE_CACHE) != 0){
				return CacheFileBuffer.getReadOnlyCacheBuffer(path, 0x8000, 128, stoff, edoff);
			}
			else{
				return FileBuffer.createBuffer(path, stoff, edoff);
			}
		}
	}
	
	/**
	 * Load the data referenced by the node (and specified positions) into a FileBuffer container for
	 * easy random access.
	 * <br>If this node has a container chain, this method will try to decompress/decrypt the surrounding
	 * container to a temporary file in order to access the source data correctly (otherwise offset
	 * may be incorrect).
	 * <br>If this node's <code>EncryptionDefinition</code> is non-null, the <code>decrypt</code>
	 * parameter is true, and there is an appropriate <code>StaticDecryptor</code>, this method
	 * will attempt to run decryption on the incoming data as well.
	 * <br>This method otherwise loads data from the source as-is; it does no type formatting, 
	 * processing, or internal decompression.
	 * @param stpos Position in bytes, relative to start of node, to start data load. 
	 * @param len Number of bytes from node to load.
	 * @param options Load option flags
	 * @param bufferPath Path on local file system to use for data buffers (unwrapping, decryption, etc.),
	 * if disk buffer option is set.
	 * @return <code>FileBuffer</code> wrapping the resulting loaded data.
	 * @throws IOException If there is an error reading from/writing to disk during the load attempt
	 * or temp file creation.
	 * @since 3.6.0
	 * @see LOADOP_FORCE_CACHE
	 * @see LOADOP_DECRYPT
	 * @see LOADOP_DISK_BUFFER
	 */
	public FileBuffer loadData(long stpos, long len, int options, String bufferPath) throws IOException{
		//Reset error flags
		load_flag_decwrap_direct = false;
		lfail_flags = 0;
		if(this.isDirectory()){
			lfail_flags |= FileNode.LOADFAIL_FLAG_ISDIR;
			return null;
		}
		
		//Block align offsets. 
		// This is to ensure initial load doesn't try to start mid-block.
		long o_stpos = stpos; //Output start position
		long o_len = len; //Output len
		int b_start = -1; //Index of start block
		int b_end = -1; //Index of end block
		if(blocksize_out > 1){
			long ed = stpos + len;
			b_start = (int)(stpos/blocksize_out);
			b_end = (int)(ed/blocksize_out);
			if(ed % blocksize_out != 0) b_end++;
			
			//Adjust stpos and len to be aligned to output blocks
			stpos = (long)b_start * (long)blocksize_out;
			long edpos = (long)b_end * (long)blocksize_out;
			len = edpos - stpos;
		}
		
		//Unwrap (this handles containers)
		FileNode src = generateSource(bufferPath, options);
		
		//Load data from unwrapped source
		long i_stpos = stpos;
		long i_len = len;
		if(b_start >= 0){
			//Adjust source offsets to reflect input block size
			i_stpos = (long)b_start * (long)blocksize_in;
			long i_edpos = (long)b_end * (long)blocksize_in;
			i_len = i_edpos - i_stpos;
		}
		
		//Direct-load the source data as-is (don't apply decryption processing)
		FileBuffer data = src.loadDirect(i_stpos, i_len, options);
		
		//Wrap buffer w/ decryption buffer if wanted/needed
		if(((options & LOADOP_DECRYPT) != 0) && hasEncryption() && !load_flag_decwrap_direct){
			boolean edone = false;
			int ecount = encryption_chain.size();
			if(ecount == 1){
				//See if the one entry covers the whole file...
				//If so, just wrap data as-is
				EncryInfoNode ereg = encryption_chain.getFirst();
				long eend = ereg.offset + ereg.length; long dend = stpos + len;
				if(ereg.offset <= stpos && eend >= dend){
					//The one encryption region covers the whole chunk we extracted.
					StaticDecryptor sdec = StaticDecryption.getDecryptorState(ereg.def.getID());
					if(sdec != null){
						DecryptorMethod decm = sdec.generateDecryptor(this);
						if(stpos != 0) decm.adjustOffsetBy(stpos);
						EncryptedFileBuffer ebuff = new EncryptedFileBuffer(data, decm);
						data = ebuff;
						edone = true;
					}
					else{
						lfail_flags |= FileNode.LOADFAIL_FLAG_DATA_DECRYPT;
						edone = true;
					}
				}
			}
			
			if(!edone){
				//Chain regions together...
				MultiFileBuffer multi = new MultiFileBuffer((ecount << 1) + 1);
				long dend = stpos + len;
				long pos = stpos;
				int eadded = 0;
				for(EncryInfoNode ereg : encryption_chain){

					long eend = ereg.offset + ereg.length;
					if(eend <= stpos) continue; //This region is before the requested data
					if(ereg.offset >= dend) break; //This region is after the end of requested data

					//Get anything between the position and the region start...
					if(ereg.offset > pos){
						FileBuffer chunk = data.createCopy(pos-stpos, ereg.offset - stpos);
						multi.addToFile(chunk);
						pos = ereg.offset;
					}
					
					//Do region
					if(eend > dend) eend = dend;
					FileBuffer chunk = data.createCopy(ereg.offset-stpos, eend - stpos);
					StaticDecryptor sdec = StaticDecryption.getDecryptorState(ereg.def.getID());
					if(sdec != null){
						DecryptorMethod decm = sdec.generateDecryptor(this);
						decm.adjustOffsetBy(stpos + ereg.offset);
						EncryptedFileBuffer ebuff = new EncryptedFileBuffer(chunk, decm);
						multi.addToFile(ebuff);
					}
					else{
						lfail_flags |= FileNode.LOADFAIL_FLAG_DATA_DECRYPT;
						multi.addToFile(chunk);
					}
					eadded++;
					pos = eend;
				}
				
				if(eadded > 0) data = multi;
			}
		}
		
		if(b_start >= 0){
			//Check if we need to make a RO sub buffer
			stpos = (long)b_start * (long)blocksize_out;
			
			//Make an RO Sub Buffer
			long o_ed = o_stpos + o_len;
			
			long l_st = o_stpos - stpos;
			long l_ed = o_ed - stpos;
			
			data = data.createReadOnlyCopy(l_st, l_ed);
		}

		return data;
	}
	
	/**
	 * Load the data referenced by the node (and specified positions) into a FileBuffer container for
	 * easy random access.
	 * <br>If this node has a container chain, this method will try to decompress/decrypt the surrounding
	 * container to a temporary file in order to access the source data correctly (otherwise offset
	 * may be incorrect).
	 * <br>If this node's <code>EncryptionDefinition</code> is non-null, the <code>decrypt</code>
	 * parameter is true, and there is an appropriate <code>StaticDecryptor</code>, this method
	 * will attempt to run decryption on the incoming data as well.
	 * <br>This method otherwise loads data from the source as-is; it does no type formatting, 
	 * processing, or internal decompression.
	 * @param stpos Position in bytes, relative to start of node, to start data load. 
	 * @param len Number of bytes from node to load.
	 * @param options Option flags
	 * @return <code>FileBuffer</code> wrapping the resulting loaded data.
	 * @throws IOException If there is an error reading from/writing to disk during the load attempt
	 * or temp file creation.
	 * @since 3.6.0
	 * @see LOADOP_FORCE_CACHE
	 * @see LOADOP_DECRYPT
	 * @see LOADOP_DISK_BUFFER
	 */
	protected FileBuffer loadData(long stpos, long len, int options) throws IOException{
		return loadData(stpos, len, options, genTempPathForContainer());
	}
	
	/**
	 * Load the data referenced by the node (and specified positions) into a FileBuffer container for
	 * easy random access.
	 * <br>If this node has a container chain, this method will try to decompress/decrypt the surrounding
	 * container to a temporary file in order to access the source data correctly (otherwise offset
	 * may be incorrect).
	 * <br>If this node's <code>EncryptionDefinition</code> is non-null, the <code>decrypt</code>
	 * parameter is true, and there is an appropriate <code>StaticDecryptor</code>, this method
	 * will attempt to run decryption on the incoming data as well.
	 * <br>This method otherwise loads data from the source as-is; it does no type formatting, 
	 * processing, or internal decompression.
	 * @param stpos Position in bytes, relative to start of node, to start data load. 
	 * @param len Number of bytes from node to load.
	 * @param forceCache Whether or not to instantiate all <code>FileBuffer</code> containers
	 * as <code>CacheFileBuffer</code> regardless of size. This can be useful in the case of
	 * patchwork/fragmented nodes where loaded data can consist of many smaller pieces that
	 * may add up to a very large total.
	 * @param decrypt Whether or not to attempt decryption on loaded data, as specified by
	 * this node's encryption chain. Container decryption will be attempted regardless of whether
	 * this parameter is set.
	 * @return <code>FileBuffer</code> wrapping the resulting loaded data.
	 * @throws IOException If there is an error reading from/writing to disk during the load attempt
	 * or temp file creation.
	 * @deprecated As of 3.6.0 -- Use <code>loadData(long stpos, long len, int options)</code>
	 */
	protected FileBuffer loadData(long stpos, long len, boolean forceCache, boolean decrypt) throws IOException{
		int options = LOADOP_NONE;
		if(forceCache) options |= LOADOP_FORCE_CACHE;
		if(decrypt) options |= LOADOP_DECRYPT;
		return loadData(stpos, len, options);
	}
	
	/**
	 * Load the data referenced by the node (and specified positions) into a FileBuffer container for
	 * easy random access.
	 * <br>If this node has a container chain, this method will try to decompress/decrypt the surrounding
	 * container to a temporary file in order to access the source data correctly (otherwise offset
	 * may be incorrect).
	 * <br>If this node's <code>EncryptionDefinition</code> is non-null, the <code>decrypt</code>
	 * parameter is true, and there is an appropriate <code>StaticDecryptor</code>, this method
	 * will attempt to run decryption on the incoming data as well.
	 * <br>This method otherwise loads data from the source as-is; it does no type formatting, 
	 * processing, or internal decompression.
	 * @param stpos Position in bytes, relative to start of node, to start data load. 
	 * @param len Number of bytes from node to load.
	 * @param decrypt Whether or not to attempt decryption on loaded data, as specified by
	 * this node's encryption chain. Container decryption will be attempted regardless of whether
	 * this parameter is set.
	 * @return <code>FileBuffer</code> wrapping the resulting loaded data.
	 * @throws IOException If there is an error reading from/writing to disk during the load attempt
	 * or temp file creation.
	 * @deprecated As of 3.6.0 -- Use <code>loadData(long stpos, long len, int options)</code>
	 */
	public FileBuffer loadData(long stpos, long len, boolean decrypt) throws IOException{
		int options = LOADOP_NONE;
		if(decrypt) options |= LOADOP_DECRYPT;
		return loadData(stpos, len, options);
	}
	
	/**
	 * Load the data referenced by the node (and specified positions) into a FileBuffer container for
	 * easy random access.
	 * <br>If this node has a container chain, this method will try to decompress/decrypt the surrounding
	 * container to a temporary file in order to access the source data correctly (otherwise offset
	 * may be incorrect).
	 * <br>If this node's <code>EncryptionDefinition</code> is non-null, the <code>decrypt</code>
	 * parameter is true, and there is an appropriate <code>StaticDecryptor</code>, this method
	 * will attempt to run decryption on the incoming data as well.
	 * <br>This method otherwise loads data from the source as-is; it does no type formatting, 
	 * processing, or internal decompression.
	 * @param stpos Position in bytes, relative to start of node, to start data load. 
	 * @param len Number of bytes from node to load.
	 * @return <code>FileBuffer</code> wrapping the resulting loaded data.
	 * @throws IOException If there is an error reading from/writing to disk during the load attempt
	 * or temp file creation.
	 */
	public FileBuffer loadData(long stpos, long len) throws IOException{
		return loadData(stpos, len, LOADOP_NONE);
	}
	
	/**
	 * Load the data referenced by the node into a FileBuffer container for
	 * easy random access, with specified options.
	 * <br>If this node has a container chain, and flag is set, this method will try to decompress/decrypt the surrounding
	 * container to a temporary file in order to access the source data correctly (otherwise offset
	 * may be incorrect).
	 * <br>If this node's <code>EncryptionDefinition</code> is non-null, the decrypt option flag is set, 
	 * and there is an appropriate <code>StaticDecryptor</code>, this method
	 * will attempt to run decryption on the incoming data as well.
	 * <br>This method otherwise loads data from the source as-is; it does no type formatting, 
	 * processing, or internal decompression.
	 * @param options Load option flags
	 * @return <code>FileBuffer</code> wrapping the resulting loaded data.
	 * @throws IOException If there is an error reading from/writing to disk during the load attempt
	 * or temp file creation.
	 * @since 3.6.0
	 * @see LOADOP_DECOMPRESS
	 * @see LOADOP_DECRYPT
	 * @see LOADOP_DISK_BUFFER
	 * @see LOADOP_FORCE_CACHE
	 */
	public FileBuffer loadData(int options) throws IOException{
		return loadData(0, getLength(), options);
	}
	
	/**
	 * Load the data referenced by the node into a FileBuffer container for
	 * easy random access.
	 * <br>If this node has a container chain, this method will try to decompress/decrypt the surrounding
	 * container to a temporary file in order to access the source data correctly (otherwise offset
	 * may be incorrect).
	 * <br>If this node's <code>EncryptionDefinition</code> is non-null, the <code>decrypt</code>
	 * parameter is true, and there is an appropriate <code>StaticDecryptor</code>, this method
	 * will attempt to run decryption on the incoming data as well.
	 * <br>This method otherwise loads data from the source as-is; it does no type formatting, 
	 * processing, or internal decompression.
	 * @return <code>FileBuffer</code> wrapping the resulting loaded data.
	 * @throws IOException If there is an error reading from/writing to disk during the load attempt
	 * or temp file creation.
	 */
 	public FileBuffer loadData() throws IOException{
		return loadData(0, getLength(), LOADOP_DECRYPT);
	}
	
 	/**
 	 * Load the data referenced by this node, apply any decompression specified by the
 	 * compression definitions in the format type chain, and return the resulting decompressed
 	 * data in a <code>FileBuffer</code> wrapper for easy random access.
 	 * @param bufferPath Path for temp file to write decompressed version of data to, if disk buffer is requested.
 	 * @param options Option flags
 	 * @return <code>FileBuffer</code> wrapping the resulting loaded data.
 	 * @throws IOException If there is an error reading from/writing to disk during the load attempt
	 * or temp file creation.
 	 * @since 3.6.1
 	 * @see LOADOP_FORCE_CACHE
	 * @see LOADOP_DECRYPT
	 * @see LOADOP_DISK_BUFFER
 	 */
 	protected FileBuffer loadDecompressedData(String bufferPath, int options) throws IOException{
 		FileBuffer buffer = loadData(0L, getLength(), options | LOADOP_DECRYPT);
		
 		//This looks for compression specified for THIS node - NOT containers
 		//	(ie. when node is a file in a compressed archive)
		FileTypeNode typechain = getTypeChainHead();
		while(typechain != null){
			if(typechain.isCompression()){
				if(typechain instanceof CompDefNode){
					AbstractCompDef def = ((CompDefNode)typechain).getDefinition();
					
					if((options & LOADOP_DISK_BUFFER) != 0){
						if(bufferPath == null) {
							lfail_flags |= LOADFAIL_FLAG_NO_DISKBUFF_PATH;
							return null;
						}
						boolean okay = def.decompressToDiskBuffer(buffer.getReferenceAt(0L), bufferPath, CompressionDefs.DECOMP_OP_NONE);
						if(!okay) return null;
						
						if((options & LOADOP_FORCE_CACHE) != 0){
							buffer = CacheFileBuffer.getReadOnlyCacheBuffer(bufferPath);
						}
						else buffer = FileBuffer.createBuffer(bufferPath);
					}
					else{
						buffer = def.decompressToMemory(buffer.getReferenceAt(0L), -1, CompressionDefs.DECOMP_OP_NONE);
						if(buffer == null){
							lfail_flags |= LOADFAIL_FLAG_MEMBUFF_TOOBIG;
						}
					}
				}
				
				typechain = typechain.getChild();
			}
			else break;
		}
		
		//System.err.println("Returning buffer of size 0x" + Long.toHexString(buffer.getFileSize()));
		return buffer;
 	}
 	
 	/**
 	 * Load the data referenced by this node, apply any decompression specified by the
 	 * compression definitions in the format type chain, and return the resulting decompressed
 	 * data in a <code>FileBuffer</code> wrapper for easy random access.
 	 * @param options Option flags
 	 * @return <code>FileBuffer</code> wrapping the resulting loaded data.
	 * @throws IOException If there is an error reading from/writing to disk during the load attempt
	 * or temp file creation.
	 * @since 3.6.0
	 * @see LOADOP_FORCE_CACHE
	 * @see LOADOP_DECRYPT
	 * @see LOADOP_DISK_BUFFER
 	 */
 	protected FileBuffer loadDecompressedData(int options) throws IOException{
 		return loadDecompressedData(genTempPathForContainer(), options);
 	}
 	
 	/**
 	 * Load the data referenced by this node, apply any decompression specified by the
 	 * compression definitions in the format type chain, and return the resulting decompressed
 	 * data in a <code>FileBuffer</code> wrapper for easy random access.
 	 * @param forceCache Whether or not to instantiate all <code>FileBuffer</code> containers
	 * as <code>CacheFileBuffer</code> regardless of size. This can be useful in the case of
	 * patchwork/fragmented nodes where loaded data can consist of many smaller pieces that
	 * may add up to a very large total.
 	 * @return <code>FileBuffer</code> wrapping the resulting loaded data.
	 * @throws IOException If there is an error reading from/writing to disk during the load attempt
	 * or temp file creation.
	 * @deprecated As of 3.6.0 -- Use <code>loadDecompressedData(int options)</code>
 	 */
 	protected FileBuffer loadDecompressedData(boolean forceCache) throws IOException{
 		int options = LOADOP_NONE;
		if(forceCache) options |= LOADOP_FORCE_CACHE;
		return loadDecompressedData(null, options);
	}
 	
 	/**
 	 * Load the data referenced by this node, apply any decompression specified by the
 	 * compression definitions in the format type chain, and return the resulting decompressed
 	 * data in a <code>FileBuffer</code> wrapper for easy random access.
 	 * @return <code>FileBuffer</code> wrapping the resulting loaded data.
	 * @throws IOException If there is an error reading from/writing to disk during the load attempt
	 * or temp file creation.
 	 */
	public FileBuffer loadDecompressedData() throws IOException{
		return loadDecompressedData(LOADOP_NONE);
	}
	
	/**
	 * Copy the data referenced in this node to a file on disk.
	 * @param path Path of output file on local file system.
	 * @param options Option flags
	 * @return True if copy was successful, false otherwise.
	 * @throws IOException If there is an error reading or writing from disk at any point.
	 * @since 3.6.0
	 * @see LOADOP_DECOMPRESS
	 * @see LOADOP_DECRYPT
	 * @see LOADOP_DISK_BUFFER
	 * @see LOADOP_FORCE_CACHE
	 */
	public boolean copyDataTo(String path, int options) throws IOException{
		if(path == null || path.isEmpty()) return false;
		
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path));
		copyDataTo(bos, options);
		bos.close();
		
		return true;
	}
	
	/**
	 * Copy the data referenced in this node to a file on disk.
	 * @param path Path of output file on local file system.
	 * @param decompress Whether or not to attempt auto-decompression.
	 * @return True if copy was successful, false otherwise.
	 * @throws IOException If there is an error reading or writing from disk at any point.
	 * @deprecated As of 3.6.0 -- Use <code>copyDataTo(String path, int options)</code>
	 */
	public boolean copyDataTo(String path, boolean decompress) throws IOException{
		int options = LOADOP_NONE;
		if(decompress) options |= LOADOP_DECOMPRESS;
		return copyDataTo(path, options);
	}
	
	/**
	 * Copy the data referenced by this node to the specified output stream.
	 * @param out Output stream to write data to.
	 * @param options Option flags
	 * @return True if copy was successful, false otherwise.
	 * @throws IOException If there is an error reading or writing from disk at any point.
	 * @since 3.6.0
	 * @see LOADOP_DECOMPRESS
	 * @see LOADOP_DECRYPT
	 * @see LOADOP_DISK_BUFFER
	 * @see LOADOP_FORCE_CACHE
	 */
	public boolean copyDataTo(OutputStream out, int options) throws IOException{
		if(out == null) return false;
		
		FileBuffer dat = null;
		if((options & LOADOP_DECOMPRESS) != 0) dat = loadDecompressedData(options);
		else dat = loadData();
		dat.writeToStream(out);
		
		return true;
	}
	
	/**
	 * Copy the data referenced by this node to the specified output stream.
	 * @param out Output stream to write data to.
	 * @param decompress Whether or not to attempt auto-decompression.
	 * @return True if copy was successful, false otherwise.
	 * @throws IOException If there is an error reading or writing from disk at any point.
	 * @deprecated As of 3.6.0 -- Use <code>copyDataTo(OutputStream out, int options)</code>
	 */
	public boolean copyDataTo(OutputStream out, boolean decompress) throws IOException{
		int options = LOADOP_NONE;
		if(decompress) options |= LOADOP_DECOMPRESS;
		return copyDataTo(out, options);
	}
	
	/* --- Debug --- */
	
	/**
	 * Debug print a line representing this <code>FileNode</code> to stderr.
	 * @param indents Number of tab characters to insert at beginning of line.
	 */
	public void printMeToStdErr(int indents) {
		StringBuilder sb = new StringBuilder(128);
		for(int i = 0; i < indents; i++) sb.append("\t");
		String tabs = sb.toString();
		
		String off = "0x" + Long.toHexString(offset);
		String end = "0x" + Long.toHexString(offset + length);
		
		System.err.println(tabs + "->" + this.fileName + " (" + off + " - " + end + ")");
	}
	
	/**
	 * Print a line representing this <code>FileNode</code> to the specified <code>Writer</code>.
	 * @param out Output target.
	 * @param indents Number of tab characters to insert at beginning of line.
	 * @throws IOException If there is an error writing to the <code>Writer</code>.
	 */
	public void printMeTo(Writer out, int indents) throws IOException{
		StringBuilder sb = new StringBuilder(128);
		for(int i = 0; i < indents; i++) sb.append("\t");
		String tabs = sb.toString();
		
		//String off = "0x" + Long.toHexString(offset);
		//String end = "0x" + Long.toHexString(offset + length);
		
		out.write(tabs + "->" + this.fileName + " (" + getLocationString() + ")\n");	
	}
	
	protected String getTypeString(){return "FileNode";}
	protected String getOffsetString(){return "0x" + Long.toHexString(getOffset());}
	protected String getLengthString(){return "0x" + Long.toHexString(getLength());}
	
	protected void printReferenceTrace(int indents){
		StringBuilder sb = new StringBuilder(indents+1);
		for(int i = 0; i < indents; i++) sb.append('\t');
		String tabs = sb.toString();
		
		//Print me.
		System.err.println(tabs + getFullPath());
		System.err.println(tabs + "UID: " + Long.toHexString(getGUID()));
		System.err.println(tabs + "Class: " + getTypeString());
		if(hasVirtualSource()) System.err.println(tabs + "Source: <virtual>");
		else System.err.println(tabs + "Source: " + getSourcePath());
		System.err.println(tabs + "Offset: " + getOffsetString());
		System.err.println(tabs + "Length: " + getLengthString());
		if(blocksize_in != -1) System.err.println(tabs + "Input Blocksize: 0x" + Integer.toHexString(blocksize_in));
		if(blocksize_out != -1) System.err.println(tabs + "Output Blocksize: 0x" + Integer.toHexString(blocksize_out));
		System.err.println(tabs + "Is Encrypted: " + hasEncryption());
		System.err.println(tabs + "Is Compressed: " + hasCompression());
		System.err.println(tabs + "Inside Compressed Container: " + sourceDataCompressed());
		
		//Print compression container chain
		if(container != null){
			System.err.println(tabs + "Compressed Container: ");
			container.printReferenceTrace(indents+1);
		}
		
		//Print source chain
		if(sourceNode != null){
			System.err.println(tabs + "Virtual Source Node: ");
			sourceNode.printReferenceTrace(indents+1);
		}
		
	}
	
	/**
	 * Print details of this node and its data sources recursively to stderr.
	 * @since 3.5.0
	 */
	public void printReferenceTrace(){
		printReferenceTrace(0);	
	}
	
}
