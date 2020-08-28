package waffleoRai_Utils;

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

}
