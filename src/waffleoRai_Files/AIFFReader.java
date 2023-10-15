package waffleoRai_Files;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import waffleoRai_DataContainers.MultiValMap;
import waffleoRai_Files.tree.FileNode;
import waffleoRai_Utils.BufferReference;
import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.FileBuffer.UnsupportedFileTypeException;

public class AIFFReader {
	
	/*----- Constants -----*/
	
	public static final String MAGIC_STR = "FORM";
	public static final String MAGIC_AIFF_STR = "AIFF";
	public static final String MAGIC_LIST_STR = "LIST";
	
	public static final int MAGIC = 0x464f524d;
	public static final int MAGIC_AIFF = 0x41494646;
	public static final int MAGIC_LIST = 0x4c495354;
	
	/*----- Inner Classes -----*/
	
	public static class AIFFChunk{
		private String magic;
		private FileNode chunk;
		
		private FileBuffer cachedChunk;
		
		private AIFFChunk(){}
		
		public String getMagicNumber(){return magic;}
		public FileNode getChunkReference(){return chunk;}
		
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
		}
		
		public int getDataSize(){
			if(chunk == null) return 0;
			return (int)chunk.getLength();
		}
		
		public int getFullChunkSize(){
			//Includes header
			if(chunk == null) return 0;
			int datsize = (int)chunk.getLength();
			return datsize + 8;
		}
		
	}

	/*----- Instance Variables -----*/
	
	private String aiff_type;
	private MultiValMap<String, AIFFChunk> top_level;
	
	/*----- Init -----*/
	
	protected AIFFReader(){
		top_level = new MultiValMap<String, AIFFChunk>();
	}

	/*----- Readers -----*/
	
	public static AIFFReader readFile(String path, boolean cache) throws IOException, UnsupportedFileTypeException{
		FileBuffer buffer = FileBuffer.createBuffer(path, true);
		return readFile(buffer.getReferenceAt(0L), cache);
	}
	
	public static AIFFReader readFile(Path path, boolean cache) throws IOException, UnsupportedFileTypeException{
		String spath = path.toAbsolutePath().toString();
		FileBuffer buffer = FileBuffer.createBuffer(spath, true);
		return readFile(buffer.getReferenceAt(0L), cache);
	}
	
	public static AIFFReader readFile(FileBuffer data, boolean cache) throws IOException, UnsupportedFileTypeException{
		if(data == null) return null;
		return readFile(data.getReferenceAt(0L), cache);
	}
	
	public static AIFFReader readFile(BufferReference data, boolean cache) throws IOException, UnsupportedFileTypeException{
		if(data == null) return null;
		AIFFReader rdr = new AIFFReader();
		
		long startPos = data.getBufferPosition();
		data.setByteOrder(true);
		int vali = data.nextInt();
		if(vali != MAGIC){
			throw new UnsupportedFileTypeException("AIFFReader.readFile || \"FORM\" magic number not found!");
		}
		
		long aiffEnd = startPos + 8 + data.nextInt();
		rdr.aiff_type = data.nextASCIIString(4);
		
		String filepath = data.getBuffer().getPath();
		while(data.getBufferPosition() < aiffEnd){
			String cmagic = data.nextASCIIString(4);
			if(cmagic == null || cmagic.isEmpty()) break;
			int csize = data.nextInt();
			
			AIFFChunk aiffch = new AIFFChunk();

			aiffch.magic = cmagic;
			FileNode chunk = new FileNode(null, cmagic);
			chunk.setSourcePath(filepath);
			chunk.setOffset(data.getBufferPosition());
			chunk.setLength(Integer.toUnsignedLong(csize));
			aiffch.chunk = chunk;
			
			if(cache){
				FileBuffer backbuff = data.getBuffer();
				aiffch.cachedChunk = backbuff.createReadOnlyCopy(chunk.getOffset(), 
						chunk.getOffset() + chunk.getLength());
			}
			
			rdr.top_level.addValue(aiffch.magic, aiffch);
		}
		
		return rdr;
	}
	
	/*----- Getters -----*/
	
	public String getAIFFFileType(){return aiff_type;}
	public boolean hasTopLevelChunk(String key){return top_level.containsKey(key);}
	
	public AIFFChunk getFirstTopLevelChunk(String key){
		return top_level.getFirstValueWithKey(key);
	}
	
	public List<AIFFChunk> getTopLevelChunks(String key){
		List<AIFFChunk> vals = top_level.getValues(key);
		if(vals == null || vals.isEmpty()) return new LinkedList<AIFFChunk>();
		//This is already a copy, so we can return as is.
		return vals;
	}
	
	/*----- Setters -----*/
	
	private static void setSourcePathForChunk(AIFFChunk chunk, String path){
		if(chunk == null) return;
		if(chunk.chunk != null) chunk.chunk.setSourcePath(path);
	}
	
	public void setSourcePath(String path){
		//Updates all file nodes to reference this file
		List<String> keyset = top_level.getOrderedKeys();
		for(String key : keyset){
			List<AIFFChunk> values = top_level.getValues(key);
			for(AIFFChunk c : values){
				setSourcePathForChunk(c, path);
			}
		}
	}
	
	public void clearDataCache(){
		List<String> keys = top_level.getOrderedKeys();
		for(String key : keys){
			List<AIFFChunk> values = top_level.getValues(key);
			for(AIFFChunk c : values){
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
