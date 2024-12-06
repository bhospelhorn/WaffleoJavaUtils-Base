package waffleoRai_SeqSound;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import waffleoRai_SoundSynth.SequenceController;

public class SeqEventLister implements SequenceController{
	
	/*----- Constants -----*/
	
	/*----- Instance Variables -----*/
	
	private Writer master_writer;
	private Writer[] channel_writers;
	private int timebase;
	
	private int tick = 0;
	
	/*----- Init -----*/
	
	public SeqEventLister(String path_stem, int res_ppqn) throws IOException{
		master_writer = new BufferedWriter(new FileWriter(path_stem + "_master.tsv"));
		channel_writers = new Writer[16];
		for(int i = 0; i < 16; i++){
			channel_writers[i] = new BufferedWriter(new FileWriter(String.format("%s_ch%02d.tsv", path_stem, i)));
		}
		timebase = res_ppqn;
	}
	
	/*----- Getters -----*/
	
	/*----- Setters -----*/
	
	/*----- Output -----*/
	
	private void writeln(String cmd, Writer w){
		try{
			w.write(Integer.toString(tick));
			w.write("\t");
			w.write(cmd);
			w.write("\n");
		}
		catch(IOException ex){ex.printStackTrace();}
	}
	
	/*----- Controller -----*/

	public void setMasterVolume(byte value) {
		writeln("svol " + value, master_writer);
	}

	public void setMasterExpression(byte value) {
		writeln("sexp " + value, master_writer);
	}

	public void setMasterPan(byte value) {
		writeln("span " + value, master_writer);
	}

	public void setTempo(int tempo_uspqn) {
		int bpm = MIDI.uspqn2bpm(tempo_uspqn, timebase);
		writeln("tempo " + bpm, master_writer);
	}

	public void addMarkerNote(String note) {
		writeln("mnote \"" + note + "\"", master_writer);
	}

	public void setTimeSignature(int beats, int div) {
		writeln("timesig " + beats + "|" + div, master_writer);
	}
	
	public void setChannelVolume(int ch, byte value) {
		writeln("cvol " + value, channel_writers[ch]);
	}

	public void setChannelExpression(int ch, byte value) {
		writeln("cexp " + value, channel_writers[ch]);
	}

	public void setChannelPan(int ch, byte value) {
		writeln("cpan " + value, channel_writers[ch]);
	}

	public void setChannelPriority(int ch, byte value) {
		writeln("cpri " + value, channel_writers[ch]);
	}

	public void setModWheel(int ch, short value) {
		writeln("modset 0x" + Integer.toHexString(value), channel_writers[ch]);
	}

	public void setPitchWheel(int ch, short value) {
		writeln("pwheel 0x" + Integer.toHexString(value), channel_writers[ch]);
	}

	public void setPitchBend(int ch, int cents) {
		writeln("pbendabs " + cents, channel_writers[ch]);
	}

	public void setPitchBendRange(int ch, int cents) {
		writeln("pbendrng " + cents, channel_writers[ch]);
	}

	public void setReverbSend(int ch, byte value) {
		writeln("rvb " + value, channel_writers[ch]);
	}
	
	public void setTremoloSend(int ch, byte value) {
		writeln("trml " + value, channel_writers[ch]);
	}
	
	public void setChorusSend(int ch, byte value) {
		writeln("chrs " + value, channel_writers[ch]);
	}

	public void setVibratoSpeed(int ch, double value) {
		writeln("vibspeed " + value, channel_writers[ch]);
	}

	public void setVibratoAmount(int ch, byte value) {
		writeln("vibamt " + value, channel_writers[ch]);
	}

	public void setPortamentoTime(int ch, short value) {
		writeln("porttime " + value, channel_writers[ch]);
	}

	public void setPortamentoAmount(int ch, byte value) {
		writeln("portamt " + value, channel_writers[ch]);
	}

	public void setPortamentoOn(int ch, boolean on) {
		if(on) writeln("porton", channel_writers[ch]);
		else writeln("portoff", channel_writers[ch]);
	}

	public void setLegato(int ch, boolean on) {
		if(on) writeln("legon", channel_writers[ch]);
		else writeln("legoff", channel_writers[ch]);
	}
	
	public void setProgram(int ch, int bank, int program) {
		writeln("chbp " + bank + " " + program, channel_writers[ch]);
	}

	public void setProgram(int ch, int program) {
		writeln("chprog " + program, channel_writers[ch]);
	}

	public void setControllerValue(int ch, int controller, byte value) {
		writeln("ctrlrset " + controller + " " + value, channel_writers[ch]);
	}

	public void setControllerValue(int ch, int controller, int value, boolean omitFine) {
		writeln("ctrlrset " + controller + " " + value, channel_writers[ch]);
	}

	public void setEffect1(int ch, byte value) {
		writeln("eff1 " + value, channel_writers[ch]);
	}

	public void setEffect2(int ch, byte value) {
		writeln("eff2 " + value, channel_writers[ch]);
	}

	public void addNRPNEvent(int ch, int index, int value, boolean omitFine) {
		writeln("nrpn " + index + " " + value, channel_writers[ch]);
	}

	public void noteOn(int ch, byte note, byte vel) {
		writeln("noteon " + note + " " + vel, channel_writers[ch]);
	}

	public void noteOff(int ch, byte note, byte vel) {
		writeln("noteoff " + note + " " + vel, channel_writers[ch]);
	}

	public void noteOff(int ch, byte note) {
		writeln("noteoff " + note, channel_writers[ch]);
	}

	public long advanceTick() {
		return tick++;
	}

	public void complete() {
		try{
			master_writer.close();
			for(int i = 0; i < 16; i++){
				channel_writers[i].close();
			}
		}
		catch(IOException ex){
			ex.printStackTrace();
		}
	}

}
