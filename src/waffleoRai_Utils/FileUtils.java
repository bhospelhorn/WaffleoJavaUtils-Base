package waffleoRai_Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.List;

import waffleoRai_Files.NodeMatchCallback;
import waffleoRai_Files.tree.DirectoryNode;
import waffleoRai_Files.tree.FileNode;

public class FileUtils {
	
	public static final String SEP = File.separator;
	
	public static String getUserHomeDir(){
		return System.getProperty("user.home");
	}
	
	public static String getUserAppdataDir(){
		String osname = System.getProperty("os.name");
		osname = osname.toLowerCase();
		String homedir = System.getProperty("user.home");
		if(homedir.endsWith("/") || homedir.endsWith("\\")) {
			homedir = homedir.substring(0, homedir.length()-1);
		}
		
		if(osname.startsWith("win")){
			//Assumed Windows
			if(homedir.endsWith("Documents")){
				//Screw Documents! Remove!
				homedir = homedir.substring(0, homedir.length()-9);
			}
			return homedir + "\\AppData\\Local";
		}
		else if(osname.startsWith("macos") || osname.startsWith("osx")){
			//Assumed Mac
			return homedir + "/Library/Preferences";
		}
		else{
			//Assumed Unix misc.
			return homedir + "/.appdata";
		}
	}
	
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
	
	public static byte[] getMD5Sum(byte[] data){

		try{
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(data);
			byte[] hash = md.digest();
			return hash;
		}
		catch(Exception x){
			x.printStackTrace();
			return null;
		}

	}

	public static boolean moveDirectory(String src, String dest, boolean overwrite) throws IOException{

		if(!FileBuffer.directoryExists(src)) return false;
		
		boolean b = true;
		if(!FileBuffer.directoryExists(dest)) Files.createDirectories(Paths.get(dest));
		DirectoryStream<Path> src_str = Files.newDirectoryStream(Paths.get(src));
		for(Path p : src_str){
			if(Files.isDirectory(p)){
				String myname = p.getFileName().toString();
				b = b && moveDirectory(p.toAbsolutePath().toString(), dest + File.separator + myname, overwrite);
			}
			else{
				//Just move.
				String tpath = p.toAbsolutePath().toString().replace(src, dest);
				if(!overwrite && FileBuffer.fileExists(tpath)){
					Files.delete(Paths.get(tpath));
				}
				else{
					Files.move(p, Paths.get(tpath), StandardCopyOption.REPLACE_EXISTING);	
				}
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
	
	public static int deleteRecursive(String directory_path) throws IOException{
		int dcount = 0;
		if(!FileBuffer.directoryExists(directory_path)) return 0;
		List<String> subdirs = new LinkedList<String>();
		
		DirectoryStream<Path> dstr = Files.newDirectoryStream(Paths.get(directory_path));
		for(Path p : dstr){
			if(Files.isDirectory(p)){
				subdirs.add(p.toAbsolutePath().toString());
			}
			else{
				Files.delete(p);
				dcount++;
			}
		}
		dstr.close();
		
		//Do subdirs
		for(String subdir : subdirs) dcount += deleteRecursive(subdir);
		
		Files.delete(Paths.get(directory_path));
		
		return dcount;
	}
	
	public static String unixRelPath2Local(String workingDir, String path){
		if(path == null) return null;
		
		String[] pathParts = path.split("/");
		int back = 0;
		LinkedList<String> outq = new LinkedList<String>();

		for(int i = 0; i < pathParts.length; i++){
			if(pathParts[i] == null) continue;
			if(pathParts[i].equals(".")) continue;
			else if(pathParts[i].equals("..")){
				if(outq.isEmpty()){back--;}
				else{outq.pollFirst();}
			}
			else{
				outq.addLast(pathParts[i]);
			}
		}
		
		if(workingDir != null){
			//See if working dir is a file...
			int lastslash = workingDir.lastIndexOf(SEP);
			if(lastslash >= 0){
				String filename = workingDir.substring(lastslash + 1);
				if(filename.contains(".")){
					if(FileBuffer.fileExists(filename)){
						workingDir = workingDir.substring(0, lastslash);
					}
				}
			}
			
			String[] dirparts = workingDir.split(SEP);
			int last = dirparts.length - 1 - back;
			for(int i = last; i >= 0; i--){
				outq.push(dirparts[i]);
			}
		}
		
		int sz = 0;
		for(String s : outq) sz += s.length() + 2;
		StringBuilder sb = new StringBuilder(sz);
		boolean first = true;
		for(String s : outq){
			if(!first) sb.append(SEP);
			else first = false;
			sb.append(s);
		}
		
		return sb.toString();
	}
	
	public static String localPath2UnixRel(String workingDir, String path){
		if(path == null) return null;
		if(workingDir == null){
			if(path.contains("\\")){
				path = path.replace('\\', '/');
				path = path.replace(":", "");
				path = "/" + path;
				return path;
			}
			else return path;
		}
		
		String[] wdParts = workingDir.split(SEP);
		String[] trgParts = path.split(SEP);
		int m = -1;
		for(int i = 0; i < wdParts.length; i++){
			if(i >= trgParts.length) break;
			if(!wdParts[i].equals(trgParts[i])){
				break;
			}
			m++;
		}
		
		m++; //First mismatch
		LinkedList<String> list = new LinkedList<String>();
		if(m < wdParts.length){
			//Need some ".." I suppose
			for(int i = m; i < wdParts.length; i++) list.add("..");
		}
		else list.add(".");
		for(int i = m; i < trgParts.length; i++){
			list.add(trgParts[i]);
		}
		
		int alloc = 0;
		for(String s : list) alloc += s.length() + 2;
		StringBuilder sb = new StringBuilder(alloc);
		boolean first = true;
		for(String s : list){
			if(!first) sb.append("/");
			else first = false;
			sb.append(s);
		}
		
		return sb.toString();
	}
	
}
