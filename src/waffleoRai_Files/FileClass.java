package waffleoRai_Files;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum FileClass {
	
	COMPRESSED(1),
	
	ARCHIVE(100),
	
	SYSTEM(200),
	
	EXECUTABLE(300),
	CODELIB(301),
	
	SOUND_ARC(400),
	SOUND_STREAM(401),
	SOUND_WAVE(402),
	SOUND_WAVEARC(403),
	SOUND_WAVECOL(404), //such as brwsd
	SOUND_SEQ(405),
	SOUNDBANK(406),
	SOUND_MISC(407);
	
	private int val;
	
	private FileClass(int i){
		val = i;
	}
	
	public int getIntegerValue(){return val;}

	//-----------------
	
	private static Map<Integer, FileClass> imap;
	
	public static FileClass getFromInteger(int val){

		if(imap == null){
			imap = new ConcurrentHashMap<Integer, FileClass>();
			for(FileClass v : FileClass.values()) imap.put(v.getIntegerValue(), v);
		}
		
		return imap.get(val);
	}
	
}
