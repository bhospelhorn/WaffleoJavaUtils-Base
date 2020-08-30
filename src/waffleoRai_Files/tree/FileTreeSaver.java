package waffleoRai_Files.tree;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import waffleoRai_Compression.definitions.AbstractCompDef;
import waffleoRai_Compression.definitions.CompDefNode;
import waffleoRai_Compression.definitions.CompressionDefs;
import waffleoRai_Files.EncryptionDefinition;
import waffleoRai_Files.EncryptionDefinitions;
import waffleoRai_Files.FileClass;
import waffleoRai_Files.FileDefinitions;
import waffleoRai_Files.FileTypeDefNode;
import waffleoRai_Files.FileTypeDefinition;
import waffleoRai_Files.FileTypeNode;
import waffleoRai_Utils.BinFieldSize;
import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.SerializedString;
import waffleoRai_Utils.FileBuffer.UnsupportedFileTypeException;

public class FileTreeSaver {
	
	/*Big Endian
	 * 
	 * Magic "tReE" [4]
	 * Flags [2] (V7+)
	 * 	15 - Has Tree
	 * 	14 - Has List
	 * Version [2] (4 bytes in V1-V6)
	 * Offset to source path table [4]
	 * Offset to tree start [4]
	 * Offset to list start [4] (V7+)
	 * (RESERVED) [12] (V7+)
	 * 
	 * Source Path Table (So don't have to repeat over and over...)
	 * 	Block Magic "SPBt" [4]
	 * 	# Of paths in table [4]
	 * 		Offset from "SPBt" to string [4 * n]
	 * 
	 * 		Path [VLS 2x2 * n]
	 * 
	 * List
	 * 	Block Magic "tsil" [4] (V7+)
	 * 	Node count [4] (V7+)
	 * 
	 * Tree
	 * 	Block Magic "eert" [4]
	 * 		Nodes, starting with root (followed serially by children)
	 * 
	 * F/D Node
	 * 	Flags [2]
	 * 		OLD FLAGS ---
	 * 			15 - Is Directory?
	 * 			14 - Is Link?
	 * 			13 - Has metadata? (V3+)
	 * 			12 - Is CD Node? (V5+)
	 * 			11 - Is fragmented node? (V6+)
	 * 			10 - Is patchwork node? (V7+)
	 * 		 	7 - Source compressed? (If file)
	 * 		 	6 - Has type chain [file]/file class[dir] (V2+)(V4+ for dir)
	 * 		 	5 - Has encryption (V2+)
	 * 		FLAGS (V7+) ---
	 * 			15 - Is Directory
	 * 			14 - (Reserved)
	 * 			13 - Has metadata?
	 * 			11-8 - FileNode type enum (4 bits)
	 * 				0 - Regular
	 * 				1 - Link Node
	 * 				2 - CD/Disc Node
	 * 				3 - Fragmented Node
	 * 				4 - Patchwork Node
	 * 				5 - Complex Patchwork Node
	 * 			7 - Has container? (File)
	 * 			6 - Has type chain [file]/file class[dir] (V2+)(V4+ for dir)
	 * 			5 - Has encryption (V2+)
	 * 			4 - Data source is virtual (V7+)
	 * 			3 - Container link is UID (V7+)
	 * 				If not, container node is defined within this node
	 * 		
	 *  UID [8] (V7+)
	 *  Parent UID [8] (If in list mode) (V7+)
	 * 	Name [VLS 2x2] 
	 * 	Metadata [VLS 4x2] (V3+) (If applicable)
	 * 
	 * If file...
	 * 	Source Path Index [4]
	 * 	Source Node UID [8] (If applicable) (V7+)
	 * 	Offset [8]
	 * 	Length [8]
	 * 
	 * File subtype data....
	 * 	Block count [4] (If applicable) (V6+)
	 *	Block location data [(16 or 20)*blocks] (If applicable) (V6+)
	 *		Path Index [4] (If patchwork) (V7+)
	 *		Offset [8]
	 *		Size [8]
	 *	# Inner Nodes [4] (If applicable (complex patchwork)) (V7+)
	 *  Inner Nodes [var] (If applicable (complex patchwork)) (V7+)
	 *  Sector Size Data [8] (If applicable) (V5+)
	 *  	Data Size [4]
	 *  	Header Size [2]
	 *  	Footer Size [2]
	 *  
	 * File load data...
	 *  (Encryption)
	 *  	/DEP/ Encryption Def ID [4] (If applicable) (V2-V6)
	 *  	/DEP/ Encryption Start [8] (If applicable) (V4-V6)
	 *  	/DEP/ Encryption Region Size [8] (If applicable) (V4-V6)
	 *  	#Encryption Nodes [4] (If appl.) (V7+)
	 *  		Encryption Def ID [4] (V7+)
	 *  		Encryption Start [8] (V7+)
	 *  		Encryption Region Size [8] (V7+)
	 *  (Type chain)
	 *  	# Type chain nodes [4] (If applicable) (V2+)
	 *  	Type chain [4 * n] (If applicable) (V2+)
	 *  		ID [4] (First bit is always set if compression)
	 *  (Container)
	 * 		/DEP/ Compression Start Offset [8] (If applicable) [V1 only]
	 * 		/DEP/ #Compression Chain Nodes [4] (If applicable) (V2-V6)
	 * 		/DEP/ Compression Chain [20 * n] (If applicable) (V2-V6)
	 * 			Definition ID [4]
	 * 			Start offset[8]
	 * 			Length [8]
	 *		Container Node UID [8] (If appl.) (V7+)
	 *			If flags 7 and 3 are set.
	 *		Container Node Def... (If appl.) (V7+) 
	 *			If flag 7 is set but 3 is not.
	 * 
	 * If directory...
	 * 	Number of children [4] (Omitted in list mode)
	 * 	File class [2] (If applicable) (V4+)
	 * 
	 * If link...
	 * 	(If target is a file, then need (some) file fields.
	 * 		don't need dir fields if dir, though) 
	 * 	Offset of target (relative to "eert" or "tsil") [4]
	 * 
	 * 
	 * Metadata is stored as one giant string formatted as follows:
	 * KEY0=VALUE0;KEY1=VALUE1;KEY2= (etc.)
	 * Keys and values cannot contain the following characters: '=', ';'
	 * There can only be one string value per key
	 * 
	 * Starting with version 7, there can be an alternate block to "eert"-- "tsil"
	 * This is read iteravely, not recursively. Doesn't need to be a full tree, can
	 * be isolated leaves or branches.
	 * When in list mode, an extra field for parent UID is included. Set to -1L if unused.
	 * 
	 */
	
