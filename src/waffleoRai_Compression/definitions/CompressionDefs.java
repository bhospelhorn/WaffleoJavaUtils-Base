package waffleoRai_Compression.definitions;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.FileBufferStreamer;
import waffleoRai_Utils.FileNode;

public class CompressionDefs {
	
	private static String comp_temp_dir;
	private static Map<Integer, AbstractCompDef> def_map;
	
	public static String getCompressionTempDir()
	{
		if(comp_temp_dir == null)
		{
			try{
				String tdir = FileBuffer.getTempDir();
				comp_temp_dir = tdir + File.separator + "waffleoutils";
				if(!FileBuffer.directoryExists(comp_temp_dir)) Files.createDirectory(Paths.get(comp_temp_dir));
			}
			catch(IOException e)
			{
				e.printStackTrace();
				return null;
			}
		}
		return comp_temp_dir;
	}
	
	public static void setCompressionTempDir(String path)
	{
		comp_temp_dir = path;
	}
	
	public static void clearTempDir() throws IOException
	{
		DirectoryStream<Path> dstr = Files.newDirectoryStream(Paths.get(getCompressionTempDir()));
		for(Path p : dstr)
		{
			if(FileBuffer.fileExists(p.toAbsolutePath().toString())) Files.deleteIfExists(p);
		}
		dstr.close();
	}
	
	public static String decompressFileToDiskBuffer(FileNode node, AbstractCompDef compdef) throws IOException
	{
		return decompressFileToDiskBuffer(node.getSourcePath(), node.getOffset(), node.getLength(), compdef);
	}
	
	public static String decompressFileToDiskBuffer(String path, long off, long size, AbstractCompDef compdef) throws IOException
	{
		FileBuffer buffer = FileBuffer.createBuffer(path, off, off+size);
		FileBufferStreamer streamer = new FileBufferStreamer(buffer);
		return compdef.decompressToDiskBuffer(streamer);
	}
	
	private static void buildDefinitionMap()
	{
		def_map = new ConcurrentHashMap<Integer, AbstractCompDef>();
	}
	
	public static void registerDefinition(AbstractCompDef def)
	{
		if(def_map == null) buildDefinitionMap();
		def_map.put(def.getDefinitionID(), def);
	}
	
	public static AbstractCompDef getCompressionDefinition(int comp_def_id)
	{
		if(def_map == null) buildDefinitionMap();
		return def_map.get(comp_def_id);
	}
	
}
