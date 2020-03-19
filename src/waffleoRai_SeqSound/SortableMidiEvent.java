package waffleoRai_SeqSound;

import javax.sound.midi.MidiEvent;

public class SortableMidiEvent implements Comparable<SortableMidiEvent>{

	private MidiEvent event;
	
	public SortableMidiEvent(MidiEvent e)
	{
		if (e == null) throw new IllegalArgumentException();
		event = e;
	}
	
	public MidiEvent getEvent()
	{
		return event;
	}
	
	public int hashCode()
	{
		if (event == null) return 0;
		return event.hashCode();
	}
	
	public boolean equals(Object o)
	{
		if (event == null) return false;
		return event.equals(o);
	}
	
	@Override
	public int compareTo(SortableMidiEvent o) 
	{
		if (o == null) return 1;
		//Compare time coordinates
		if (o.getEvent().getTick() > this.getEvent().getTick()) return -1;
		else if (o.getEvent().getTick() < this.getEvent().getTick()) return 1;
		
		//Then by status type... Sysex, then meta, then control, then note on/off
		int ostat = o.getEvent().getMessage().getStatus();
		int tstat = this.getEvent().getMessage().getStatus();
		int otype = (ostat >>> 4) & 0xF;
		int ttype = (tstat >>> 4) & 0xF;
		
		if(otype != ttype)
		{
			if(otype == 0xF) return 1;
			if(ttype == 0xF) return -1;
			
			if(otype == 0xC) return 1;
			if(ttype == 0xC) return -1;
			
			if(otype == 0xB) return 1;
			if(ttype == 0xB) return -1;
			
			return ttype - otype;
		}
		
		//Sort by channel
		int och = ostat & 0xF;
		int tch = tstat & 0xF;
		if(och != tch) return tch-och;
		
		//Sort by status
		return tstat - ostat;

	}

}
