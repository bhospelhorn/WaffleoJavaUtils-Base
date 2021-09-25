package waffleoRai_Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MultiValMap<K extends Comparable<K>,V> {
	
private Map<K, List<V>> map;
	
	public MultiValMap(){
		map = new HashMap<K, List<V>>();
	}
	
	public boolean addValue(K key, V value){
		if(value == null || key == null) return false;
		List<V> list = map.get(key);
		if(list == null){
			list = new LinkedList<V>();
			map.put(key, list);
		}
		list.add(value);
		return true;
	}
	
	public void clearValues(){map.clear();}

	public List<K> getOrderedKeys(){
		List<K> list = new ArrayList<K>(map.size()+1);
		list.addAll(map.keySet());
		Collections.sort(list);
		return list;
	}
	
	public List<V> getValues(K key){
		List<V> copy = new LinkedList<V>();
		List<V> list = map.get(key);
		if(list != null) copy.addAll(list);
		return copy;
	}

}
