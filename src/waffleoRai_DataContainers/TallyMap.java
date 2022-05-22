package waffleoRai_DataContainers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TallyMap {

	private Map<Integer, Integer> coremap;
	
	public TallyMap(){
		this(false);
	}
	
	public TallyMap(boolean threadsafe){
		if(threadsafe){
			coremap = new ConcurrentHashMap<Integer, Integer>();
		}
		else{
			coremap = new HashMap<Integer, Integer>();	
		}
	}
	
	public void increment(int value){
		Integer t = coremap.get(value);
		if (t == null) coremap.put(value, 1);
		else coremap.put(value, t + 1);
	}
	
	public void decrement(int value){
		Integer t = coremap.get(value);
		if (t == null) coremap.put(value, -1);
		else coremap.put(value, t - 1);
	}
	
	public void increment(int value, int amount){
		Integer t = coremap.get(value);
		if (t == null) coremap.put(value, amount);
		else coremap.put(value, t + amount);
	}
	
	public void decrement(int value, int amount){
		Integer t = coremap.get(value);
		if (t == null) coremap.put(value, amount * -1);
		else coremap.put(value, t - amount);
	}
	
	public void setZero(int value){
		Integer t = coremap.get(value);
		if (t == null) coremap.put(value, 0);
		else coremap.put(value, 0);
	}
	
	public void clear(){
		coremap.clear();
	}
	
	public boolean clearEntry(int value){
		if(coremap.remove(value) != null) return true;
		return false;
	}
	
	public boolean hasEntry(int value){
		return coremap.containsKey(value);
	}
	
	public List<Integer> getAllValues(){
		List<Integer> list = new ArrayList<Integer>(coremap.size());
		list.addAll(coremap.keySet());
		Collections.sort(list);
		
		return list;
	}
	
	public int getCount(int value){
		Integer t = coremap.get(value);
		if (t == null) return 0;
		else return t;
	}
	
}
