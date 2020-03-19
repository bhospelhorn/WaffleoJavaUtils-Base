package waffleoRai_SoundSynth;

import java.time.OffsetDateTime;
import java.util.concurrent.ConcurrentLinkedQueue;

import waffleoRai_Utils.Arunnable;

//Worker thread wakes up either every second, or whenever buffer drops below 10%
//Switch to blocking queue if this seems to be a problem

public abstract class BufferedFilter implements Filter{
	
	/*----- Constants -----*/
	
	public static final double BUFFER_REFILL_THRESH = 0.10;
	
	/*----- InstanceVariables -----*/
	
	private FilterBuffer[] buffer;
	private BufferedFilterWorker worker;
	
	private int channel_capacity;
	private volatile boolean closed;
	
	private Boolean blocker;
	private volatile boolean hold;
	private volatile boolean pending;
	
	/*----- Buffering -----*/
	
	public class FilterBuffer
	{
		private int capacity;
		private ConcurrentLinkedQueue<Integer> samples;
		
		public FilterBuffer(int maxSize)
		{
			capacity = maxSize;
			samples = new ConcurrentLinkedQueue<Integer>();
		}
		
		public boolean putSamples(int s)
		{
			if(isFull()) return false;
			return samples.add(s);
		}
		
		public int popSample()
		{
			Integer i = samples.poll();
			if(i == null) return 0;
			return i;
		}
		
		public boolean isFull()
		{
			return samples.size() >= capacity;
		}
		
		public boolean isEmpty()
		{
			return samples.isEmpty();
		}
		
		public double getFillLevel()
		{
			return (double)samples.size() / (double)capacity;
		}
		
		public void clear()
		{
			samples.clear();
		}
		
	}
	
	public class BufferedFilterWorker extends Arunnable
	{
		
		public BufferedFilterWorker()
		{
			super.setName("bufferedfilterworker_" + Long.toHexString(OffsetDateTime.now().toEpochSecond()));
			super.sleeps = true;
			super.sleeptime = 1000;
		}
		
		public void doSomething() 
		{
			//Eat interrupts while filling buffer
			if(hold) return;
			while(!bufferFull())
			{
				int[] next = null;
				try{next = generateNextSamples();}
				catch(InterruptedException e){return;}
				if(next == null) break;
				for(int i = 0; i < buffer.length; i++)
				{
					buffer[i].putSamples(next[i]);
					if(pending && buffer[i].getFillLevel() >= 0.5)
					{
						synchronized(blocker)
						{
							pending = false;
							blocker.notifyAll();
						}
					}
				}
			}
		}
		
	}
	
	/*----- Control -----*/
	
	public BufferedFilter(int buffer_size, int channels)
	{
		buffer = new FilterBuffer[channels];
		channel_capacity = buffer_size;
		for(int i = 0; i < channels; i++) buffer[i] = new FilterBuffer(channel_capacity);
		worker = new BufferedFilterWorker();
		blocker = new Boolean(false);
	}
	
	protected void startBuffering()
	{
		Thread t = new Thread(worker);
		t.setName(worker.getName());
		t.setDaemon(true);
		t.start();
	}
	
	public boolean bufferFull()
	{
		for(FilterBuffer b : buffer)
		{
			if(b.isFull()) return true;
		}
		return false;
	}
	
	public synchronized void reset()
	{
		if(!closed) close();
		
		//for(int i = 0; i < buffer.length; i++) buffer[i] = new FilterBuffer(channel_capacity);
		worker = new BufferedFilterWorker();
		closed = false;
		
		Thread t = new Thread(worker);
		t.setName(worker.getName());
		t.setDaemon(true);
		t.start();
	}
	
	public synchronized void close()
	{
		closed = true;
		worker.requestTermination();
		for(int i = 0; i < buffer.length; i++) buffer[i].clear();
	}
	
	protected synchronized void setBufferHold(boolean b)
	{
		hold = b;
	}
	
	protected synchronized void flushBuffer()
	{
		//DOES NOT STOP BUFFERING WORKER THREAD
		for(int i = 0; i < buffer.length; i++) buffer[i].clear();
	}
	
	protected abstract int[] generateNextSamples() throws InterruptedException;

	public int[] nextSample() throws InterruptedException
	{
		int ccount = buffer.length;
		int[] out = new int[ccount];
		
		for(int i = 0; i < ccount; i++)
		{
			while(buffer[i].isEmpty())
			{
				//Block if no buffered samples
				if(closed) return null;
				synchronized(blocker)
				{
					pending = true;
					blocker.wait();
				}
			}
			
			if(closed)return null;
			out[i] = buffer[i].popSample();
		}
		if(buffer[0].getFillLevel() < BUFFER_REFILL_THRESH) worker.interruptThreads();
		
		return out;
	}
	
}
