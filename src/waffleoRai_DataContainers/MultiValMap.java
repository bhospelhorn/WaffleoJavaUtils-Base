package waffleoRai_DataContainers;

import java.util.ArrayList;
import java.util.Collection;
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
	
	public V getFirstValueWithKey(K key){
		List<V> list = map.get(key);
		if(list == null) return null;
		if(list.isEmpty()) return null;
		return list.get(0);
	}

	public boolean hasValueAt(K key, V value){
		List<V> list = map.get(key);
		if(list == null || list.isEmpty()) return false;
		return list.contains(value);
	}
	
	public Map<K, List<V>> getBackingMap(){return map;}
	
	public boolean containsKey(K key){return map.containsKey(key);}
	
	public boolean isEmpty(){return map.isEmpty();}
	
	public List<V> remove(K key){
		return map.remove(key);
	}
	
	public int keyCount() {
		return map.size();
	}
	
	public Collection<V> allValues(){
		if(map.isEmpty()) return new LinkedList<V>();
		List<V> list = new ArrayList<V>(valueCount());
		
		for(List<V> val : map.values()) {
			list.addAll(val);
		}
		
		return list;
	}
	
	public int valueCount() {
		//ALL values
		int count = 0;
		for(List<V> val : map.values()) {
			count += val.size();
		}
		return count;
	}
	
}
