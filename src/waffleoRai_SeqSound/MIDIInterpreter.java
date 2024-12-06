package waffleoRai_SeqSound;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

import waffleoRai_SoundSynth.SequenceController;

public class MIDIInterpreter {
	
	/*
	 * Recognizes track name "master" (not case-sensitive)
	 * Recognized controller commands on this track are 
	 * forwarded to the target as master commands, not to channel
	 */
	
	private Sequence seq;
	private long tick;
	
	private SequenceController dest;
	private MappedTrack[] play_tracks;
	private ChannelStatus[] channel_values;
	
	private static class ChannelStatus{
		
		//Highest bit is set if value has changed
		// 	and update needs to be sent to the target
		
		public int ch_idx;
		
		private boolean[] nrpnflags;
		private int data;
		private int nrpn_id;
		private Deque<Integer> data_deque;
		private Deque<Integer> nrpn_deque;
		
		public int pan;
		public int expression;
		public int ch_vol;
		public int bank;
		public int mod;
		public int port_time;
		public int eff1;
		public int eff2;
		
		public int rvb_send;
		public int chrs_send;
		public int trml_send;
		
		public int master_vol;
		
		public ChannelStatus(int idx){
			ch_idx = idx;
			data_deque = new LinkedList<Integer>();
			nrpn_deque = new LinkedList<Integer>();
			nrpnflags = new boolean[2];
		}
		
		public void updateTarget(SequenceController target){
			final int mask1 = 0x80000000;
			final int mask2 = 0x7fffffff;
			if((pan & mask1) != 0) target.setChannelPan(ch_idx, (byte)(pan >>> 8));
			pan &= mask2;
			if((expression & mask1) != 0) target.setChannelExpression(ch_idx, (byte)(expression>>>8));
			expression &= mask2;
			if((ch_vol & mask1) != 0) target.setChannelVolume(ch_idx, (byte)(ch_vol>>>8));
			ch_vol &= mask2;
			if((master_vol & mask1) != 0) target.setMasterVolume((byte)(master_vol>>>8));
			master_vol &= mask2;
			if((bank & mask1) != 0) target.setProgram(ch_idx, bank & mask2, 0);
			bank &= mask2;
			if((mod & mask1) != 0) target.setModWheel(ch_idx, (short)mod);
			mod &= mask2;
			if((eff1 & mask1) != 0) target.setEffect1(ch_idx, (byte)(eff1 >>> 8));
			eff1 &= mask2;
			if((eff2 & mask1) != 0) target.setEffect2(ch_idx, (byte)(eff2 >>> 8));
			eff2 &= mask2;
			if((rvb_send & mask1) != 0) target.setReverbSend(ch_idx, (byte)(rvb_send));
			rvb_send &= mask2;
			if((chrs_send & mask1) != 0) target.setChorusSend(ch_idx, (byte)(chrs_send));
			chrs_send &= mask2;
			if((trml_send & mask1) != 0) target.setTremoloSend(ch_idx, (byte)(trml_send));
			trml_send &= mask2;
			if((port_time & mask1) != 0) target.setControllerValue(ch_idx, MIDIControllers.PORTAMENTO_TIME, port_time & mask2, false);
			port_time &= mask2;
			
			while(!nrpn_deque.isEmpty()){
				target.addNRPNEvent(ch_idx, nrpn_deque.pop(), data_deque.pop(), false);
			}
			if((nrpn_id & mask1) != 0) target.addNRPNEvent(ch_idx, nrpn_id & mask2, data & mask2, false);
			nrpn_id &= mask2;
			data &= mask2;
		}
		