	/* ----- Constants ----- */
	
	public static final String MAGIC = "tReE";
	public static final String MAGIC_TABLE = "SPBt";
	public static final String MAGIC_TREE = "eert";
	public static final String MAGIC_LIST = "tsil";
	
	public static final int CURRENT_VERSION = 7;
	
	public static final String ENCODING = "UTF8";
	
	public static final String SCRUBBED_PATH_PLACEHOLDER = "<NA>";
	
	public static final int NODETYPE_STANDARD = 0;
	public static final int NODETYPE_LINK = 1;
	public static final int NODETYPE_DISK = 2;
	public static final int NODETYPE_FRAG = 3;
	public static final int NODETYPE_PATCHWORK = 4;
	public static final int NODETYPE_PATCHWORK_C = 5;
	
	public static final String METAKEY_SRCNODE_UID = "SRCNODE_UID";
	public static final String METAKEY_CNTRNODE_UID = "CNTRNODE_UID";
	
	/* ----- Structs ----- */
	
	private static class ParsedDir{
		public int size;
		public DirectoryNode node;
		
		public ParsedDir(DirectoryNode n, int s){
			node = n;
			size = s;
		}
	}
	
	private static class ParsedNode{
		public int size;
		public FileNode node;
		
		public ParsedNode(FileNode n, int s){
			node = n;
			size = s;
		}
	}
	
	/* ----- Parse ----- */
	
	private static ParsedDir parseDirNode(FileBuffer in, DirectoryNode parent, long stpos, String[] paths, Map<Integer, FileNode> offmap, int version, boolean listmode)
	{
		//System.err.println("FileTreeSaver.parseDirNode || Directory found -- stpos = 0x " + Long.toHexString(stpos));
		DirectoryNode dir = new DirectoryNode(parent, "");
		offmap.put((int)stpos, dir);
		
		long cpos = stpos;
		int flags = Short.toUnsignedInt(in.shortFromFile(cpos)); cpos+=2;
		boolean hasfc = false;
		
		//UIDs
		if(version >= 7){
			dir.setGUID(in.longFromFile(cpos)); cpos+=8;
			if(listmode) {dir.scratch_long = in.longFromFile(cpos); cpos+=8;}
		}

		//Get name...
		SerializedString ss = in.readVariableLengthString(ENCODING, cpos, BinFieldSize.WORD, 2);
		cpos += ss.getSizeOnDisk();
		dir.setFileName(ss.getString());
		//System.err.println("FileTreeSaver.parseDirNode || Dir Name: " + dir.getFileName());
		
		//Metadata
		if(version >= 3 && (flags & 0x2000) != 0)
		{
			ss = in.readVariableLengthString(ENCODING, cpos, BinFieldSize.DWORD, 2);
			cpos += ss.getSizeOnDisk();
			String metastr = ss.getString();
			
			String[] fields = metastr.split(";");
			for(String pair : fields)
			{
				String[] split = pair.split("=");
				if(split.length < 2) continue;
				dir.setMetadataValue(split[0], split[1]);
			}
		}
		if(version >= 4 && (flags & 0x0040) != 0) hasfc = true;
		
		//Get children
		int ccount = 0;
		if((version < 7) || !listmode) {ccount = in.intFromFile(cpos); cpos += 4;}
		if(hasfc){
			int fcval = Short.toUnsignedInt(in.shortFromFile(cpos)); cpos+=2;
			FileClass fc = FileClass.getFromInteger(fcval);
			dir.setFileClass(fc);
		}
		for(int i = 0; i < ccount; i++){
			int flags2 = Short.toUnsignedInt(in.shortFromFile(cpos));
			if((flags2 & 0x8000) != 0) cpos += parseDirNode(in, dir, cpos, paths, offmap, version, listmode).size;
			else cpos += parseFileNode(in, dir, cpos, paths, offmap, version, listmode).size;
		}
		
		return new ParsedDir(dir, ((int)(cpos-stpos)));
	}
	
