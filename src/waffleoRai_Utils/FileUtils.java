package waffleoRai_Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;

import waffleoRai_Files.NodeMatchCallback;
import waffleoRai_Files.tree.DirectoryNode;
import waffleoRai_Files.tree.FileNode;

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

	public static FileNode findPartnerNode(FileNode n, String metakey_path, String metakey_id){

		//First checks path meta value
		//If nothing, checks ID meta value
		
		if(n == null) return null;
		if(metakey_path != null){
			String path_val = n.getMetadataValue(metakey_path);
			if(path_val != null){
				//See if node exists at that location
				if(n.getParent() != null){
					FileNode partner = n.getParent().getNodeAt(path_val);	
					if(partner != null) return partner;
				}
			}
		}
		
		//Check ID
		if(metakey_id != null){
			String id_val = n.getMetadataValue(metakey_id);
			if(id_val != null){
				DirectoryNode parent = n.getParent();
				if(parent != null){
					String mpath = n.findNodeThat(new NodeMatchCallback(){

						public boolean meetsCondition(FileNode n) {
							if(n.isDirectory()) return false;
							String v = n.getMetadataValue(metakey_id);
							if(v == null) return false;
							return v.equals(id_val);
						}
						
					});
					if(mpath != null && !mpath.isEmpty()){
						FileNode partner = parent.getNodeAt(mpath);	
						if(partner != null){
							n.setMetadataValue(metakey_path, mpath);
							return partner;
						}
					}
				}
			}
		}
		
		
		return null;
	}
	
	public static String bytes2str(byte[] byte_arr){
		if(byte_arr == null) return "<NULL>";
		
		int chars = byte_arr.length << 1;
		StringBuilder sb = new StringBuilder(chars+2);
		
		for(int i = 0; i < byte_arr.length; i++) sb.append(String.format("%02x", byte_arr[i]));
		
		return sb.toString();
	}
	
	
}