		public void addNRPN(byte val, boolean msb){
			//See if byte has already been written for current NRPN
			final int mask1 = 0x80000000;
			final int mask2 = 0x7fffffff;
			if((nrpn_id & mask1) != 0){
				if((msb && nrpnflags[0]) || (!msb && nrpnflags[1])){
					//Add to deque
					nrpn_deque.add(nrpn_id & mask2);
					data_deque.add(data & mask2);
					nrpn_id = 0; data = 0;
					nrpnflags[0] = false;
					nrpnflags[1] = false;
				}
			}
			int ival = Byte.toUnsignedInt(val);
			if(msb){ival <<= 8; nrpnflags[0] = true;}
			else nrpnflags[1] = true;
			nrpn_id |= ival;
			nrpn_id |= mask1;
		}
		
		public static int writeValueTo(int initval, byte val, boolean msb){
			final int mask1 = 0x80000000;
			int outval = initval;
			if((outval & mask1) != 0){
				//Clear the target byte
				if(msb) outval &= 0xFF;
				else outval &= 0xFF00;
			}
			int ival = Byte.toUnsignedInt(val);
			if(msb) ival <<= 8;
			outval |= ival;
			outval |= mask1;
			return outval;
		}
		
		public void addData(byte val, boolean msb){
			data = writeValueTo(data, val, msb);
		}
		
	}
	
	private static class MappedTrack{
		
		public Map<Long, List<MidiMessage>> events;
		public boolean is_master;
		public boolean end_flag;
		
		public MappedTrack(){
			events = new HashMap<Long, List<MidiMessage>>();
			end_flag = false;
		}
		
		public void putEvent(MidiEvent event){
			List<MidiMessage> elist = events.get(event.getTick());
			if(elist == null){
				elist = new LinkedList<MidiMessage>();
				events.put(event.getTick(), elist);
			}
			elist.add(event.getMessage());
		}
		
	}
	
	public MIDIInterpreter(Sequence midi_seq){
		seq = midi_seq;
	}
	
