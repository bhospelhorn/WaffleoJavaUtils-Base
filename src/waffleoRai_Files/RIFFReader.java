package waffleoRai_Files;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import waffleoRai_DataContainers.MultiValMap;
import waffleoRai_Files.tree.FileNode;
import waffleoRai_Utils.BufferReference;
import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.FileBuffer.UnsupportedFileTypeException;;

public class RIFFReader {
	
	/*----- Constants -----*/
	
	public static final String MAGIC_STR = "RIFF";
	public static final String MAGIC_LIST_STR = "LIST";
	
	public static final int MAGIC = 0x52494646;
	public static final int MAGIC_LIST = 0x4c495354;
	
	/*----- Inner Classes -----*/
	
	public static class RIFFChunk{
		private String magic;
		private FileNode chunk;
		private RIFFList list;
		
		private FileBuffer cachedChunk;
		
		private RIFFChunk(){}
		
		public String getMagicNumber(){return magic;}
		public FileNode getChunkReference(){return chunk;}
		public boolean isList(){return list != null;}
		public RIFFList getListContents(){return list;}
		
		public FileBuffer openBuffer() throws IOException{
			if(chunk == null) return null;
			if(cachedChunk == null){
				cachedChunk = chunk.loadData();
			}
			return cachedChunk;
		}
		
		public BufferReference open() throws IOException{
			if(chunk == null) return null;
			if(cachedChunk == null){
				cachedChunk = chunk.loadData();
			}
			return cachedChunk.getReferenceAt(0L);
		}
		
		public void clearCache() throws IOException{
			if(cachedChunk != null){
				cachedChunk.dispose(); 
				cachedChunk = null;
			}
			if(list != null) list.clearCache();
		}
		
		public int getDataSize(){
			if(chunk == null) return 0;
			return (int)chunk.getLength();
		}
		
		public int getFullChunkSize(){
			//Includes header
			if(chunk == null) return 0;
			int datsize = (int)chunk.getLength();
			if(list != null) return datsize + 12;
			return datsize + 8;
		}
		
	}
	
	public static class RIFFList{
		private RIFFChunk[] contents;
		
		private RIFFList(){}
		
		private RIFFList(int alloc){
			if(alloc > 0){
				contents = new RIFFChunk[alloc];
			}
		}
		
		public int getLength(){
			if(contents == null) return 0;
			return contents.length;
		}
		
		public RIFFChunk getItem(int index){
			if(contents == null) return null;
			if(index < 0 || index >= contents.length) return null;
			return contents[index];
		}
		
		public List<RIFFChunk> getAllItems(){
			if(contents == null) return new LinkedList<RIFFChunk>();
			ArrayList<RIFFChunk> list = new ArrayList<RIFFChunk>(contents.length);
			for(int i = 0; i < contents.length; i++){
				if(contents[i] != null) list.add(contents[i]);
			}
			return list;
		}
		
		public MultiValMap<String, RIFFChunk> mapContentsById(){
			MultiValMap<String, RIFFChunk> map = new MultiValMap<String, RIFFChunk>();
			if(contents != null){
				for(int i = 0; i < contents.length; i++){
					if(contents[i] == null) continue;
					map.addValue(contents[i].magic, contents[i]);
				}
			}
			return map;
		}
		
		public void clearCache() throws IOException{
			if(contents == null) return;
			for(int i = 0; i < contents.length; i++){
				contents[i].clearCache();
			}
		}
		
	}
	
	public static class RIFFINFO{
		private String key;
		private String value;
		
		public void readFromChunk(RIFFChunk chunk) throws IOException{
			if(chunk == null) return;
			key = chunk.getMagicNumber();
			FileBuffer buff = chunk.openBuffer();
			value = buff.getASCII_string(0L, '\0');
		}
		
		public void readFromChunk(RIFFChunk chunk, String encoding) throws IOException{
			if(chunk == null) return;
			key = chunk.getMagicNumber();
			FileBuffer buff = chunk.openBuffer();
			value = buff.readEncoded_string(encoding, 0L, '\0');
		}
		
		public String getKey(){return key;}
		public String getValue(){return value;}

	}
	
	/*----- Instance Variables -----*/
	
	private String riff_type;
	private int align = 1;
	private MultiValMap<String, RIFFChunk> top_level;
	
	/*----- Init -----*/
	
	protected RIFFReader(){
		top_level = new MultiValMap<String, RIFFChunk>();
	}
	
	/*----- Readers -----*/
	
	private static void readListChunk(BufferReference data, RIFFChunk listChunk, String filepath, int align, boolean cache){
		int listsz = data.nextInt();
		listChunk.magic = data.nextASCIIString(4);
		
		FileNode lchunk = new FileNode(null, listChunk.magic);
		lchunk.setSourcePath(filepath);
		lchunk.setOffset(data.getBufferPosition());
		lchunk.setLength(Integer.toUnsignedLong(listsz - 4));
		listChunk.chunk = lchunk;
		
		LinkedList<RIFFChunk> children = new LinkedList<RIFFChunk>();
		long listend = data.getBufferPosition() + listsz - 4;
		while(data.getBufferPosition() < listend){
			String cmagic = data.nextASCIIString(4);
			int csize = data.nextInt();
			
			RIFFChunk child_chunk = new RIFFChunk();
			
			if(cmagic.equals(MAGIC_LIST_STR)){
				data.subtract(4L);
				readListChunk(data, child_chunk, filepath, align, cache);
			}
			else{
				child_chunk.magic = cmagic;
				FileNode chunk = new FileNode(null, cmagic);
				chunk.setSourcePath(filepath);
				chunk.setOffset(data.getBufferPosition());
				chunk.setLength(Integer.toUnsignedLong(csize));
				child_chunk.chunk = chunk;
				
				if(cache){
					FileBuffer backbuff = data.getBuffer();
					child_chunk.cachedChunk = backbuff.createReadOnlyCopy(chunk.getOffset(), chunk.getOffset() + chunk.getLength());
				}
				
				data.add(csize);
				if(align > 1){
					while((data.getBufferPosition() % align) != 0){
						data.add(1L);
					}
				}
			}
			
			children.add(child_chunk);
		}
		
		//Add children to list chunk.
		int ccount = children.size();
		listChunk.list = new RIFFList(ccount);
		int i = 0;
		for(RIFFChunk child : children){
			listChunk.list.contents[i++] = child;
		}
	}
	