	private static ParsedNode parseFileNode(FileBuffer in, DirectoryNode parent, long stpos, String[] paths, Map<Integer, FileNode> offmap, int version, boolean listmode)
	{
		//System.err.println("FileTreeSaver.parseFileNode || File found -- stpos = 0x " + Long.toHexString(stpos));
		long cpos = stpos;
		int flags = Short.toUnsignedInt(in.shortFromFile(cpos)); cpos+=2;
		
		int nodetype = NODETYPE_STANDARD;
		FileNode fn = null;
		if(version >= 7){
			nodetype = (flags >>> 8) & 0xF;
			switch(nodetype){
			case NODETYPE_STANDARD: fn = new FileNode(parent, ""); break;
			case NODETYPE_LINK: fn = new LinkNode(parent, null, ""); break;
			case NODETYPE_DISK: fn = new ISOFileNode(parent, ""); break;
			case NODETYPE_FRAG: fn = new FragFileNode(parent, ""); break;
			case NODETYPE_PATCHWORK: fn = new PatchworkFileNode(parent, ""); break;
			case NODETYPE_PATCHWORK_C: 
				PatchworkFileNode pfn = new PatchworkFileNode(parent, ""); 
				pfn.setComplexMode(true);
				fn = pfn;
				break;
			}
		}
		else{
			if(version >= 5 && (flags & 0x1000) != 0){
				fn = new ISOFileNode(parent, "");
			}
			else if(version >= 6 && (flags & 0x800) != 0){
				fn = new FragFileNode(parent, "");
			}
			else if ((flags & 0x4000) != 0){
				fn = new LinkNode(parent, null, "");
			}
			else fn = new FileNode(parent, "");
		}
		
		offmap.put((int)stpos, fn);
		
		//UIDs
		if(version >= 7){
			fn.setGUID(in.longFromFile(cpos)); cpos += 8;
			if(listmode) {fn.scratch_long = in.longFromFile(cpos); cpos += 8;}
		}
		
		//Name
		SerializedString ss = in.readVariableLengthString(ENCODING, cpos, BinFieldSize.WORD, 2);
		cpos += ss.getSizeOnDisk();
		fn.setFileName(ss.getString());
		//System.err.println("FileTreeSaver.parseFileNode || File Name: " + fn.getFileName());
		
		if(version >= 3 && (flags & 0x2000) != 0) {
			//Has metadata
			ss = in.readVariableLengthString(ENCODING, cpos, BinFieldSize.DWORD, 2);
			cpos += ss.getSizeOnDisk();
			String metastr = ss.getString();
			
			String[] fields = metastr.split(";");
			for(String pair : fields){
				String[] split = pair.split("=");
				if(split.length < 2) continue;
				fn.setMetadataValue(split[0], split[1]);
			}
		}
		
		
		//Node location
		int pathidx = in.intFromFile(cpos); cpos+=4;
		if(version >= 7 && (flags & 0x10) != 0){
			//Virtual source
			long srcuid = in.longFromFile(cpos); cpos+=8;
			fn.setUseVirtualSource(true);
			fn.setMetadataValue(METAKEY_SRCNODE_UID, Long.toHexString(srcuid));
		}
		
		fn.setOffset(in.longFromFile(cpos)); cpos += 8;
		fn.setLength(in.longFromFile(cpos)); cpos+=8;
		
		if(paths != null && pathidx >= 0 && pathidx < paths.length){
			fn.setSourcePath(paths[pathidx]);
		}
		
		//Type specific location data
		if(nodetype == NODETYPE_FRAG){
			FragFileNode ffn = (FragFileNode)fn;
			int block_count = in.intFromFile(cpos); cpos+=4;
			for(int i = 0; i < block_count; i++){
				long off = in.longFromFile(cpos); cpos+=8;
				long len = in.longFromFile(cpos); cpos+=8;
				ffn.addBlock(off, len);
			}
		}
		else if(nodetype == NODETYPE_PATCHWORK){
			PatchworkFileNode pfn = (PatchworkFileNode)fn;
			int block_count = in.intFromFile(cpos); cpos+=4;
			for(int i = 0; i < block_count; i++){
				int pidx = in.intFromFile(cpos); cpos+=4;
				long off = in.longFromFile(cpos); cpos+=8;
				long len = in.longFromFile(cpos); cpos+=8;
				String ppath = null;
				if(paths != null && pidx >= 0 && pidx < paths.length){
					ppath = paths[pidx];
				}
				pfn.addBlock(ppath, off, len);
			}
		}
		else if(nodetype == NODETYPE_PATCHWORK_C){
			PatchworkFileNode pfn = (PatchworkFileNode)fn;
			int block_count = in.intFromFile(cpos); cpos+=4;
			for(int i = 0; i < block_count; i++){
				ParsedNode pn = parseFileNode(in, null, cpos, paths, offmap, version, listmode);
				cpos += pn.size;
				pfn.addBlock(pn.node);
			}
		}
		else if (nodetype == NODETYPE_DISK){
			int dsize = in.intFromFile(cpos); cpos += 4;
			int hsize = in.shortFromFile(cpos); cpos += 2;
			int fsize = in.shortFromFile(cpos); cpos += 2;
			((ISOFileNode)fn).setSectorDataSizes(hsize, dsize, fsize);
		}
		
		//Encryption info
		if((flags & 0x20) != 0){
			
			int count = 1;
			if(version >= 7){
				count = in.intFromFile(cpos); cpos+=4;
			}

			for(int i = 0; i < count; i++){
				int defid = in.intFromFile(cpos); cpos += 4;
				EncryptionDefinition def = EncryptionDefinitions.getByID(defid);
				long enc_off = 0;
				long enc_sz = fn.getLength();
				
				if(version >= 4){
					enc_off = in.longFromFile(cpos); cpos+=8;
					enc_sz = in.longFromFile(cpos); cpos+=8;
				}
				
				fn.addEncryption(def, enc_off, enc_sz);
			}
			
		}
		
		//Type Chain
		if((flags & 0x40) != 0){
			int tcount = in.intFromFile(cpos); cpos += 4;
			FileTypeNode head = null;
			FileTypeNode node = null;
			for(int i = 0; i < tcount; i++){
				int id = in.intFromFile(cpos); cpos += 4;
				if((id & 0x80000000) != 0){
					//It's a compression.
					AbstractCompDef def = CompressionDefs.getCompressionDefinition(id);
					if(def != null)
					{
						CompDefNode cnode = new CompDefNode(def);
						if(node == null) {head = cnode; node = head;}
						else {node.setChild(cnode); node = cnode;}
					}
				}
				else{
					//It's a format
					FileTypeDefinition def = FileDefinitions.getDefinitionByID(id);
					if(def != null)
					{
						FileTypeDefNode fnode = new FileTypeDefNode(def);
						if(node == null) {head = fnode; node = head;}
						else {node.setChild(fnode); node = fnode;}
					}
				}
			}
			fn.setTypeChainHead(head);
		}
		
		//Container
		if((flags & 0x80) != 0){
			FileNode cntr = null;
			if(version < 2){
				//Just a "compression start offset"
				long cst = in.longFromFile(cpos); cpos+=8;
				cntr = new FileNode(null, fn.getFileName() + "-cntr");
				cntr.setSourcePath(fn.getSourcePath());
				cntr.setOffset(cst);
				cntr.setLength(fn.getLength() + (fn.getOffset() - cst));
			}
			else if(version >= 2 && version < 7){
				//Compression chain
				int chainsize = in.intFromFile(cpos); cpos+=4;
				String path = fn.getSourcePath();
				for(int i = 0; i < chainsize; i++){
					FileNode cpar = cntr;
					
					int defid = in.intFromFile(cpos); cpos+=4;
					cntr = new FileNode(null, fn.getFileName() + "-cntr" + i);
					cntr.setSourcePath(path);
					cntr.setOffset(in.longFromFile(cpos)); cpos+=8;
					cntr.setLength(in.longFromFile(cpos)); cpos+=8;
					cntr.addTypeChainNode(new CompDefNode(CompressionDefs.getCompressionDefinition(defid)));
					if(cpar != null) cntr.setContainerNode(cpar);
				}
			}
			else if(version >= 7){
				//Container
				//Either UID or recursive
				if((flags & 0x08) != 0){
					//Just a uid, link later
					long cuid = in.longFromFile(cpos); cpos+=8;
					fn.setMetadataValue(METAKEY_CNTRNODE_UID, Long.toHexString(cuid));
				}
				else{
					//Full node def
					ParsedNode pn = parseFileNode(in, null, cpos, paths, offmap, version, listmode);
					cpos += pn.size;
					fn.setContainerNode(pn.node);
				}
			}
			if(cntr != null) fn.setContainerNode(cntr);
		}
		
		//Link offset
		if(nodetype == NODETYPE_LINK){
			fn.scratch_field = in.intFromFile(cpos); cpos += 4;
		}
		
		return new ParsedNode(fn, ((int)(cpos - stpos)));
	}

