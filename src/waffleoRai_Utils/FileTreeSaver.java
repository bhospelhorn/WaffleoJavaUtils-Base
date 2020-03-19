package waffleoRai_Utils;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import waffleoRai_Compression.definitions.AbstractCompDef;
import waffleoRai_Compression.definitions.CompDefNode;
import waffleoRai_Compression.definitions.CompressionDefs;
import waffleoRai_Compression.definitions.CompressionInfoNode;
import waffleoRai_Files.EncryptionDefinition;
import waffleoRai_Files.EncryptionDefinitions;
import waffleoRai_Files.FileDefinitions;
import waffleoRai_Files.FileTypeDefNode;
import waffleoRai_Files.FileTypeDefinition;
import waffleoRai_Files.FileTypeNode;
import waffleoRai_Utils.FileBuffer.UnsupportedFileTypeException;

public class FileTreeSaver {
	
	/*Big Endian
	 * 
	 * Magic "tReE" [4]
	 * Version [4]
	 * Offset to source path table [4]
	 * Offset to tree start [4]
	 * 
	 * Source Path Table (So don't have to repeat over and over...)
	 * 	Block Magic "SPBt" [4]
	 * 	# Of paths in table [4]
	 * 		Offset from "SPBt" to string [4 * n]
	 * 
	 * 		Path [VLS 2x2 * n]
	 * 
	 * Tree
	 * 	Block Magic "eert" [4]
	 * 		Nodes, starting with root (followed serially by children)
	 * 
	 * F/D Node
	 * 	Flags [2]
	 * 		15 - Is Directory?
	 * 		14 - Is Link?
	 * 		13 - Has metadata? (V3+)
	 * 		 7 - Source compressed? (If file)
	 * 		 6 - Has type chain (V2+)
	 * 		 5 - Has encryption (V2+)
	 * 	Name [VLS 2x2] 
	 * 	Metadata [VLS 4x2] (V3+) (If applicable)
	 * 
	 * If file...
	 * 	Source Path Index [4]
	 * 	Offset [8]
	 * 	Length [8]
	 *  Encryption Def ID [4] (If applicable) (V2+)
	 *  # Type chain nodes [4] (If applicable) (V2+)
	 *  Type chain [4 * n] (If applicable) (V2+)
	 *  	ID [4] (First bit is always set if compression)
	 * 	/DEP/ Compression Start Offset [8] (If applicable) [V1 only]
	 * 	#Compression Chain Nodes [4] (If applicable) (V2+)
	 * 	Compression Chain [20 * n] (If applicable) (V2+)
	 * 		Definition ID [4]
	 * 		Start offset[8]
	 * 		Length [8]
	 * 
	 * If directory...
	 * 	Number of children [4]
	 * 
	 * If link...
	 * 	(If target is a file, then need file fields.
	 * 		don't need dir fields if dir, though) 
	 * 	Offset of target (relative to "eert") [4]
	 * 
	 * 
	 * Metadata is stored as one giant string formatted as follows:
	 * KEY0=VALUE0;KEY1=VALUE1;KEY2= (etc.)
	 * Keys and values cannot contain the following characters: '=', ';'
	 * There can only be one string value per key
	 */
	
	public static final String MAGIC = "tReE";
	public static final String MAGIC_TABLE = "SPBt";
	public static final String MAGIC_TREE = "eert";
	
	public static final int CURRENT_VERSION = 3;
	
	public static final String ENCODING = "UTF8";
	
	public static final String SCRUBBED_PATH_PLACEHOLDER = "<NA>";
	
	private static int parseDirNode(FileBuffer in, DirectoryNode parent, long stpos, Map<Integer, FileNode> offmap, int version)
	{
		DirectoryNode dir = new DirectoryNode(parent, "");
		offmap.put((int)stpos, dir);
		
		long cpos = stpos;
		int flags = Short.toUnsignedInt(in.shortFromFile(cpos)); cpos+=2;

		//Get name...
		SerializedString ss = in.readVariableLengthString(ENCODING, cpos, BinFieldSize.WORD, 2);
		cpos += ss.getSizeOnDisk();
		dir.setFileName(ss.getString());
		
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
		
		//Get children count...
		int ccount = in.intFromFile(cpos); cpos += 4;
		
		for(int i = 0; i < ccount; i++)
		{
			int flags2 = Short.toUnsignedInt(in.shortFromFile(cpos));
			if((flags2 & 0x4000) != 0) cpos += parseLinkNode(in, dir, cpos, offmap, version);
			else if((flags2 & 0x8000) != 0) cpos += parseDirNode(in, dir, cpos, offmap, version);
			else cpos += parseFileNode(in, dir, cpos, offmap, version);
		}
		
		return (int)(cpos-stpos);
	}
	
