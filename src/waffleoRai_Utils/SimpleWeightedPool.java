package waffleoRai_Utils;

import java.util.Random;

public class SimpleWeightedPool<T>{

	private static class PoolNode<T>{
		public T value;
		public double weight;
		
		public PoolNode<T> next;
	}
	
	private PoolNode<T> head;
	private PoolNode<T> tail;
	private Random rng;
	
	public SimpleWeightedPool() {
		rng = new Random();
	}
	
	public SimpleWeightedPool(long randomSeed) {
		rng = new Random(randomSeed);
	}
	
	public void clearPool() {
		PoolNode<T> node = head;
		PoolNode<T> nn = null;
		while(node != null) {
			nn = node;
			node = nn.next;
			nn.next = null;
		}
		head = null;
		tail = null;
	}
	
	public void addToPool(T element, double weight) {
		PoolNode<T> node = new PoolNode<T>();
		node.value = element;
		node.weight = weight;
		if(tail != null) {
			tail.next = node;
		}
		if(head == null) head = node;
		tail = node;
	}
	
	public T pullElement() {
		double sum = 0.0;
		double rand = rng.nextDouble();
		PoolNode<T> node = head;
		while(node != null) {
			sum += node.weight;
			if(rand < sum) return node.value;
			node = node.next;
		}
		return null;
	}
	
	public Random getRNG() {return rng;}
	
}