 	public static RIFFReader readFile(String path, int align,  boolean cache) throws IOException, UnsupportedFileTypeException{
		FileBuffer buffer = FileBuffer.createBuffer(path, false);
		return readFile(buffer.getReferenceAt(0L), align, cache);
	}
	
	public static RIFFReader readFile(Path path, int align,  boolean cache) throws IOException, UnsupportedFileTypeException{
		String spath = path.toAbsolutePath().toString();
		FileBuffer buffer = FileBuffer.createBuffer(spath, false);
		return readFile(buffer.getReferenceAt(0L), align, cache);
	}
	
	public static RIFFReader readFile(FileBuffer data, int align,  boolean cache) throws IOException, UnsupportedFileTypeException{
		if(data == null) return null;
		return readFile(data.getReferenceAt(0L), align, cache);
	}
	
	public static RIFFReader readFile(BufferReference data, int align, boolean cache) throws IOException, UnsupportedFileTypeException{
		if(data == null) return null;
		RIFFReader rdr = new RIFFReader();
		rdr.align = align;
		
		//Check for RIFF magic
		long startPos = data.getBufferPosition();
		data.setByteOrder(true);
		int vali = data.nextInt();
		if(vali != MAGIC){
			throw new UnsupportedFileTypeException("RIFFReader.readFile || \"RIFF\" magic number not found!");
		}
		
		data.setByteOrder(false);
		
		long riffEnd = startPos + 8 + data.nextInt();
		rdr.riff_type = data.nextASCIIString(4);
		
		String filepath = data.getBuffer().getPath();
		while(data.getBufferPosition() < riffEnd){
			String cmagic = data.nextASCIIString(4);
			if(cmagic == null || cmagic.isEmpty()) break;
			int csize = data.nextInt();
			
			RIFFChunk riffch = new RIFFChunk();

			if(cmagic.equals(MAGIC_LIST_STR)){
				data.subtract(4L);
				readListChunk(data, riffch, filepath, rdr.align, cache);
			}
			else{
				riffch.magic = cmagic;
				FileNode chunk = new FileNode(null, cmagic);
				chunk.setSourcePath(filepath);
				chunk.setOffset(data.getBufferPosition());
				chunk.setLength(Integer.toUnsignedLong(csize));
				riffch.chunk = chunk;
				
				if(cache){
					FileBuffer backbuff = data.getBuffer();
					riffch.cachedChunk = backbuff.createReadOnlyCopy(chunk.getOffset(), 
							chunk.getOffset() + chunk.getLength());
				}
				
				data.add(csize);
				if(rdr.align > 1){
					while((data.getBufferPosition() % rdr.align) != 0){
						data.add(1L);
					}
				}
			}
			
			rdr.top_level.addValue(riffch.magic, riffch);
		}
		
		return rdr;
	}
	
	public static Map<String, String> readINFOList(RIFFChunk info) throws IOException{

		if(info == null || !info.isList()) return null;
		if(!info.magic.equals("INFO")) return null;
		
		Map<String, String> map = new HashMap<String, String>();
		RIFFList ilist = info.getListContents();
		if(ilist.contents == null) return map;
		
		for(int i = 0; i < ilist.contents.length; i++){
			if(ilist.contents[i] == null) continue;
			RIFFINFO inode = new RIFFINFO();
			inode.readFromChunk(ilist.contents[i]);
			map.put(inode.key, inode.value);
		}
		
		return map;
	}
	
	/*----- Getters -----*/
	
	public String getRIFFFileType(){return riff_type;}
	public boolean hasTopLevelChunk(String key){return top_level.containsKey(key);}
	
	public RIFFChunk getFirstTopLevelChunk(String key){
		return top_level.getFirstValueWithKey(key);
	}
	
	public List<RIFFChunk> getTopLevelChunks(String key){
		List<RIFFChunk> vals = top_level.getValues(key);
		if(vals == null || vals.isEmpty()) return new LinkedList<RIFFChunk>();
		//This is already a copy, so we can return as is.
		return vals;
	}
	
	/*----- Setters -----*/
	
	private static void setSourcePathForChunk(RIFFChunk chunk, String path){
		if(chunk == null) return;
		if(chunk.chunk != null) chunk.chunk.setSourcePath(path);
		if(chunk.list != null){
			if(chunk.list.contents != null){
				for(int i = 0; i < chunk.list.contents.length; i++){
					setSourcePathForChunk(chunk.list.contents[i], path);
				}
			}
		}
	}
	
	public void setSourcePath(String path){
		//Updates all file nodes to reference this file
		List<String> keyset = top_level.getOrderedKeys();
		for(String key : keyset){
			List<RIFFChunk> values = top_level.getValues(key);
			for(RIFFChunk c : values){
				setSourcePathForChunk(c, path);
			}
		}
	}
	
	public void clearDataCache(){
		List<String> keys = top_level.getOrderedKeys();
		for(String key : keys){
			List<RIFFChunk> values = top_level.getValues(key);
			for(RIFFChunk c : values){
				try {c.clearCache();} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void close(){
		clearDataCache();
		top_level.clearValues();
	}
	
}
