package waffleoRai_Files;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
	
	/*----- Instance Variables -----*/
	
	private String riff_type;
	private MultiValMap<String, RIFFChunk> top_level;
	
	/*----- Init -----*/
	
	protected RIFFReader(){
		top_level = new MultiValMap<String, RIFFChunk>();
	}
	
	/*----- Readers -----*/
	
	private static void readListChunk(BufferReference data, RIFFChunk listChunk, String filepath, boolean cache){
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
				readListChunk(data, child_chunk, filepath, cache);
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
	
 	public static RIFFReader readFile(String path, boolean cache) throws IOException, UnsupportedFileTypeException{
		FileBuffer buffer = FileBuffer.createBuffer(path, false);
		return readFile(buffer.getReferenceAt(0L), cache);
	}
	
	public static RIFFReader readFile(Path path, boolean cache) throws IOException, UnsupportedFileTypeException{
		String spath = path.toAbsolutePath().toString();
		FileBuffer buffer = FileBuffer.createBuffer(spath, false);
		return readFile(buffer.getReferenceAt(0L), cache);
	}
	
	public static RIFFReader readFile(FileBuffer data, boolean cache) throws IOException, UnsupportedFileTypeException{
		if(data == null) return null;
		return readFile(data.getReferenceAt(0L), cache);
	}
	
	public static RIFFReader readFile(BufferReference data, boolean cache) throws IOException, UnsupportedFileTypeException{
		if(data == null) return null;
		RIFFReader rdr = new RIFFReader();
		
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
				readListChunk(data, riffch, filepath, cache);
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
			}
			
			rdr.top_level.addValue(riffch.magic, riffch);
		}
		
		return rdr;
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
	
}