	private static void resolveLinks(DirectoryNode dir, Map<Integer, FileNode> offmap, String[] pathtbl)
	{
		List<FileNode> children = dir.getChildren();
		for(FileNode child : children)
		{
			if(child instanceof DirectoryNode) resolveLinks((DirectoryNode)child, offmap, pathtbl);
			else if(child instanceof LinkNode)
			{
				//Do the actual linking...
				int targ_off = child.scratch_field;
				FileNode target = offmap.get(targ_off);
				if(target != null){
					((LinkNode)child).setLinkOnly(target);
				}
			}
			else
			{
				//Get src path
				if(child.scratch_field < 0) continue;
				child.setSourcePath(pathtbl[child.scratch_field]);
			}
		}
	}
	
	private static void mapNodesByGUID(DirectoryNode dir, Map<Long, FileNode> idmap){

		idmap.put(dir.getGUID(), dir);
		
		List<FileNode> children = dir.getChildren();
		for(FileNode child : children){
			if(child instanceof DirectoryNode){
				DirectoryNode dchild = (DirectoryNode)child;
				mapNodesByGUID(dchild, idmap);
			}
			else idmap.put(child.getGUID(), child);
		}
		
	}
	
	public static DirectoryNode loadTree(String inpath) throws IOException, UnsupportedFileTypeException{
		FileBuffer in = FileBuffer.createBuffer(inpath, true);
		long cpos = in.findString(0, 0x10, MAGIC);
		if(cpos < 0) throw new FileBuffer.UnsupportedFileTypeException("FileTreeSaver.loadTree || Magic number could not be found!");
		
		//For now, just skip most of header...
		cpos += 4; //Skip magic we just checked
		int hflags = Short.toUnsignedInt(in.shortFromFile(cpos)); cpos += 2;
		int version = Short.toUnsignedInt(in.shortFromFile(cpos)); cpos += 2;
		cpos += 4; //Offset of Source path table
		long toff = Integer.toUnsignedLong(in.intFromFile(cpos)); cpos += 4;
		long loff = toff;
		if(version >= 7){
			loff = Integer.toUnsignedLong(in.intFromFile(cpos)); cpos += 4;
			cpos += 12;
		}
		//System.err.println("Tree Offset: 0x" + Long.toHexString(toff));
		
		//Read the source path table...
		cpos += 4; //Skip "SPBt"
		int path_count = in.intFromFile(cpos); cpos+=4;
		String[] path_table = new String[path_count];
		//System.err.println("Path count: " + path_count);
		for(int i = 0; i < path_count; i++){
			int off = 16 + in.intFromFile(cpos); cpos += 4;
			long spos = Integer.toUnsignedLong(off);
			path_table[i] = in.readVariableLengthString(ENCODING, spos, BinFieldSize.WORD, 2).getString();
		}
		
		//Prep map
		Map<Long, FileNode> uidmap = new TreeMap<Long, FileNode>();
		
		//Read list...
		List<FileNode> nodelist = null;
		Map<Integer, FileNode> offmap = new HashMap<Integer, FileNode>();
		if((hflags & 0x4000) != 0){
			//Has list
			cpos = loff + 4; //Skip magic
			int ncount = in.intFromFile(cpos); cpos+=4;
			nodelist = new ArrayList<FileNode>(ncount+1);
			for(int i = 0; i < ncount; i++){
				int flags = Short.toUnsignedInt(in.shortFromFile(cpos));
				if((flags & 0x8000) != 0){
					ParsedDir pd = parseDirNode(in, null, cpos, path_table, offmap, version, true);
					cpos += pd.size;
					nodelist.add(pd.node);
					uidmap.put(pd.node.getGUID(), pd.node);
				}
				else{
					ParsedNode pn = parseFileNode(in, null, cpos, path_table, offmap, version, true);
					cpos += pn.size;
					nodelist.add(pn.node);
					uidmap.put(pn.node.getGUID(), pn.node);
				}
			}
			
			//Resolve initial links - so can reuse offmap
			for(FileNode node : nodelist){
				//Link
				if(node instanceof LinkNode){
					FileNode link = offmap.get(node.scratch_field);
					if(link != null) ((LinkNode) node).setLink(link);
				}
			}
		}
		
		
		//Read the tree...
		DirectoryNode root = null;
		offmap.clear();
		if(version < 7 || (hflags & 0x8000) != 0){
			cpos = toff + 4;
			root = parseDirNode(in, null, cpos, path_table, offmap, version, false).node;	
		}
		mapNodesByGUID(root, uidmap);
		
		//Resolve links...
		resolveLinks(root, offmap, path_table);
		
		//Parents...
		if(nodelist != null){
			for(FileNode node : nodelist){
				FileNode p = uidmap.get(node.scratch_long);
				if((p != null) && (p instanceof DirectoryNode)){
					node.setParent(DirectoryNode.castFileNode(p));
				}
			}
		}
		
		//Containers & virtual sources...
		List<FileNode> allnodes = new LinkedList<FileNode>();
		allnodes.addAll(uidmap.values());
		for(FileNode node : allnodes){
			String mdval = node.getMetadataValue(METAKEY_SRCNODE_UID);
			if(mdval != null){
				long uid = Long.parseUnsignedLong(mdval, 16);
				FileNode target = uidmap.get(uid);
				if(target != null) node.setVirtualSourceNode(target);
				
				node.clearMetadataValue(METAKEY_SRCNODE_UID);
			}
			
			mdval = node.getMetadataValue(METAKEY_CNTRNODE_UID);
			if(mdval != null){
				long uid = Long.parseUnsignedLong(mdval, 16);
				FileNode target = uidmap.get(uid);
				if(target != null) node.setContainerNode(target);
				
				node.clearMetadataValue(METAKEY_CNTRNODE_UID);
			}
		}
		
		return root;
	}
	
