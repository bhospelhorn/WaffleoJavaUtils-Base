package waffleoRai_SoundSynth;

public class IntArrayWindow {

	private int[] window;
	
	private int back;
	private int front;
	private int pos;
	
	private int holding;
	
	private boolean rewound;
	
	public IntArrayWindow(int capacity)
	{
		window = new int[capacity];
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
	
	public boolean put(int i)
	{
		if(isFull()) return false;
		window[front++] = i;
		holding++;
		//front++;
		if(front >= window.length) front = 0;
		//System.err.println("ArrayWindow.put || front = " + front);
		//if(isFull()) System.err.println("Buffer now full!!");
		
		return true;
	}

	public boolean forcePut(int i)
	{
		if(isFull()) pop();
		return put(i);
	}
	
	public int pop()
	{
		if (holding < 1) return -1;
		int i = window[back++];
		holding--;
		//back++;
		if(back >= window.length) back = 0;
		return i;
	}
	
	public boolean canGet()
	{
		if (holding < 1) return false;
		if (atEnd()) return false;
		return true;
	}
	
	public int get()
	{
		if (holding < 1 || atEnd()) return -1;
		int i = window[pos++];
		if(pos >= window.length) pos = 0;
		rewound = false;
		return i;
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
	
	public int getFromFront(int p)
	{
		int n = convertPos(p);
		if(n == -1) return 0;
		return window[n];
	}
	
	public int getFromBack(int p)
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
	
}