	private void handleControllerMessage(MidiMessage msg, MappedTrack track, int channel){
		//Method for handling the 0xBx commands
		if(channel_values[channel] == null) return;
		ChannelStatus cs = channel_values[channel];
		byte[] mbytes = msg.getMessage();
		switch(mbytes[1]){
		case MIDIControllers.BANK_SELECT: //Bank select (hi)
			cs.bank = ChannelStatus.writeValueTo(cs.bank, mbytes[2], true);
			break;
		case MIDIControllers.MOD_WHEEL: //mod wheel (hi)
			cs.mod = ChannelStatus.writeValueTo(cs.mod, mbytes[2], true);
			break;
		case MIDIControllers.PORTAMENTO_TIME: //Portamento time (hi)
			cs.port_time = ChannelStatus.writeValueTo(cs.port_time, mbytes[2], true);
			break;
		case MIDIControllers.DATA_ENTRY: //Data (hi)
			cs.addData(mbytes[2], true);
			break;
		case MIDIControllers.VOLUME: //Channel volume (hi)
			if(!track.is_master) cs.ch_vol = ChannelStatus.writeValueTo(cs.ch_vol, mbytes[2], true);
			else cs.master_vol = ChannelStatus.writeValueTo(cs.master_vol, mbytes[2], true);
			break;
		case MIDIControllers.PAN: //Pan (hi)
			cs.pan = ChannelStatus.writeValueTo(cs.pan, mbytes[2], true);
			break;
		case MIDIControllers.EXPRESSION: //Expression (hi)
			if(!track.is_master) cs.expression = ChannelStatus.writeValueTo(cs.expression, mbytes[2], true);
			else cs.master_vol = ChannelStatus.writeValueTo(cs.master_vol, mbytes[2], true);
			break;
		case MIDIControllers.EFFECTS_1: //Effect 1 (hi)
			cs.eff1 = ChannelStatus.writeValueTo(cs.eff1, mbytes[2], true);
			break;
		case MIDIControllers.EFFECTS_2: //Effect 2 (hi)
			cs.eff2 = ChannelStatus.writeValueTo(cs.eff2, mbytes[2], true);
			break;
		case MIDIControllers.TREMOLO_SEND:
			cs.trml_send = ChannelStatus.writeValueTo(cs.trml_send, mbytes[2], true);
			break;
		case MIDIControllers.REVERB_SEND:
			cs.rvb_send = ChannelStatus.writeValueTo(cs.rvb_send, mbytes[2], true);
			break;
		case MIDIControllers.CHORUS_SEND:
			cs.chrs_send = ChannelStatus.writeValueTo(cs.chrs_send, mbytes[2], true);
			break;
		case MIDIControllers.BANK_SELECT_LSB: //Bank select (lo)
			cs.bank = ChannelStatus.writeValueTo(cs.bank, mbytes[2], false);
			break;
		case MIDIControllers.MOD_WHEEL_LSB: //mod wheel (lo)
			cs.mod = ChannelStatus.writeValueTo(cs.mod, mbytes[2], false);
			break;
		case MIDIControllers.PORTAMENTO_TIME_LSB: //Portamento time (lo)
			cs.port_time = ChannelStatus.writeValueTo(cs.port_time, mbytes[2], false);
			break;
		case MIDIControllers.DATA_ENTRY_LSB: //Data (lo)
			cs.addData(mbytes[2], false);
			break;
		case MIDIControllers.VOLUME_LSB: //Channel volume (lo)
			if(!track.is_master) cs.ch_vol = ChannelStatus.writeValueTo(cs.ch_vol, mbytes[2], false);
			else cs.master_vol = ChannelStatus.writeValueTo(cs.master_vol, mbytes[2], false);
			break;
		case MIDIControllers.PAN_LSB: //Pan (lo)
			cs.pan = ChannelStatus.writeValueTo(cs.pan, mbytes[2], false);
			break;
		case MIDIControllers.EXPRESSION_LSB: //Expression (lo)
			if(!track.is_master) cs.expression = ChannelStatus.writeValueTo(cs.expression, mbytes[2], false);
			else cs.master_vol = ChannelStatus.writeValueTo(cs.master_vol, mbytes[2], false);
			break;
		case MIDIControllers.EFFECTS_1_LSB: //Effect 1 (lo)
			cs.eff1 = ChannelStatus.writeValueTo(cs.eff1, mbytes[2], false);
			break;
		case MIDIControllers.EFFECTS_2_LSB: //Effect 2 (lo)
			cs.eff2 = ChannelStatus.writeValueTo(cs.eff2, mbytes[2], false);
			break;
		case MIDIControllers.NRPN_LSB: //NRPN ID (lo)
			cs.addNRPN(mbytes[2], false);
			break;
		case MIDIControllers.NRPN_MSB: //NRPN ID (hi)
			cs.addNRPN(mbytes[2], true);
			break;
		}
	}
	
	private void handleSystemMessage(MidiMessage msg, MappedTrack track){
		//Method for handling 0xFx commands
		//Only recognizes 0xff commands at this time.
		/*
		 * Recognizes the following meta commands
		 * 01, 06, 07 as text cues
		 * 2f - end of track
		 * 51 - set tempo
		 * 58 - set time signature
		 */
		if(msg.getStatus() == 0xFF){
			byte[] mbytes = msg.getMessage();
			switch(mbytes[1]){
			case MIDIMetaCommands.TEXT:
			case MIDIMetaCommands.MARKER:
			case MIDIMetaCommands.CUE:
				String txt = readTextMessage(msg);
				dest.addMarkerNote(txt);
				break;
			case MIDIMetaCommands.END:
				track.end_flag = true;
				break;
			case MIDIMetaCommands.TEMPO:
				int tempo = 0;
				tempo |= Byte.toUnsignedInt(mbytes[3]) << 16;
				tempo |= Byte.toUnsignedInt(mbytes[4]) << 8;
				tempo |= Byte.toUnsignedInt(mbytes[5]);
				dest.setTempo(tempo);
				break;
			case MIDIMetaCommands.TIMESIG:
				int nn = Byte.toUnsignedInt(mbytes[0]);
				int dd = Byte.toUnsignedInt(mbytes[1]);
				dd = 1 << dd;
				dest.setTimeSignature(nn, dd);
				break;
			}
		}
	}
	