	public static Collection<FileNode> loadList(String inpath) throws IOException, UnsupportedFileTypeException{

		FileBuffer in = FileBuffer.createBuffer(inpath, true);
		long cpos = in.findString(0, 0x10, MAGIC);
		if(cpos < 0) throw new UnsupportedFileTypeException("FileTreeSaver.loadList || Magic number could not be found!");
		
		//For now, just skip most of header...
		cpos += 4; //Skip magic we just checked
		cpos += 2; //Flags
		int version = Short.toUnsignedInt(in.shortFromFile(cpos)); cpos += 2;
		cpos += 4; //Offset of Source path table
		long toff = Integer.toUnsignedLong(in.intFromFile(cpos)); cpos += 4;
		long loff = toff;
		if(version >= 7){
			loff = Integer.toUnsignedLong(in.intFromFile(cpos)); cpos += 4;
			cpos += 12;
		}
		
		//Read the source path table...
		cpos += 4; //Skip "SPBt"
		int path_count = in.intFromFile(cpos); cpos+=4;
		String[] path_table = new String[path_count];
		for(int i = 0; i < path_count; i++){
			int off = 16 + in.intFromFile(cpos); cpos += 4;
			long spos = Integer.toUnsignedLong(off);
			path_table[i] = in.readVariableLengthString(ENCODING, spos, BinFieldSize.WORD, 2).getString();
		}
		
		//Read the list...
		cpos = loff + 8;
		Map<Integer, FileNode> offmap = new HashMap<Integer, FileNode>();
		Map<Long, FileNode> idmap = new HashMap<Long, FileNode>();
		List<FileNode> nodes = new LinkedList<FileNode>();
		long filesize = in.getFileSize();
		while(cpos < filesize){
			int flags_preview = Short.toUnsignedInt(in.shortFromFile(cpos));
			if((flags_preview & 0x8000) != 0){
				ParsedDir d = parseDirNode(in, null, cpos, path_table, offmap, version, true);
				nodes.add(d.node);
				cpos += d.size;
				idmap.put(d.node.getGUID(), d.node);
			}
			else{
				ParsedNode f = parseFileNode(in, null, cpos, path_table, offmap, version, true);
				nodes.add(f.node);
				cpos += f.size;
				idmap.put(f.node.getGUID(), f.node);
			}
		}
		
		//Resolve links...
		for(FileNode node : nodes){
			if(node instanceof LinkNode){
				LinkNode ln = (LinkNode)node;
				FileNode link = offmap.get(ln.scratch_field);
				if(link != null) ln.setLink(link);
			}
			
			//Containers and virtual sources...
			String mdval = node.getMetadataValue(METAKEY_SRCNODE_UID);
			if(mdval != null){
				long uid = Long.parseUnsignedLong(mdval, 16);
				FileNode target = idmap.get(uid);
				if(target != null) node.setVirtualSourceNode(target);
				
				node.clearMetadataValue(METAKEY_SRCNODE_UID);
			}
			
			mdval = node.getMetadataValue(METAKEY_CNTRNODE_UID);
			if(mdval != null){
				long uid = Long.parseUnsignedLong(mdval, 16);
				FileNode target = idmap.get(uid);
				if(target != null) node.setContainerNode(target);
				
				node.clearMetadataValue(METAKEY_CNTRNODE_UID);
			}
		}
		
		return nodes;
	}
	
	public static int loadNodes(String inpath, DirectoryNode mountTarget) throws IOException, UnsupportedFileTypeException{

		Collection<FileNode> nodelist = loadList(inpath);
		
		//Map node GUIDs
		Map<Long, FileNode> idmap = new TreeMap<Long, FileNode>();
		for(FileNode fn : nodelist) idmap.put(fn.getGUID(), fn);
		mapNodesByGUID(mountTarget, idmap);
		
		//Link parents and mount
		for(FileNode fn : nodelist){
			long pid = fn.scratch_long;
			if(pid == -1L) continue;
			
			FileNode parent = idmap.get(pid);
			if(parent == null) continue;
			if(!(parent instanceof DirectoryNode)) continue;
			DirectoryNode dparent = (DirectoryNode)parent;
			fn.setParent(dparent);
		}
		
		return nodelist.size();
	}
	
	/* ----- Serialization ----- */
	
	private static void mapRefNodes(Map<Long, FileNode> map, FileNode node){
		FileNode ref = node.getContainer();
		if(ref != null){
			if(ref.getGUID() == -1L) ref.generateGUID();
			map.put(ref.getGUID(), ref);
			mapRefNodes(map, ref);
		}
		
		ref = node.getVirtualSource();
		if(ref != null){
			if(ref.getGUID() == -1L) ref.generateGUID();
			map.put(ref.getGUID(), ref);
			mapRefNodes(map, ref);
		}
	}
	
	private static void saveRefNodePaths(Set<String> set, FileNode node){
		FileNode ref = node.getContainer();
		if(ref != null){
			set.add(ref.getSourcePath());
			saveRefNodePaths(set, ref);
		}
		
		ref = node.getVirtualSource();
		if(ref != null){
			set.add(ref.getSourcePath());
			saveRefNodePaths(set, ref);
		}
	}
	
	private static void getSourcePaths(Set<String> set, DirectoryNode dir)
	{
		List<FileNode> children = dir.getChildren();
		for(FileNode child : children){
			if(child.isDirectory()){
				if(child instanceof DirectoryNode){
					getSourcePaths(set, (DirectoryNode)child);
				}
			}
			else{
				if(child.getSourcePath() != null) set.add(child.getSourcePath());
				if(child instanceof PatchworkFileNode) set.addAll(((PatchworkFileNode)child).getAllSourcePaths());
				saveRefNodePaths(set, child);
			}
		}
	}
	
	private static class DatNode
	{
		private FileNode src;
		
		private FileBuffer dat;
		private List<DatNode> children;
		
		public DatNode()
		{
			children = new LinkedList<DatNode>();
		}
		
		public long getTotalSize()
		{
			long sz = dat.getFileSize();
			for(DatNode child : children) sz += child.getTotalSize();
			return sz;
		}
		
		public void writeToStream(OutputStream out) throws IOException
		{
			//out.write(dat.getBytes());
			dat.writeToStream(out);
			for(DatNode child : children) child.writeToStream(out);
		}
		
	}

	private static String serializeMetadata(FileNode node)
	{
		List<String> keylist = node.getMetadataKeys();
		StringBuilder sb = new StringBuilder(2048);
		
		boolean first = true;
		for(String key : keylist)
		{
			if(!first)sb.append(';');
			first = false;
			
			String value = node.getMetadataValue(key);
			sb.append(key + "=" + value);
		}
		
		return sb.toString();
	}
	
