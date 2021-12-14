package waffleoRai_Utils;

import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

public class StackStack<T> {

	private LinkedList<T> current;
	private Map<Integer, LinkedList<T>> states;
	
	public StackStack(){
		current = new LinkedList<T>();
		states = new TreeMap<Integer, LinkedList<T>>();
	}
	
	public T pop(){return current.pop();}
	public T peek(){return current.peek();}
	public T peekBottom(){return current.peekLast();}
	public void push(T item){current.push(item);}
	public void add(T item){current.add(item);}
	
	public boolean loadState(int state_id){
		LinkedList<T> state = states.remove(state_id);
		if(state == null) return false;
		current.clear();
		current = state;
		return true;
	}
	
	public boolean saveState(int state_id){
		if(states.containsKey(state_id)) return false;
		LinkedList<T> state = current;
		states.put(state_id, state);
		current = new LinkedList<T>();
		current.addAll(state);
		return true;
	}
	
	public void clear(){current.clear();}
	public void clearStates(){states.clear();}
	
}
