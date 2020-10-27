package waffleoRai_Encryption;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public abstract class FileCryptTable {

	protected Map<Long, FileCryptRecord> records;
	protected ArrayList<ArrayList<byte[]>> keys;
	
	public FileCryptTable(){
		records = new TreeMap<Long, FileCryptRecord>();
		keys = new ArrayList<ArrayList<byte[]>>(4);
		
		for(int i = 0; i < 4; i++) keys.add(new ArrayList<byte[]>());
	}
	
	public int countRecords(){
		return records.size();
	}
	
	public FileCryptRecord getRecord(long file_uid){
		return records.get(file_uid);
	}
	
	public byte[] getKey(int type, int idx){
		if(type < 0 || keys.size() <= type) return null;
		ArrayList<byte[]> typelist = keys.get(type);
		
		if(idx < 0 || idx >= typelist.size()) return null;
		return typelist.get(idx);
	}
	
	public void addRecord(long file_uid, FileCryptRecord record){
		records.put(file_uid, record);
	}
	
	public FileCryptRecord removeRecord(long file_uid){
		return records.remove(file_uid);
	}
	
	public void clearRecords(){
		records.clear();
	}
	
	public int addKey(int type, byte[] key){
		//Returns index within type
		if(type < 0 || keys.size() <= type) return -1;
		
		int idx = getIndexOfKey(type, key);
		if(idx >= 0) return idx;
		
		ArrayList<byte[]> typelist = keys.get(type);
		if(typelist == null) return -1;
		int i = typelist.size();
		typelist.add(key);
		
		return i;
	}
	
	public int getIndexOfKey(int type, byte[] key){
		if(type < 0 || keys.size() <= type) return -1;
		ArrayList<byte[]> typelist = keys.get(type);
		if(typelist == null) return -1;
		int i = 0;
		for(byte[] k : typelist){
			if(Arrays.equals(key, k)) return i;
			i++;
		}
		
		return -1;
	}
	
	public abstract boolean importFromFile(String filepath) throws IOException;
	public abstract boolean exportToFile(String filepath) throws IOException;

	
}