	private static DatNode serializeFileNode(FileNode node, Map<String, Integer> srcPathMap, boolean includeParentID, boolean refcontainers){

		if(node.getGUID() == -1L) node.generateGUID();
		
		//Serialize node name
		FileBuffer cname = new FileBuffer(3 + (node.getFileName().length()<< 1), true);
		cname.addVariableLengthString(ENCODING, node.getFileName(), BinFieldSize.WORD, 2);
		
		//Serialize metadata
		FileBuffer meta = null;
		if(node.hasMetadata()){
			String str = serializeMetadata(node);
			meta = new FileBuffer(5 + (str.length() << 1), true);
			meta.addVariableLengthString(ENCODING, str, BinFieldSize.DWORD, 2);
		}
		
		//Serialize container, if applicable
		FileBuffer cont_serial = null;
		if(!refcontainers && node.sourceDataCompressed()){
			cont_serial = serializeFileNode(node.getContainer(), srcPathMap, includeParentID, false).dat;
		}
		
		//Set flags and estimate size for allocation
		List<FileTypeNode> typechain = node.getTypeChainAsList();
		int flag = 0;
		int csz = 28 + (int)cname.getFileSize() + 20;
		if(node.sourceDataCompressed()){
			if(cont_serial != null) csz += cont_serial.getFileSize();
			else csz+=8;
			flag |= 0x80;
			if(refcontainers) flag |= 0x08;
		}
		if(node.hasEncryption()){
			int ecount = node.getEncryptionDefChain().size();
			csz += 20 * ecount; 
			flag |= 0x20;
		}
		if(!typechain.isEmpty()){csz += (4 + (typechain.size() << 2)); flag |= 0x40;}
		if(node.hasMetadata()){csz += meta.getFileSize(); flag |= 0x2000;}
		if(node.hasVirtualSource()) flag |= 0x10;
		
		int nodetype = NODETYPE_STANDARD;
		if(node instanceof LinkNode) nodetype = NODETYPE_LINK;
		else if(node instanceof ISOFileNode) nodetype = NODETYPE_DISK;
		else if(node instanceof FragFileNode) nodetype = NODETYPE_FRAG;
		else if(node instanceof PatchworkFileNode){
			if(((PatchworkFileNode)node).complexMode()) nodetype = NODETYPE_PATCHWORK_C;
			else nodetype = NODETYPE_PATCHWORK;
		}
		flag |= (nodetype & 0xF) << 8;
		
		//Allocate & Add initial fields
		DatNode datnode = new DatNode();
		FileBuffer cdat = new FileBuffer(csz, true);
		datnode.src = node;
		datnode.dat = cdat;
		
		cdat.addToFile((short)flag);
		cdat.addToFile(node.getGUID());
		
		if(includeParentID){
			DirectoryNode parent = node.getParent();
			if(parent == null) cdat.addToFile(-1L);
			else{
				if(parent.getGUID() == -1L) parent.generateGUID();
				cdat.addToFile(parent.getGUID());
			}
		}
		
		cdat.addToFile(cname);
		if(meta != null) cdat.addToFile(meta);
		
		//If link to directory, don't need anything else.
		if(node instanceof LinkNode && node.isDirectory()) return datnode;
		
		//Location data
		Integer srcidx = 0;
		if(srcPathMap != null){
			srcidx = srcPathMap.get(node.getSourcePath());
			if(srcidx == null) srcidx = -1;
		}
		cdat.addToFile(srcidx);
		if(node.hasVirtualSource()){
			FileNode srcnode = node.getVirtualSource();
			if(srcnode == null) cdat.addToFile(-1L);
			else{
				if(srcnode.getGUID() == -1L) srcnode.generateGUID();
				cdat.addToFile(srcnode.getGUID());
			}
		}
		cdat.addToFile(node.getOffset());
		cdat.addToFile(node.getLength());
		
		//Node type specific location data
		if(node instanceof FragFileNode){
			FragFileNode ffn = (FragFileNode)node;
			List<long[]> blocks = ffn.getBlocks();
			cdat.addToFile(blocks.size());
			for(long[] block : blocks){
				cdat.addToFile(block[0]);
				cdat.addToFile(block[1]);
			}
		}
		
		if(node instanceof PatchworkFileNode){
			PatchworkFileNode pfn = (PatchworkFileNode)node;
			List<FileNode> blocks = pfn.getBlocks();
			cdat.addToFile(blocks.size());
			if(pfn.complexMode()){
				for(FileNode block : blocks){
					datnode.children.add(serializeFileNode(block, srcPathMap, includeParentID, refcontainers));
				}
			}
			else{
				for(FileNode block : blocks){
					Integer psrcidx = -1;
					if(srcPathMap != null){
						psrcidx = srcPathMap.get(block.getSourcePath());
						if(psrcidx == null) psrcidx = -1;
					}
					cdat.addToFile(psrcidx);
					cdat.addToFile(block.getOffset());
					cdat.addToFile(block.getLength());
				}	
			}
		}
		
		if(node instanceof ISOFileNode){
			ISOFileNode isochild = (ISOFileNode)node;
			cdat.addToFile(isochild.getSectorDataSize());
			cdat.addToFile((short)isochild.getSectorHeadSize());
			cdat.addToFile((short)isochild.getSectorFootSize());
		}
		
		//Encryption
		if(node.hasEncryption()){
			List<EncryptionDefinition> edefs = node.getEncryptionDefChain();
			long[][] eregs = node.getEncryptedRegions();
			
			cdat.addToFile(edefs.size());
			int i = 0;
			for(EncryptionDefinition def : edefs){
				cdat.addToFile(def.getID());
				cdat.addToFile(eregs[i][0]);
				cdat.addToFile(eregs[i][1]);
				i++;
			}
		}
		
		//Type chain
		if(!typechain.isEmpty()){
			cdat.addToFile(typechain.size());
			for(FileTypeNode n : typechain){
				cdat.addToFile(n.getTypeID());
			}
		}
		
		//Compression container
		if(node.sourceDataCompressed()){
			if(cont_serial != null) cdat.addToFile(cont_serial);
			else{
				FileNode cont = node.getContainer();
				if(cont.getGUID() == -1L) cont.generateGUID();
				cdat.addToFile(cont.getGUID());
			}
		}

		return datnode;
	}