	private static int parseFileNode(FileBuffer in, DirectoryNode parent, long stpos, Map<Integer, FileNode> offmap, int version)
	{
		FileNode fn = new FileNode(parent, "");
		offmap.put((int)stpos, fn);
		
		long cpos = stpos;
		int flags = Short.toUnsignedInt(in.shortFromFile(cpos)); cpos+=2;
		
		SerializedString ss = in.readVariableLengthString(ENCODING, cpos, BinFieldSize.WORD, 2);
		cpos += ss.getSizeOnDisk();
		fn.setFileName(ss.getString());
		
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
				fn.setMetadataValue(split[0], split[1]);
			}
		}
		
		fn.scratch_field = in.intFromFile(cpos); cpos+=4;
		fn.setOffset(in.longFromFile(cpos)); cpos += 8;
		fn.setLength(in.longFromFile(cpos)); cpos+=8;
		
		if((flags & 0x20) != 0)
		{
			int defid = in.intFromFile(cpos); cpos += 4;
			EncryptionDefinition def = EncryptionDefinitions.getByID(defid);
			fn.setEncryption(def);
		}
		
		if((flags & 0x40) != 0)
		{
			int tcount = in.intFromFile(cpos); cpos += 4;
			FileTypeNode head = null;
			FileTypeNode node = null;
			for(int i = 0; i < tcount; i++)
			{
				int id = in.intFromFile(cpos); cpos += 4;
				if((id & 0x80000000) != 0)
				{
					//It's a compression.
					AbstractCompDef def = CompressionDefs.getCompressionDefinition(id);
					if(def != null)
					{
						CompDefNode cnode = new CompDefNode(def);
						if(node == null) {head = cnode; node = head;}
						else {node.setChild(cnode); node = cnode;}
					}
				}
				else
				{
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
		
		if((flags & 0x80) != 0)
		{
			//fn.setSourceDataCompressed(in.longFromFile(cpos)); cpos += 8;
			if(version < 2)
			{
				//Just add a null def with offset
				long offset = in.longFromFile(cpos); cpos+=8;
				fn.addCompressionChainNode(null, offset, 0);
			}
			else
			{
				int compcount = in.intFromFile(cpos); cpos+=4;
				for(int i = 0; i < compcount; i++)
				{
					int id = in.intFromFile(cpos); cpos+=4;
					long offset = in.longFromFile(cpos); cpos += 8;
					long len = in.longFromFile(cpos); cpos += 8;
					
					fn.addCompressionChainNode(CompressionDefs.getCompressionDefinition(id), offset, len);
				}
			}
		}
		
		return (int)(cpos - stpos);
	}
	
	private static int parseLinkNode(FileBuffer in, DirectoryNode parent, long stpos, Map<Integer, FileNode> offmap, int version)
	{
		LinkNode ln = new LinkNode(parent, null, "");
		offmap.put((int)stpos, ln);
		
		long cpos = stpos;
		int flags = Short.toUnsignedInt(in.shortFromFile(cpos)); cpos+=2;
		
		SerializedString ss = in.readVariableLengthString(ENCODING, cpos, BinFieldSize.WORD, 2);
		cpos += ss.getSizeOnDisk();
		ln.setFileName(ss.getString());
		
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
				ln.setMetadataValue(split[0], split[1]);
			}
		}
		
		if((flags & 0x8000) == 0)
		{
			//File fields, if applicable
			cpos+=4;
			ln.setOffset(in.longFromFile(cpos)); cpos += 8;
			ln.setLength(in.longFromFile(cpos)); cpos+=8;
			
			if((flags & 0x20) != 0)
			{
				int defid = in.intFromFile(cpos); cpos += 4;
				EncryptionDefinition def = EncryptionDefinitions.getByID(defid);
				ln.setEncryption(def);
			}
			
			if((flags & 0x40) != 0)
			{
				int tcount = in.intFromFile(cpos); cpos += 4;
				FileTypeNode head = null;
				FileTypeNode node = null;
				for(int i = 0; i < tcount; i++)
				{
					int id = in.intFromFile(cpos); cpos += 4;
					if((id & 0x80000000) != 0)
					{
						//It's a compression.
						AbstractCompDef def = CompressionDefs.getCompressionDefinition(id);
						if(def != null)
						{
							CompDefNode cnode = new CompDefNode(def);
							if(node == null) {head = cnode; node = head;}
							else {node.setChild(cnode); node = cnode;}
						}
					}
					else
					{
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
				ln.setTypeChainHead(head);
			}
			
			if((flags & 0x80) != 0)
			{
				//fn.setSourceDataCompressed(in.longFromFile(cpos)); cpos += 8;
				if(version < 2)
				{
					//Just add a null def with offset
					long offset = in.longFromFile(cpos); cpos+=8;
					ln.addCompressionChainNode(null, offset, 0);
				}
				else
				{
					int compcount = in.intFromFile(cpos); cpos+=4;
					for(int i = 0; i < compcount; i++)
					{
						int id = in.intFromFile(cpos); cpos+=4;
						long offset = in.longFromFile(cpos); cpos += 8;
						long len = in.longFromFile(cpos); cpos += 8;
						
						ln.addCompressionChainNode(CompressionDefs.getCompressionDefinition(id), offset, len);
					}
				}
			}
		}
		
		ln.scratch_field = in.intFromFile(cpos); cpos+=4;
		
		return (int)(cpos - stpos);
	}
	
	private static DirectoryNode parseRootDir(FileBuffer in, long stoff, Map<Integer, FileNode> offmap, int version)
	{
		DirectoryNode root = new DirectoryNode(null, "");
		offmap.put((int)stoff, root);
		//Skip flags and name
		long cpos = stoff + 4;
		int ccount = in.intFromFile(cpos); cpos += 4;
		
		for(int i = 0; i < ccount; i++)
		{
			int flags = Short.toUnsignedInt(in.shortFromFile(cpos));
			if((flags & 0x4000) != 0) cpos += parseLinkNode(in, root, cpos, offmap, version);
			else if((flags & 0x8000) != 0) cpos += parseDirNode(in, root, cpos, offmap, version);
			else cpos += parseFileNode(in, root, cpos, offmap, version);
		}
		
		return root;
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
				if(target != null)
				{
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
	
	public static DirectoryNode loadTree(String inpath) throws IOException, UnsupportedFileTypeException
	{
		FileBuffer in = FileBuffer.createBuffer(inpath, true);
		long cpos = in.findString(0, 0x10, MAGIC);
		if(cpos < 0) throw new FileBuffer.UnsupportedFileTypeException("FileTreeSaver.loadTree || Magic number could not be found!");
		
		//For now, just skip most of header...
		cpos += 4;
		int version = in.intFromFile(cpos); cpos += 4;
		cpos += 4;
		long toff = Integer.toUnsignedLong(in.intFromFile(cpos)); cpos += 4;
		//System.err.println("Tree Offset: 0x" + Long.toHexString(toff));
		
		//Read the source path table...
		cpos += 4; //Skip "SPBt"
		int path_count = in.intFromFile(cpos); cpos+=4;
		String[] path_table = new String[path_count];
		//System.err.println("Path count: " + path_count);
		for(int i = 0; i < path_count; i++)
		{
			int off = 16 + in.intFromFile(cpos); cpos += 4;
			long spos = Integer.toUnsignedLong(off);
			path_table[i] = in.readVariableLengthString(ENCODING, spos, BinFieldSize.WORD, 2).getString();
		}
		
		//Read the tree...
		cpos = toff + 4;
		Map<Integer, FileNode> offmap = new HashMap<Integer, FileNode>();
		DirectoryNode root = parseRootDir(in, cpos, offmap, version);
		
		//Resolve links...
		resolveLinks(root, offmap, path_table);
		
		return root;
	}
	
	private static void getSourcePaths(Set<String> set, DirectoryNode dir)
	{
		List<FileNode> children = dir.getChildren();
		for(FileNode child : children)
		{
			if(child.isDirectory())
			{
				if(child instanceof DirectoryNode)
				{
					getSourcePaths(set, (DirectoryNode)child);
				}
			}
			else{
				if(child.getSourcePath() != null) set.add(child.getSourcePath());
			}
		}
	}
	
	private static class DatNode
	{
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
			out.write(dat.getBytes());
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
	
	private static DatNode serializeDir(DirectoryNode dir, Map<String, Integer> srcPathMap, Map<Integer, DatNode> offmap, int off)
	{
		//Note offset
		dir.scratch_field = off;
		
		//Serialize just the name...
		FileBuffer nodename = null;
		if(dir.getParent() == null)
		{
			//Root has empty name.
			nodename = new FileBuffer(2, true);
			nodename.addToFile((short)0);
		}
		else
		{
			nodename = new FileBuffer(3 + (dir.getFileName().length()<< 1), true);
			nodename.addVariableLengthString(ENCODING, dir.getFileName(), BinFieldSize.WORD, 2);
		}
		
		List<FileNode> children = dir.getChildren();
		
		int dsz = 2 + (int)nodename.getFileSize() + 4;
		FileBuffer data = new FileBuffer(dsz, true);
		int dflags = 0x8000;
		if(dir.hasMetadata()) dflags |= 0x2000;
		data.addToFile((short)dflags);
		data.addToFile(nodename);
		if(dir.hasMetadata())
		{
			data.addVariableLengthString(ENCODING, serializeMetadata(dir), BinFieldSize.DWORD, 2);
		}
		data.addToFile(children.size());
		
		DatNode dirnode = new DatNode();
		dirnode.dat = data;
		
		offmap.put(off, dirnode);
		off += dsz;
		
		for(FileNode child : children)
		{
			if(child instanceof DirectoryNode)
			{
				DatNode cdir = serializeDir((DirectoryNode)child, srcPathMap, offmap, off);
				off += (int)cdir.getTotalSize();
				dirnode.children.add(cdir);
			}
			else if(child instanceof LinkNode)
			{
				child.scratch_field = off;
				LinkNode lchild = (LinkNode)child;
				
				FileBuffer cname = new FileBuffer(3 + (lchild.getFileName().length()<< 1), true);
				cname.addVariableLengthString(ENCODING, lchild.getFileName(), BinFieldSize.WORD, 2);
				if(child.hasMetadata()){
					data.addVariableLengthString(ENCODING, serializeMetadata(child), BinFieldSize.DWORD, 2);
				}
				
				List<FileTypeNode> typechain = lchild.getTypeChainAsList();
				List<CompressionInfoNode> compchain = lchild.getCompressionChain();
				int ccsz = compchain.size();
				int flag = 0x4000;
				int csz = 2 + (int)cname.getFileSize() + 4;
				if(lchild.isDirectory()) flag |= 0x8000;
				else
				{
					csz += 20;
					//if(lchild.sourceDataCompressed()){csz += 8; flag |= 0x80;}
					if(lchild.sourceDataCompressed()){csz += (4 + (20 * ccsz)); flag |= 0x80;}
					if(!typechain.isEmpty()){csz += (4 + (typechain.size() << 2)); flag |= 0x40;}
					if(lchild.getEncryption() != null){csz += 4; flag |= 0x20;}
				}
				
				FileBuffer cdat = new FileBuffer(csz, true);
				cdat.addToFile((short)flag);
				cdat.addToFile(cname);
				
				if(!lchild.isDirectory())
				{
					cdat.addToFile(-1); //Same as link target
					cdat.addToFile(lchild.getRelativeOffset());
					cdat.addToFile(lchild.getLength());
					
					if(lchild.getEncryption() != null)
					{
						cdat.addToFile(lchild.getEncryption().getID());
					}
					
					if(!typechain.isEmpty())
					{
						cdat.addToFile(typechain.size());
						for(FileTypeNode node : typechain) cdat.addToFile(node.getTypeID());
					}
					//if(lchild.sourceDataCompressed())cdat.addToFile(lchild.getOffsetOfCompressionStart());
					if(lchild.sourceDataCompressed())
					{
						cdat.addToFile(ccsz);
						for(CompressionInfoNode comp : compchain)
						{
							AbstractCompDef def = comp.getDefinition();
							if(def != null) cdat.addToFile(def.getDefinitionID());
							else cdat.addToFile(-1);
							cdat.addToFile(comp.getStartOffset());
							cdat.addToFile(comp.getLength());
						}
					}
				}
				
				//Offset to link will be added LATER
				
				DatNode cnode = new DatNode();
				offmap.put(off, cnode);
				cnode.dat = cdat;
				dirnode.children.add(cnode);
				off += cdat.getFileSize();
			}
			else
			{
				child.scratch_field = off;
				FileBuffer cname = new FileBuffer(3 + (child.getFileName().length()<< 1), true);
				cname.addVariableLengthString(ENCODING, child.getFileName(), BinFieldSize.WORD, 2);
				if(child.hasMetadata()){
					data.addVariableLengthString(ENCODING, serializeMetadata(child), BinFieldSize.DWORD, 2);
				}
				
				List<FileTypeNode> typechain = child.getTypeChainAsList();
				List<CompressionInfoNode> compchain = child.getCompressionChain();
				int ccsz = compchain.size();
				int flag = 0;
				int csz = 2 + (int)cname.getFileSize() + 20;
				if(child.sourceDataCompressed()){csz += (4 + (20 * ccsz)); flag |= 0x80;}
				if(!typechain.isEmpty()){csz += (4 + (typechain.size() << 2)); flag |= 0x40;}
				if(child.getEncryption() != null){csz += 4; flag |= 0x20;}
				//if(child.sourceDataCompressed()){csz += 8; flag |= 0x80;}
				
				FileBuffer cdat = new FileBuffer(csz, true);
				cdat.addToFile((short)flag);
				cdat.addToFile(cname);
				
				Integer srcidx = 0;
				if(srcPathMap != null)
				{
					srcidx = srcPathMap.get(child.getSourcePath());
					if(srcidx == null) srcidx = -1;
				}
				cdat.addToFile(srcidx);
				cdat.addToFile(child.getOffset());
				cdat.addToFile(child.getLength());
				if(child.getEncryption() != null)
				{
					cdat.addToFile(child.getEncryption().getID());
				}
				if(!typechain.isEmpty())
				{
					cdat.addToFile(typechain.size());
					for(FileTypeNode node : typechain) cdat.addToFile(node.getTypeID());
				}
				//if(child.sourceDataCompressed())cdat.addToFile(child.getOffsetOfCompressionStart());
				if(child.sourceDataCompressed())
				{
					cdat.addToFile(ccsz);
					for(CompressionInfoNode comp : compchain)
					{
						AbstractCompDef def = comp.getDefinition();
						if(def != null) cdat.addToFile(def.getDefinitionID());
						else cdat.addToFile(-1);
						cdat.addToFile(comp.getStartOffset());
						cdat.addToFile(comp.getLength());
					}
				}
				
				DatNode cnode = new DatNode();
				offmap.put(off, cnode);
				cnode.dat = cdat;
				dirnode.children.add(cnode);
				off += cdat.getFileSize();
			}
		}
		
		return dirnode;
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
		if(!scrubPaths)
		{
			srcPathMap = new HashMap<String, Integer>();
			int sz1 = 8;
			int sz2 = 0;
			int i = 0;
			for(String p : paths)
			{
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
			for(String p : paths)
			{
				sptbl_1.addToFile(off);
				sptbl_2.addVariableLengthString(ENCODING, p, BinFieldSize.WORD, 2);
				off = sz1 + (int)sptbl_2.getFileSize();
			}	
		}
		else
		{
			int sz1 = 8 + 4;
			int sz2 = 2 + SCRUBBED_PATH_PLACEHOLDER.length();
			if(sz2 % 2 != 0) sz2++;
			sptbl_1 = new FileBuffer(sz1, true);
			sptbl_2 = new FileBuffer(sz2, true);
			
			sptbl_1.printASCIIToFile(MAGIC_TABLE);
			sptbl_1.addToFile(1);
			sptbl_1.addToFile(12);
			sptbl_2.addVariableLengthString(ENCODING, SCRUBBED_PATH_PLACEHOLDER, BinFieldSize.WORD, 2);
		}
		
		//Now do tree...
		Map<Integer, DatNode> offmap = new HashMap<Integer, DatNode>();
		DatNode sroot = serializeDir(root, srcPathMap, offmap, 4);
	
		//Add link offsets...
		Collection<LinkNode> links = getAllLinks(root, null);
		for(LinkNode link : links)
		{
			DatNode ldat = offmap.get(link.scratch_field);
			ldat.dat.addToFile(link.getLink().scratch_field);
		}

		//Now do header...
		FileBuffer header = new FileBuffer(16, true);
		header.printASCIIToFile(MAGIC);
		header.addToFile(CURRENT_VERSION);
		header.addToFile(16);
		header.addToFile(16 + off);
		
		//Now write to disk...
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outpath));
		
		bos.write(header.getBytes());
		bos.write(sptbl_1.getBytes());
		bos.write(sptbl_2.getBytes(0, sptbl_2.getFileSize()));
		FileBuffer buff = new FileBuffer(4, true);
		buff.printASCIIToFile(MAGIC_TREE);
		bos.write(buff.getBytes());
		
		sroot.writeToStream(bos);
		
		bos.close();
		
		return true;
	}

	
}
