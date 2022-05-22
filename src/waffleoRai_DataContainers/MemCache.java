package waffleoRai_DataContainers;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public abstract class MemCache<K,V> {
	
	public static final int DEFO_MAX_SIZE = 2048;
	
	private boolean threadsafe;
	private int max_items;
	
	private Map<K,V> cache;
	private Deque<K> free_queue;
	
	public MemCache(){this(false, DEFO_MAX_SIZE);}
	public MemCache(boolean concurrent){this(concurrent, DEFO_MAX_SIZE);}
	
	public MemCache(boolean concurrent, int maxSize){
		threadsafe = concurrent;
		max_items = Math.max(maxSize, 16);
		if(threadsafe){
			cache = new ConcurrentHashMap<K,V>();
			free_queue = new ConcurrentLinkedDeque<K>();
		}
		else{
			cache = new HashMap<K,V>();
			free_queue = new LinkedList<K>();
		}
	}
	
	/*--- Internal ---*/
	
	protected abstract V loadFromDisk(K key);
	
	protected void cacheLoadedItem(K key, V val){
		while(cache.size() > max_items){
			//Need to free something.
			if(free_queue.isEmpty()) cache.clear(); //Weird. Should not happen.
			else{
				K freeme = free_queue.pop();
				cache.remove(freeme);
			}
		}
		cache.put(key, val);
		free_queue.add(key);
	}
	
	protected void moveToBackOfFreeQueue(K key){
		free_queue.remove(key);
		free_queue.add(key);
	}
	
	/*--- Getters ---*/
	
	public boolean isConcurrent(){return threadsafe;}
	public int getMaxCachedItemCount(){return max_items;}
	
	public V getItem(K key){
		V item = cache.get(key);
		if(item != null){
			moveToBackOfFreeQueue(key);
			return item;
		}
		item = loadFromDisk(key);
		if(item == null) return null;
		cacheLoadedItem(key, item);
		return item;
	}

	/*--- Setters ---*/
	
	public void clear(){
		cache.clear();
		free_queue.clear();
	}
	
}