	private static DatNode serializeDir(DirectoryNode dir, Map<String, Integer> srcPathMap, Map<Integer, DatNode> offmap, int off, boolean refcontainers){
		//Note offset
		dir.scratch_field = off;
		
		//Ensure there is a GUID
		if(dir.getGUID() == -1L) dir.generateGUID();
		
		//Serialize just the name...
		FileBuffer nodename = null;
		if(dir.getParent() == null){
			//Root has empty name.
			nodename = new FileBuffer(2, true);
			nodename.addToFile((short)0);
		}
		else{
			nodename = new FileBuffer(3 + (dir.getFileName().length()<< 1), true);
			nodename.addVariableLengthString(ENCODING, dir.getFileName(), BinFieldSize.WORD, 2);
		}
		
		List<FileNode> children = dir.getChildren();
		
		//Serialize metadata...
		String metadata = null;
		if(dir.hasMetadata()) metadata = serializeMetadata(dir);
		
		int dsz = 10 + (int)nodename.getFileSize() + 4;
		if(metadata != null) dsz += 4 + 2+ metadata.length();
		boolean hasfc = (dir.getFileClass() != null);
		if(hasfc) dsz += 2;
		
		FileBuffer data = new FileBuffer(dsz, true);
		int dflags = 0x8000;
		if(dir.hasMetadata()) dflags |= 0x2000;
		if(hasfc) dflags |= 0x0040;
		data.addToFile((short)dflags);
		data.addToFile(dir.getGUID());
		data.addToFile(nodename);
		if(metadata != null){
			data.addVariableLengthString(ENCODING, metadata, BinFieldSize.DWORD, 2);
		}
		data.addToFile(children.size());
		if(hasfc) data.addToFile((short)dir.getFileClass().getIntegerValue());
		
		DatNode dirnode = new DatNode();
		dirnode.dat = data;
		dirnode.src = dir;
		
		offmap.put(off, dirnode);
		off += dsz;
		
		for(FileNode child : children){
			if(child instanceof DirectoryNode){
				DatNode cdir = serializeDir((DirectoryNode)child, srcPathMap, offmap, off, refcontainers);
				off += (int)cdir.getTotalSize();
				dirnode.children.add(cdir);
			}
			else{
				child.scratch_field = off;
				DatNode cnode = serializeFileNode(child, srcPathMap, false, refcontainers);
				
				//DatNode cnode = new DatNode();
				offmap.put(off, cnode);
				dirnode.children.add(cnode);
				off += cnode.dat.getFileSize();
			}
		}
		
		return dirnode;
	}
	
	private static List<DatNode> serializeList(Collection<FileNode> nodes, Map<String, Integer> srcPathMap, int off){

		List<DatNode> out = new LinkedList<DatNode>();
		for(FileNode node : nodes){
			
			node.scratch_field = off;
			
			DatNode cnode = null;
			//cnode.src = node;
			
			if(node instanceof DirectoryNode){
				//Just the basics
				//Size estimation & string serialization
				
				//Node name
				FileBuffer nodename = null;
				if(node.getParent() == null){
					//Root has empty name.
					nodename = new FileBuffer(2, true);
					nodename.addToFile((short)0);
				}
				else{
					nodename = new FileBuffer(3 + (node.getFileName().length()<< 1), true);
					nodename.addVariableLengthString(ENCODING, node.getFileName(), BinFieldSize.WORD, 2);
				}
				
				//Metadata
				String metadata = null;
				if(node.hasMetadata()) metadata = serializeMetadata(node);
				DirectoryNode dirnode = (DirectoryNode)node;
				
				int dsz = 18 + (int)nodename.getFileSize() + 4;
				if(metadata != null) dsz += 4 + 2+ metadata.length();
				boolean hasfc = (dirnode.getFileClass() != null);
				if(hasfc) dsz += 2;
				
				FileBuffer data = new FileBuffer(dsz, true);
				int dflags = 0x8000;
				if(node.hasMetadata()) dflags |= 0x2000;
				if(hasfc) dflags |= 0x0040;
				data.addToFile((short)dflags);
				data.addToFile(node.getGUID());
				
				DirectoryNode parent = node.getParent();
				if(parent == null) data.addToFile(-1L);
				else{
					if(parent.getGUID() == -1L) parent.generateGUID();
					data.addToFile(parent.getGUID());
				}
				
				data.addToFile(nodename);
				if(metadata != null){
					data.addVariableLengthString(ENCODING, metadata, BinFieldSize.DWORD, 2);
				}
				if(hasfc) data.addToFile((short)dirnode.getFileClass().getIntegerValue());
				
				cnode = new DatNode();
				cnode.src = node;
				cnode.dat = data;
			}
			else{
				cnode = serializeFileNode(node, srcPathMap, true, true);
			}
			
			out.add(cnode);
			off += cnode.dat.getFileSize();
		}
		
		return out;
	}
	
	private static Collection<LinkNode> getAllLinks(DirectoryNode dir, Collection<LinkNode> list)
	{
		if(list == null) list = new LinkedList<LinkNode>();
		List<FileNode> children = dir.getChildren();
		
		for(FileNode child : children)
		{
			if(child instanceof DirectoryNode)
			{
				getAllLinks((DirectoryNode)child, list);
			}
			else if(child instanceof LinkNode) list.add((LinkNode)child);
		}
		
		return list;
	}
	
	public static boolean saveTree(DirectoryNode root, String outpath) throws IOException
	{
		return saveTree(root, outpath, false);
	}

