package waffleoRai_Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;

public class FileUtils {
	
	public static byte[] getSHA1Hash(byte[] data){

		try{
			MessageDigest sha = MessageDigest.getInstance("SHA-1");
			sha.update(data);
			byte[] hash = sha.digest();
			return hash;
		}
		catch(Exception x){
			x.printStackTrace();
			return null;
		}

	}
	
	public static byte[] getSHA256Hash(byte[] data){

		try{
			MessageDigest sha = MessageDigest.getInstance("SHA-256");
			sha.update(data);
			byte[] hash = sha.digest();
			return hash;
		}
		catch(Exception x){
			x.printStackTrace();
			return null;
		}

	}

	public static boolean moveDirectory(String src, String dest) throws IOException{

		if(!FileBuffer.directoryExists(src)) return false;
		
		boolean b = true;
		if(!FileBuffer.directoryExists(dest)) Files.createDirectories(Paths.get(dest));
		DirectoryStream<Path> src_str = Files.newDirectoryStream(Paths.get(src));
		for(Path p : src_str){
			if(Files.isDirectory(p)){
				String myname = p.getFileName().toString();
				b = b && moveDirectory(p.toAbsolutePath().toString(), dest + File.separator + myname);
			}
			else{
				//Just move.
				String tpath = p.toAbsolutePath().toString().replace(src, dest);
				Files.move(p, Paths.get(tpath));
			}
		}
		src_str.close();
		
		//Delete original directory now that it should be empty
		Files.delete(Paths.get(src));
		
		return b;
	}
	
}