	private void readMessage(MidiMessage msg, MappedTrack track){
		int status = msg.getStatus();
		int stat_hi = status >>> 4;
		int chan = status & 0xF;
		byte[] mbytes = msg.getMessage();
		
		switch(stat_hi){
		case 0x8: 
			//Note off
			dest.noteOff(chan, mbytes[1], mbytes[2]);
			break;
		case 0x9:
			//Note on
			dest.noteOn(chan, mbytes[1], mbytes[2]);
			break;
		case 0xa:
			//Aftertouch. Ignore.
			break;
		case 0xb:
			//Controller message. Send to method.
			handleControllerMessage(msg, track, chan);
			break;
		case 0xc:
			//Change program
			dest.setProgram(chan, mbytes[1]);
			break;
		case 0xd:
			//Channel Pressure. Ignore.
			break;
		case 0xe:
			//Pitch Bend
			int val = 0;
			val |= (int)mbytes[1];
			val |= (int)mbytes[2] << 7;
			dest.setPitchWheel(chan, (short)val);
			break;
		case 0xf:
			//Meta/System message. Send to method.
			handleSystemMessage(msg, track);
			break;
		}
		
	}
	
	private String readTextMessage(MidiMessage msg){
		if(msg == null) return null;
		if(msg.getStatus() != 0xFF) return null;
		byte[] bytes = msg.getMessage();
		if(bytes[1] > 7) return null;
		if(bytes[1] < 1) return null;
		int len = Byte.toUnsignedInt(bytes[2]);
		StringBuilder sb = new StringBuilder(len+1);
		for(int i = 0; i < len; i++){
			sb.append((char)bytes[3+i]);
		}
		return sb.toString();
	}
	
	public void readMIDITo(SequenceController target){
		if(target == null || seq == null) return;
		tick = 0;
		dest = target;
		
		channel_values = new ChannelStatus[16];
		for(int i = 0; i < 16; i++){
			channel_values[i] = new ChannelStatus(i);
		}
		
		//Rearrange data in tracks to make it easier to read them all at once
		Track[] tracks = seq.getTracks();
		play_tracks = new MappedTrack[tracks.length];
		for(int i = 0; i < tracks.length; i++){
			Track t = tracks[i];
			if(t == null) continue;
			MappedTrack pt = new MappedTrack();
			play_tracks[i] = pt;
			
			int tsize = t.size();
			for(int j = 0; j < tsize; j++){
				MidiEvent ev = t.get(j);
				MidiMessage msg = ev.getMessage();
				pt.putEvent(ev);
				//Check if master track.
				if(msg.getStatus() == 0xFF){
					byte[] bytes = msg.getMessage();
					if(bytes[1] != 0x03) continue;
					String tname = readTextMessage(msg).toLowerCase();
					pt.is_master = (tname.equals("master"));
				}
			}
		}
		
		//
		boolean all_end = false;
		while(!all_end){
			all_end = true;
			for(int i = 0; i < tracks.length; i++){
				if(play_tracks[i] == null) continue;
				all_end = all_end && play_tracks[i].end_flag;
				
				//Play all events for current tick.
				List<MidiMessage> tick_events = play_tracks[i].events.get(tick);
				if(tick_events != null && !tick_events.isEmpty()){
					for(MidiMessage m : tick_events) readMessage(m, play_tracks[i]);
				}
			}	
			//Flush channels for tick (so as to not sent multiple controller value commands
			for(int i = 0; i < 16; i++){
				channel_values[i].updateTarget(dest);
			}
			tick++;
		}
		
	}

}