	public static boolean saveTree(DirectoryNode root, String outpath, boolean scrubPaths) throws IOException
	{
		if(root == null) return false;
		if(outpath == null || outpath.isEmpty()) return false;
		
		//Build source path table...
		Set<String> paths = new TreeSet<String>();
		getSourcePaths(paths, root);
		
		//Estimate size... (since it's UTF8)
		Map<String, Integer> srcPathMap = null;
		int off = 0;
		FileBuffer sptbl_1 = null;
		FileBuffer sptbl_2 = null;
		srcPathMap = new HashMap<String, Integer>();
		int sz1 = 8;
		int sz2 = 0;
		int i = 0;
		for(String p : paths){
			sz1 += 4;
			sz2 += 4 + (p.length() << 1);
			srcPathMap.put(p, i); i++;
		}
		//sz2 is a minimum estimate
		sptbl_1 = new FileBuffer(sz1, true);
		sptbl_2 = new FileBuffer(sz2, true);
		off = sz1;
		sptbl_1.printASCIIToFile(MAGIC_TABLE);
		sptbl_1.addToFile(i);
		i = 0;
		for(String p : paths){
			sptbl_1.addToFile(off);
			if(!scrubPaths) sptbl_2.addVariableLengthString(ENCODING, p, BinFieldSize.WORD, 2);
			else sptbl_2.addVariableLengthString(ENCODING, SCRUBBED_PATH_PLACEHOLDER + i, BinFieldSize.WORD, 2);
			off = sz1 + (int)sptbl_2.getFileSize();
			i++;
		}
		
		//Isolate nodes that are not in tree... (so can list)
		Map<Long, FileNode> treenodes = new TreeMap<Long, FileNode>();
		Map<Long, FileNode> listnodes = new TreeMap<Long, FileNode>();
		Collection<FileNode> tlist = root.getAllDescendants(true);
		for(FileNode n : tlist){
			if(n.getGUID() == -1L) n.generateGUID();
			treenodes.put(n.getGUID(), n);
			
			//Get containers and virtual sources...
			mapRefNodes(listnodes, n);
		}
		if(root.getGUID() == -1L) root.generateGUID();
		treenodes.put(root.getGUID(), root);
		
		//Do list for all nodes not in tree
		long lsize = 0L;
		List<DatNode> extralist = null;
		List<FileNode> lnodes = new LinkedList<FileNode>();
		for(FileNode n : listnodes.values()){
			if(!treenodes.containsKey(n.getGUID())) lnodes.add(n);
		}
		if(!lnodes.isEmpty()){
			extralist = serializeList(lnodes, srcPathMap, 4);
			for(DatNode dn : extralist) lsize += dn.dat.getFileSize();
		}
		
		//Now do tree...
		Map<Integer, DatNode> offmap = new HashMap<Integer, DatNode>();
		DatNode sroot = serializeDir(root, srcPathMap, offmap, 4, true);
	
		//Add link offsets...
		Collection<LinkNode> links = getAllLinks(root, null);
		for(LinkNode link : links){
			DatNode ldat = offmap.get(link.scratch_field);
			ldat.dat.addToFile(link.getLink().scratch_field);
		}

		//Now do header...
		FileBuffer header = new FileBuffer(32, true);
		int hflags = 0x8000;
		long toff = 32 + off;
		if(extralist != null) {
			hflags |= 0x4000;
			toff += lsize + 8;
		}
		header.printASCIIToFile(MAGIC);
		header.addToFile((short)hflags);
		header.addToFile((short)CURRENT_VERSION);
		header.addToFile(32);
		header.addToFile((int)toff);
		if(extralist != null) header.addToFile(32 + off);
		else header.addToFile(-1);
		header.addToFile(0);
		header.addToFile(0);
		header.addToFile(0);
		
		//Now write to disk...
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outpath));
		
		bos.write(header.getBytes());
		bos.write(sptbl_1.getBytes());
		bos.write(sptbl_2.getBytes(0, sptbl_2.getFileSize()));
		if(extralist != null) {
			FileBuffer buff = new FileBuffer(8, true);
			buff.printASCIIToFile(MAGIC_LIST);
			buff.addToFile(extralist.size());
			buff.writeToStream(bos);
			
			for(DatNode dn : extralist) dn.writeToStream(bos);
		}
		
		FileBuffer buff = new FileBuffer(4, true);
		buff.printASCIIToFile(MAGIC_TREE);
		bos.write(buff.getBytes());
		
		sroot.writeToStream(bos);
		
		bos.close();
		
		return true;
	}

	public static boolean saveNodes(Collection<FileNode> nodes, String outpath, boolean scrubPaths) throws IOException{

		//Unique nodes...
		Map<Long, FileNode> nodemap = new TreeMap<Long, FileNode>();
		for(FileNode node : nodes){
			if(node.getGUID() == -1L) node.generateGUID();
			nodemap.put(node.getGUID(), node);
			
			if(node instanceof DirectoryNode){
				DirectoryNode dnode = (DirectoryNode)node;
				Collection<FileNode> dec = dnode.getAllDescendants(true);
				for(FileNode d : dec){
					if(d.getGUID() == -1L) d.generateGUID();
					nodemap.put(d.getGUID(), d);
				}
			}
			
			mapRefNodes(nodemap, node);
		}
		
		//Source path table
		Set<String> paths = new TreeSet<String>();
		for(FileNode node : nodemap.values()){
			paths.add(node.getSourcePath());
			if(node instanceof PatchworkFileNode) paths.addAll(((PatchworkFileNode)node).getAllSourcePaths());
		}
		Map<String, Integer> srcPathMap = null;
		int off = 0;
		FileBuffer sptbl_1 = null;
		FileBuffer sptbl_2 = null;
		
		srcPathMap = new HashMap<String, Integer>();
		int sz1 = 8;
		int sz2 = 0;
		int i = 0;
		for(String p : paths){
			sz1 += 4;
			sz2 += 4 + (p.length() << 1);
			srcPathMap.put(p, i); i++;
		}
		//sz2 is a minimum estimate
		sptbl_1 = new FileBuffer(sz1, true);
		sptbl_2 = new FileBuffer(sz2, true);
		off = sz1;
		sptbl_1.printASCIIToFile(MAGIC_TABLE);
		sptbl_1.addToFile(i);
		i = 0;
		for(String p : paths){
			sptbl_1.addToFile(off);
			if(!scrubPaths) sptbl_2.addVariableLengthString(ENCODING, p, BinFieldSize.WORD, 2);
			else sptbl_2.addVariableLengthString(ENCODING, SCRUBBED_PATH_PLACEHOLDER + i, BinFieldSize.WORD, 2);
			off = sz1 + (int)sptbl_2.getFileSize();
			i++;
		}	
		
		//Now list
		Collection<DatNode> slist = serializeList(nodemap.values(), srcPathMap, 4);
		
		//Add link offsets...
		for(DatNode dn : slist){
			if(dn.src instanceof LinkNode){
				LinkNode ln = (LinkNode)dn.src;
				int loff = ln.getLink().scratch_field;
				dn.dat.addToFile(loff);
			}
		}
		
		//Now do header...
		FileBuffer header = new FileBuffer(32, true);
		int hflags = 0x4000;
		header.printASCIIToFile(MAGIC);
		header.addToFile((short)hflags);
		header.addToFile((short)CURRENT_VERSION);
		header.addToFile(32);
		header.addToFile(-1);
		header.addToFile(32 + off);
		header.addToFile(0);
		header.addToFile(0);
		header.addToFile(0);
		
		//Now write to disk...
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outpath));
		
		bos.write(header.getBytes());
		bos.write(sptbl_1.getBytes());
		bos.write(sptbl_2.getBytes(0, sptbl_2.getFileSize()));
		FileBuffer buff = new FileBuffer(8, true);
		buff.printASCIIToFile(MAGIC_LIST);
		buff.addToFile(slist.size());
		bos.write(buff.getBytes());
		
		for(DatNode dn : slist){
			dn.writeToStream(bos);
		}
		
		bos.close();
		
		return true;
	}
	
}
