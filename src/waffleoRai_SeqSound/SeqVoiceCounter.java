package waffleoRai_SeqSound;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import waffleoRai_SoundSynth.SequenceController;

public class SeqVoiceCounter implements SequenceController{
	
	/*----- Constants -----*/
	
	/*----- Instance Variables -----*/
	
	private int tick;
	
	private List<byte[]> time_vcount;
	private int[] current_vcount;
	private int[] max_vcount;
	
	private int max_total_vcount; //Max on AT ONE TIME
	
	/*----- Init -----*/
	
	public SeqVoiceCounter(){
		tick = 0;
		time_vcount = new LinkedList<byte[]>();
		current_vcount = new int[16];
		max_vcount = new int[16];
		max_total_vcount = 0;
	}
	
	public void reset(){
		tick = 0;
		time_vcount.clear();
		Arrays.fill(current_vcount, 0);
		Arrays.fill(max_vcount, 0);
		max_total_vcount = 0;
	}
	
	/*----- Getters -----*/
	
	public int getCurrentVoiceCount(int channel){
		return current_vcount[channel];
	}
	
	public int getCurrentTotalVoiceCount(){
		int c = 0;
		for(int i = 0; i < 16; i++) c += current_vcount[i];
		return c;
	}
	
	public int getMaxVoiceCount(int channel){
		return max_vcount[channel];
	}
	
	public int getMaxTotalVoiceCount(){
		return max_total_vcount;
	}
	
	public byte[] getVoiceUsage(int channel){
		int tcount = time_vcount.size();
		byte[] out = new byte[tcount];
		int i = 0;
		for(byte[] tvals : time_vcount) out[i++] = tvals[channel];
		return out;
	}
	
	public byte[][] getVoiceUsageAllChannels(){
		int tcount = time_vcount.size();
		byte[][] out = new byte[16][tcount];
		int i = 0;
		for(byte[] tvals : time_vcount){
			for(int j = 0; j < 16; j++){
				out[j][i] = tvals[j];
			}
			i++;
		}
		return out;
	}
	
	public byte[] getVoiceUsageTotal(){
		int tcount = time_vcount.size();
		byte[] out = new byte[tcount];
		int i = 0;
		for(byte[] tvals : time_vcount){
			for(int j = 0; j < 16; j++){
				out[i] += tvals[j];
			}
			i++;
		}
		return out;
	}
	
	public int getTick(){return tick;}
	
	/*----- Setters -----*/
	
	/*----- Output -----*/
	
	/*----- Controller -----*/
	
	public void setMasterVolume(byte value) {}
	public void setMasterExpression(byte value) {}
	public void setMasterPan(byte value) {}
	public void setTempo(int tempo_uspqn) {}
	public void addMarkerNote(String note) {}
	public void setTimeSignature(int beats, int div) {}
	public void setChannelVolume(int ch, byte value) {}
	public void setChannelExpression(int ch, byte value) {}
	public void setChannelPan(int ch, byte value) {}
	public void setChannelPriority(int ch, byte value) {}
	public void setModWheel(int ch, short value) {}
	public void setPitchWheel(int ch, short value) {}
	public void setPitchBend(int ch, int cents) {}
	public void setPitchBendRange(int ch, int cents) {}
	public void setReverbSend(int ch, byte value) {}
	public void setTremoloSend(int ch, byte value) {}
	public void setChorusSend(int ch, byte value) {}
	public void setVibratoSpeed(int ch, double value) {}
	public void setVibratoAmount(int ch, byte value) {}
	public void setPortamentoTime(int ch, short value) {}
	public void setPortamentoAmount(int ch, byte value) {}
	public void setPortamentoOn(int ch, boolean on) {}
	public void setLegato(int ch, boolean on) {}
	public void setProgram(int ch, int bank, int program) {}
	public void setProgram(int ch, int program) {}
	public void setControllerValue(int ch, int controller, byte value) {}
	public void setControllerValue(int ch, int controller, int value, boolean omitFine) {}
	public void setEffect1(int ch, byte value) {}
	public void setEffect2(int ch, byte value) {}
	public void addNRPNEvent(int ch, int index, int value, boolean omitFine) {}

	public void noteOn(int ch, byte note, byte vel) {
		current_vcount[ch]++;
		if(current_vcount[ch] > max_vcount[ch]){
			max_vcount[ch] = current_vcount[ch];
		}
	}

	public void noteOff(int ch, byte note, byte vel) {
		current_vcount[ch] = Integer.max(0, current_vcount[ch]-1);
	}

	public void noteOff(int ch, byte note) {noteOff(ch, note, (byte)0);}

	public long advanceTick() {
		byte[] lastt = new byte[16];
		int v_on = 0;
		for(int i = 0; i < 16; i++){
			lastt[i] = (byte)current_vcount[i];
			v_on += current_vcount[i];
		}
		time_vcount.add(lastt);
		if(v_on > max_total_vcount) max_total_vcount = v_on;
		return tick++;
	}

	public void complete() {}

}
