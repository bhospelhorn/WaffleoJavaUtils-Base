package waffleoRai_SeqSound;

import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

import waffleoRai_SoundSynth.SequenceController;

public class BasicMIDIGenerator implements SequenceController{
	
	private Sequence seq;
	private Track[] tracks;
	private long tick;
	
	private int bend_range = 200;
	
	private MidiMessageGenerator gen;
	
	public BasicMIDIGenerator(int ppq) throws InvalidMidiDataException{
		seq = new Sequence(Sequence.PPQ, ppq);
		gen = new MidiMessageGenerator();
		for(int i = 0; i < 17; i++) seq.createTrack();
		tracks = seq.getTracks();
		for(int i = 0; i < 17; i++){
			MidiMessage trackname = null;
			if(i == 0) trackname = gen.genTrackName("SeqControl");
			else trackname = gen.genTrackName("CHANNEL " + String.format("%02x", (i-1)));
			tracks[i].add(new MidiEvent(trackname,0));
		}
		tick = 0;
	}

	public Sequence getSequence(){return seq;}
	
	public void setMasterVolume(byte value) {
		try {
			List<MidiMessage> msgs = gen.genVolumeChange(0, value);
			for(MidiMessage msg : msgs){
				tracks[0].add(new MidiEvent(msg, tick));
			}
		} 
		catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}

	public void setMasterPan(byte value) {
		try {
			List<MidiMessage> msgs = gen.genPanChange(0, value);
			for(MidiMessage msg : msgs){
				tracks[0].add(new MidiEvent(msg, tick));
			}
		} 
		catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}

	public void setTempo(int tempo_uspqn) {
		try {
			MidiMessage msg = gen.genTempoSet(tempo_uspqn);
			tracks[0].add(new MidiEvent(msg, tick));
		} 
		catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}

	public void addMarkerNote(String note) {
		try {
			MidiMessage msg = gen.genMarker(note);
			tracks[0].add(new MidiEvent(msg, tick));
		} 
		catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}

	public void setChannelVolume(int ch, byte value) {
		try {
			List<MidiMessage> msgs = gen.genVolumeChange(ch, value);
			for(MidiMessage msg : msgs){
				tracks[ch+1].add(new MidiEvent(msg, tick));
			}
		} 
		catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}
	
	public void setChannelExpression(int ch, byte value){
		try {
			List<MidiMessage> msgs = gen.genExpressionChange(ch, value);
			for(MidiMessage msg : msgs){
				tracks[ch+1].add(new MidiEvent(msg, tick));
			}
		} 
		catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}

	public void setChannelPan(int ch, byte value) {
		try {
			List<MidiMessage> msgs = gen.genPanChange(ch, value);
			for(MidiMessage msg : msgs){
				tracks[ch+1].add(new MidiEvent(msg, tick));
			}
		} 
		catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}

	public void setChannelPriority(int ch, byte value) {}

	public void setModWheel(int ch, short value){
		try {
			List<MidiMessage> msgs = gen.genModWheelLevel(ch, (int)value);
			for(MidiMessage msg : msgs){
				tracks[ch+1].add(new MidiEvent(msg, tick));
			}
		} 
		catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}

	public void setPitchWheel(int ch, short value) {
		try {
			MidiMessage msg = gen.genPitchBend(ch, (int)value);
			tracks[ch+1].add(new MidiEvent(msg, tick));
		} 
		catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}

	public void setPitchBend(int ch, int cents) {
		try {
			MidiMessage msg = gen.genPitchBend(ch, cents, bend_range/100);
			tracks[ch+1].add(new MidiEvent(msg, tick));
		} 
		catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}
	
	public void setPitchBendRange(int ch, int cents){
		bend_range = cents;
		try {
			List<MidiMessage> msgs = gen.genPitchBendRangeSet(ch, cents/100, cents%100);
			for(MidiMessage msg : msgs){
				tracks[ch+1].add(new MidiEvent(msg, tick));
			}
		} 
		catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}

	public void setProgram(int ch, int bank, int program) {
		try {
			List<MidiMessage> msgs = gen.genBankSelect(ch, bank);
			msgs.add(gen.genProgramChange(ch, program));
			for(MidiMessage msg : msgs){
				tracks[ch+1].add(new MidiEvent(msg, tick));
			}
		} 
		catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}

	public void setProgram(int ch, int program) {
		try {
			MidiMessage msg = gen.genProgramChange(ch, program);
			tracks[ch+1].add(new MidiEvent(msg, tick));
		} 
		catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}

	public void noteOn(int ch, byte note, byte vel) {
		try {
			MidiMessage msg = gen.genNoteOn(ch, Byte.toUnsignedInt(note), Byte.toUnsignedInt(vel));
			tracks[ch+1].add(new MidiEvent(msg, tick));
		} 
		catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}

	public void noteOff(int ch, byte note, byte vel) {
		try {
			MidiMessage msg = gen.genNoteOff(ch, Byte.toUnsignedInt(note), Byte.toUnsignedInt(vel));
			tracks[ch+1].add(new MidiEvent(msg, tick));
		} 
		catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}

	public void noteOff(int ch, byte note) {
		try {
			MidiMessage msg = gen.genNoteOff(ch, Byte.toUnsignedInt(note));
			tracks[ch+1].add(new MidiEvent(msg, tick));
		} 
		catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}
	
	public void addNRPNEvent(int ch, int index, int value, boolean omitFine){
		try {
			List<MidiMessage> msgs = gen.genNRPN(ch, index, value, omitFine);
			for(MidiMessage msg : msgs){
				tracks[ch+1].add(new MidiEvent(msg, tick));
			}
		} 
		catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}
	
	public long advanceTick(){
		return ++tick;
	}
	
	public void complete(){
		//Don't forget track end
		if(tracks != null){
			try{
				for(int i = 0; i < tracks.length; i++){
					tracks[i].add(new MidiEvent(gen.genTrackEnd(), tick));
				}
			}
			catch(Exception ex){
				ex.printStackTrace();
			}
		}
	}

}
