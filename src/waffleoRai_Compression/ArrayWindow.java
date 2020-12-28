package waffleoRai_Compression;

public class ArrayWindow {
	
	private byte[] window;
	
	private int back;
	private int front;
	private int pos;
	
	private int holding;
	
	private boolean rewound;
	
	public ArrayWindow(int capacity)
	{
		window = new byte[capacity];
		back = 0;
		front = 0;
		pos = 0;
		//full = false;
		holding = 0;
	}
	
	public boolean isEmpty()
	{
		return (holding <= 0);
	}
	
	public boolean isFull()
	{
		return (holding >= window.length);
	}
	
	public boolean atEnd()
	{
		if(pos != front) return false;
		return !rewound;
	}
	
	public boolean put(byte b)
	{
		if(isFull()) return false;
		window[front++] = b;
		holding++;
		//front++;
		if(front >= window.length) front = 0;
		//System.err.println("ArrayWindow.put || front = " + front);
		//if(isFull()) System.err.println("Buffer now full!!");
		
		return true;
	}
	
	public int put(byte[] b){
		int ct = 0;
		for(byte by : b){
			if(put(by)) ct++;
			else return ct;
		}
		return ct;
	}

	public boolean forcePut(byte b)
	{
		if(isFull()) pop();
		return put(b);
	}
	
	public byte pop()
	{
		if (holding < 1) return -1;
		byte b = window[back++];
		holding--;
		//back++;
		if(back >= window.length) back = 0;
		return b;
	}
	
	public boolean canGet()
	{
		if (holding < 1) return false;
		if (atEnd()) return false;
		return true;
	}
	
	public byte get()
	{
		if (holding < 1 || atEnd()) return -1;
		byte b = window[pos++];
		if(pos >= window.length) pos = 0;
		rewound = false;
		return b;
	}
	
	public void rewind()
	{
		pos = back;
		rewound = true;
	}

	private int convertPos(int p)
	{
		if(p < 0) return -1;
		int n = back + p;
		
		while(n >= window.length) n -= window.length;
		if(front > back)
		{
			//Must be between front and back
			if(n >= front) return -1;
			if(n < back) return -1;
		}
		else if (front < back)
		{
			//Must NOT be between front and back
			if (n >= front && n < back) return -1;
		}
		
		return n;
	}
	
	public int getSize()
	{
		return holding;
	}
	
	public byte getFromFront(int p)
	{
		int n = convertPos(p);
		if(n == -1) return 0;
		return window[n];
	}
	
	public byte getFromBack(int p)
	{
		p = holding - p - 1;
		int n = convertPos(p);
		if(n == -1) return 0;
		//return window[n];
		
		try{return window[n];}
		catch(Exception e)
		{
			System.err.println("Round Buffer State--- ");
			System.err.println("Holding: " + holding);
			System.err.println("Front: " + front);
			System.err.println("Back: " + back);
			
			e.printStackTrace();
			System.exit(1);
			return -1;
		}
	}
	
	public int emptySpace(){
		return window.length - holding;
	}

}
