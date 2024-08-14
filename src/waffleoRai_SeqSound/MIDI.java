package waffleoRai_SeqSound;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;

import waffleoRai_Utils.BitStreamer;
import waffleoRai_Utils.BufferReference;
import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.FileBuffer.UnsupportedFileTypeException;

public class MIDI {
	
	public static final String HEADER_MAG = "MThd";
	public static final String TRACK_MAG = "MTrk";
	public static final String EXTENSION = "mid";
	
	public static final String[] NOTES = {"C", "C#", "D", "Eb", 
										  "E", "F", "F#", "G",
										  "Ab", "A", "Bb", "B"};
	
	//Right now, only writes MIDI 1
	
	private int MIDI_type;
	//private int NumTracks;
	
	private boolean divMode;
	private int TicksPerQNote;
	private int FramesPerSec;
	private int TicksPerFrame;
	
	private Sequence contents;
	
	private boolean useName;
	private String internalName;
	
	private String tempDir;
	//private int tempNum;
	
	private List<String> diskClutter;
	
	/*--- Helper Objects ---*/
	
	public enum MessageType
	{
		MIDI,
		META,
		SYSEX,
		RUNNING;
	}
	
	/*--- Constructors ---*/
	
	public MIDI(Sequence seq){
		this.contents = seq;
		this.internalName = "MIDI" + this.hashCode();
		this.useName = false;
		this.diskClutter = new LinkedList<String>();
		this.MIDI_type = 1;
		this.tempDir = null;
		//this.tempNum = 0;
		readDivisionInfo();
	}
	
	public MIDI(FileBuffer mySeq) throws UnsupportedFileTypeException{
		this(mySeq.getReferenceAt(0L));
	}
	
	public MIDI(FileBuffer mySeq, long stPos) throws UnsupportedFileTypeException{
		this(mySeq.getReferenceAt(stPos));
	}
	
	public MIDI(BufferReference data) throws UnsupportedFileTypeException{
		this.contents = null;
		this.internalName = "MIDI" + this.hashCode();
		this.useName = false;
		this.diskClutter = new LinkedList<String>();
		this.MIDI_type = 1;
		this.tempDir = null;
		//this.tempNum = 0;
		parseMIDI(data);
	}
	
	private void readDivisionInfo(){
		float divisionType = contents.getDivisionType();
		if (divisionType == Sequence.PPQ){
			this.divMode = false;
			this.TicksPerQNote = contents.getResolution();
			return;
		}
		else if (divisionType == Sequence.SMPTE_24){
			this.divMode = true;
			this.FramesPerSec = -24;
			this.TicksPerFrame = contents.getResolution();
			return;
		}
		else if (divisionType == Sequence.SMPTE_25){
			this.divMode = true;
			this.FramesPerSec = -25;
			this.TicksPerFrame = contents.getResolution();
			return;
		}
		else if (divisionType == Sequence.SMPTE_30)	{
			this.divMode = true;
			this.FramesPerSec = -30;
			this.TicksPerFrame = contents.getResolution();
			return;
		}
		else if (divisionType == Sequence.SMPTE_30DROP){
			this.divMode = true;
			this.FramesPerSec = -29;
			this.TicksPerFrame = contents.getResolution();
			return;
		}
	}
	
	/*--- Getters ---*/
	
	public String getInternalName(){
		return this.internalName;
	}
	
	public boolean nameEncoded(){
		return this.useName;
	}
	
	public Sequence getSequence(){
		return this.contents;
	}
	
	public int getTPQN(){return this.TicksPerQNote;}
	
	/*--- Setters ---*/
	
	public void setInternalName(String name){
		this.internalName = name;
		this.useName = true;
	}
	
	public void setWriteBufferDir(String path){
		this.tempDir = path;
	}
	
	/*--- Parsing ---*/
	
	private MessageType getMessageType(byte stat){
		int istat = Byte.toUnsignedInt(stat);
		if (istat == 0xFF) return MessageType.META;
		else if(istat == 0xF0 || istat == 0xF7) return MessageType.SYSEX;
		else{
			if (BitStreamer.readABit(stat, 7) && (((istat >> 4) & 0xF) != 0xF)){
				return MessageType.MIDI;
			}
			else if (!BitStreamer.readABit(stat, 7)) return MessageType.RUNNING;
		}
		
		return null;
	}
	
	private int getFieldNumber(byte stat){
		int sInd = Byte.toUnsignedInt(stat);
		sInd = (sInd >> 4) & 0xF;
		if (sInd == 0x8) return 2;
		else if (sInd == 0x9) return 2;
		else if (sInd == 0xA) return 2;
		else if (sInd == 0xB) return 2;
		else if (sInd == 0xC) return 1;
		else if (sInd == 0xD) return 1;
		else if (sInd == 0xE) return 2;
			
		return -1;
	}
	
	private void readDiv(short d){
		this.divMode = BitStreamer.readABit(d, 15);
		if (this.divMode){
			int id = Short.toUnsignedInt(d);
			this.FramesPerSec = ((id >> 8) & 0x7F); //Maintained negative.
			this.TicksPerFrame = id & 0xFF;
		}
		else{
			this.TicksPerQNote = (int)d;
		}
	}
	
	private float getDivType(){
		if (!this.divMode) return Sequence.PPQ;
		else{
			switch (this.FramesPerSec){
			case -24: return Sequence.SMPTE_24;
			case -25: return Sequence.SMPTE_25;
			case -29: return Sequence.SMPTE_30DROP;
			case -30: return Sequence.SMPTE_30;
			default: break;				
			}
		}
		
		return -1;
	}
	
	private int getDivRes(){
		if (this.divMode) return this.TicksPerFrame;
		else return this.TicksPerQNote;
	}
	
	private int parseHeader(BufferReference data) throws UnsupportedFileTypeException {
		data.setByteOrder(true);
		String mstr = data.nextASCIIString(4);
		if(!HEADER_MAG.equals(mstr)) throw new UnsupportedFileTypeException("MIDI.parseHeader || MIDI magic number not found!");
		data.add(4L);
		short format = data.nextShort();
		if(format != 1) {
			throw new UnsupportedFileTypeException("MIDI.parseHeader || MIDI is not type 1 MIDI! Not supported.");
		}
		
		MIDI_type = format;
		short tracks = data.nextShort();
		short div = data.nextShort();
		readDiv(div);
		
		return (int)tracks;
	}
	
	public static int[] getVLQ(FileBuffer mySeq, long pos){
		//0 index is the value
		//1 is the number of bytes it took up
		boolean zHit = false;
		int[] VLQ = new int[2];
		long cPos = pos;
		int val = 0;
		while (!zHit){
			byte b = mySeq.getByte(cPos);
			cPos++;
			zHit = !(BitStreamer.readABit(b, 7));
			int ib = Byte.toUnsignedInt(b);
			val = (val << 7) | (ib & 0x7F);
		}
		VLQ[0] = val;
		VLQ[1] = (int)(cPos - pos);
		return VLQ;
	}
	
	public static int getVLQ(BufferReference ref){
		boolean zHit = false;
		int val = 0;
		while (!zHit){
			byte b = ref.nextByte();
			zHit = !(BitStreamer.readABit(b, 7));
			int ib = Byte.toUnsignedInt(b);
			val = (val << 7) | (ib & 0x7F);
		}
		return val;
	}

	private int parseTrack(BufferReference data, Track t) throws UnsupportedFileTypeException {
		long stpos = data.getBufferPosition();
		String mstr = data.nextASCIIString(4);
		if(!TRACK_MAG.equals(mstr)) throw new UnsupportedFileTypeException("MIDI.parseTrack || Track magic number not found!");
		int datLen = data.nextInt();
		long tEd = data.getBufferPosition() + Integer.toUnsignedLong(datLen);
		
		long tickPos = 0;
		MidiMessage lastmsg = null;
		
		while (data.getBufferPosition() < tEd){
			//1. Get delta time
			int delta = getVLQ(data);
			
			//2. Interpret delta time
			tickPos += Integer.toUnsignedLong(delta);
			
			//3. Figure out message type
			byte stat = data.nextByte();
			MessageType type = getMessageType(stat);
			if (type == null) throw new UnsupportedFileTypeException("MIDI.parseTrack || Did not recognize command: " + String.format("%02x", stat));
			
			//4. Get message length
			//5. Copy message
			//6. Add to track
			
			switch(type){
			case META:
				byte metaType = data.nextByte();
				int mLen = getVLQ(data);
				
				byte[] message = new byte[mLen];
				for (int i = 0; i < mLen; i++){
					message[i] = data.nextByte();
				}
				
				MidiMessage m;
				try {
					m = new MetaMessage(metaType, message, mLen);
					t.add(new MidiEvent(m, tickPos));
					lastmsg = m;
				} 
				catch (InvalidMidiDataException e){
					e.printStackTrace();
					throw new UnsupportedFileTypeException("MIDI.parseTrack || MIDI data was invalid for meta command " + String.format("%02x", metaType));
				}
				break;
			case MIDI:
				int bNum = getFieldNumber(stat);
				if (!(bNum == 1 || bNum == 2)) throw new UnsupportedFileTypeException("MIDI.parseTrack || Did not recognize as MIDI channel command: " + String.format("%02x", stat));
				int istat = Byte.toUnsignedInt(stat);
				
				int dat1 = Byte.toUnsignedInt(data.nextByte());
				int dat2 = 0;
				
				if (bNum == 2){
					dat2 = Byte.toUnsignedInt(data.nextByte());
				}
				
				try {
					MidiMessage mMess = new ShortMessage(istat, dat1, dat2);
					t.add(new MidiEvent(mMess, tickPos));
					lastmsg = mMess;
				} 
				catch (InvalidMidiDataException e){
					e.printStackTrace();
					throw new UnsupportedFileTypeException("MIDI.parseTrack || MIDI data was invalid for regular command " + String.format("%02x", stat));
				}
				
				break;
			case SYSEX:
				int smlen = getVLQ(data);
				
				byte[] sdata = new byte[smlen];
				for (int i = 0; i < smlen; i++){
					sdata[i] = data.nextByte();
				}
				
				try {
					MidiMessage sMess = new SysexMessage(Byte.toUnsignedInt(stat), sdata, smlen);
					t.add(new MidiEvent(sMess, tickPos));
					lastmsg = sMess;
				} 
				catch (InvalidMidiDataException e){
					e.printStackTrace();
					throw new UnsupportedFileTypeException("MIDI.parseTrack || MIDI data was invalid for sysex command " + String.format("%02x", stat));
				}
				
				break;
			case RUNNING:
				//In standard MIDI, running status can only apply to direct MIDI events
				//(No status 0xFn events)
				if(lastmsg == null){
					throw new UnsupportedFileTypeException("MIDI.parseTrack || Assumed running status, but nothing to continue run off of.");
				}
				
				int lstat = lastmsg.getStatus();
				MessageType stype = getMessageType((byte)lstat);
				if (stype != MIDI.MessageType.MIDI) {
					throw new UnsupportedFileTypeException("MIDI.parseTrack || Assumed running status, but previous command is not compatible.");
				}
				
				int cmd = (lstat >>> 4) & 0xF;
				if (cmd == 0xC || cmd == 0xD){
					//Only 1 byte data
					int d1 = Byte.toUnsignedInt(stat);
					try{
						ShortMessage msg = new ShortMessage(lstat, d1, 0);
						t.add(new MidiEvent(msg, tickPos));
						lastmsg = msg;
					} 
					catch (InvalidMidiDataException e) {
						e.printStackTrace();
						throw new UnsupportedFileTypeException("MIDI.parseTrack || MIDI data was invalid for command " + String.format("%02x", stat));
					}
				}
				else{
					int d1 = Byte.toUnsignedInt(stat);
					int d2 = Byte.toUnsignedInt(data.nextByte());

					try {
						ShortMessage msg = new ShortMessage(lstat, d1, d2);
						t.add(new MidiEvent(msg, tickPos));
						lastmsg = msg;
					} 
					catch (InvalidMidiDataException e) {
						e.printStackTrace();
						throw new UnsupportedFileTypeException("MIDI.parseTrack || MIDI data was invalid for command " + String.format("%02x", stat));
					}
				}
				
				break;
			default:
				throw new UnsupportedFileTypeException("MIDI.parseTrack || Could not read command: " + String.format("%02x", stat));
			}
			
		}
		
		
		return (int)(data.getBufferPosition() - stpos);
	}
	
	private void parseMIDI(BufferReference data) throws UnsupportedFileTypeException {
		int tNum = parseHeader(data);
		if (tNum <= 0) throw new UnsupportedFileTypeException("MIDI.parseMIDI || At least 1 track required!");
		
		try {
			this.contents = new Sequence(getDivType(), getDivRes());
		} 
		catch (InvalidMidiDataException e) {
			e.printStackTrace();
			throw new UnsupportedFileTypeException("MIDI.parseMIDI || Could not initialize sequence with provided header parameters.");
		}
		
		for (int i = 0; i < tNum; i++){
			Track t = contents.createTrack();
			this.parseTrack(data, t);
		}
	}
	
	/*--- Serialization ---*/
	
	private void clearClutter(){
		for (String p : this.diskClutter){
			try {
				Files.delete(Paths.get(p));
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		//this.tempNum = 0;
		this.diskClutter.clear();
	}
	
	public long calculateTrackSize(Track t){
		int events = t.size();
		long tSize = 0;
		tSize += 8;
		long lastTime = 0;
		for (int i = 0; i < events; i++)
		{
			//ACCOUNT FOR DELTA TIME
			int delta = (int)(t.get(i).getTick() - lastTime);
			lastTime = t.get(i).getTick();
			tSize += VLQlength(delta);
			tSize += (long)t.get(i).getMessage().getLength();
		}
		return tSize;
	}
	
	public long calculateSize(){
		long tot = 0;
		if (this.MIDI_type == 1) tot += 14;
		//System.out.println("calculateSize | Header added... tot = " + tot);
		Track[] tList = this.contents.getTracks();
		for (Track t : tList)
		{
			long tSz = calculateTrackSize(t);
			tot += tSz;
			//System.out.println("calculateSize | Adding tSz = " + tSz + ", tot = " + tot);
		}
		return tot;
	}
	
	private short generateDivision(){
		short d = 0;
		if (this.divMode){
			int id = 0;
			id = BitStreamer.writeABit(id, true, 15);
			id = id | ((this.FramesPerSec << 8) & 0x7F00);
			id = id | (this.TicksPerFrame & 0xFF);
			d = (short)id;
		}
		else{
			d = (short)(this.TicksPerQNote & 0x7FFF);
		}
		return d;
	}
	
	public static int VLQlength(int myNumber){
		if (myNumber <= 0x7F) return 1;
		if (myNumber <= 0x3FFF) return 2;
		if (myNumber <= 0x1FFFFF) return 3;
		if (myNumber <= 0x0FFFFFFF) return 4;
		return 0;
	}
	
	public static byte[] makeVLQ(int myNumber){
		if (myNumber == 0){
			byte[] vlq = new byte[1];
			vlq[0] = 0;
			return vlq;
		}
		
		int nBytes = VLQlength(myNumber);
		
		byte[] vlq = new byte[nBytes];

		for (int i = 0; i < nBytes; i++){
			int proto = (myNumber >> (7 * (nBytes - (i + 1)))) & 0x7F;
			if (i != nBytes - 1) proto = BitStreamer.writeABit(proto, true, 7);
			vlq[i] = (byte)proto;
		}
		
		return vlq;
	}

	private FileBuffer serializeHeader(){
		if (this.MIDI_type == 1){
			//System.out.println("serializeHeader | Size predicted: " + 14);
			FileBuffer header = new FileBuffer(14, true);
			header.printASCIIToFile(HEADER_MAG);
			header.addToFile(6);
			header.addToFile((short)0x01);
			short tCount = (short)this.contents.getTracks().length;
			header.addToFile(tCount);
			header.addToFile(this.generateDivision());
			//System.out.println("serializeHeader | Actual Size: " + header.getFileSize());
			return header;
		}
		return null;
	}
	
	private FileBuffer serializeRawTrack(Track t) {
		int ecount = t.size();

		//Do a track scan to estimate size
		int alloc = 0;
		for(int i = 0; i < ecount; i++){
			MidiEvent e = t.get(i);
			alloc += 4;
			
			MidiMessage msg = e.getMessage();
			alloc += msg.getLength();
		}
		
		FileBuffer out = new FileBuffer(alloc, true);
		long lasttime = 0;
		int stat = -1;
		for(int i = 0; i < ecount; i++){
			MidiEvent e = t.get(i);
			long delta = e.getTick() - lasttime;
			lasttime = e.getTick();
			byte[] d_vlq = makeVLQ((int)delta);
			for(int j = 0; j < d_vlq.length; j++) out.addToFile(d_vlq[j]);
			
			//Message
			MidiMessage msg = e.getMessage();
			int mstat = msg.getStatus();
			byte[] msgbytes = msg.getMessage();
			if(((mstat >>> 4) & 0xF) != 0xF){
				//Can use for running status
				if(mstat != stat){
					stat = mstat;
					//Write full message
					for(int j = 0; j < msgbytes.length; j++) out.addToFile(msgbytes[j]);
				}
				else{
					//Only write data
					for(int j = 1; j < msgbytes.length; j++) out.addToFile(msgbytes[j]);
				}

			}
			else{
				stat = -1;
				//Write full message
				for(int j = 0; j < msgbytes.length; j++) out.addToFile(msgbytes[j]);
			}
		}
		return out;
	}
	
	private boolean serializeTrack(Track t, OutputStream out) throws IOException{
		FileBuffer tdat = serializeRawTrack(t);
		int tsz = (int)tdat.getFileSize();

		FileBuffer head = new FileBuffer(8, true);
		head.printASCIIToFile(TRACK_MAG);
		head.addToFile(tsz);

		head.writeToStream(out);
		tdat.writeToStream(out);
		return true;
	}
	
	public boolean serializeTo(OutputStream out) throws IOException{
		Track[] tList = contents.getTracks();
		boolean good = true;
		serializeHeader().writeToStream(out);
		for (Track t : tList) good = good && serializeTrack(t, out);
		return good;
	}

	public void writeMIDI(String path) throws IOException{
		//System.out.println("writeMIDI | called... path = " + path); 
		if (this.tempDir == null || !FileBuffer.directoryExists(this.tempDir)){
			this.tempDir = FileBuffer.chopPathToDir(path);
		}
		//System.out.println("writeMIDI | tempDir = " + tempDir); 
		try {
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path));
			serializeTo(bos);
			bos.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			this.clearClutter();
		}
	}
	
	/*--- Conversion ---*/
	
	public static int bpm2uspqn(int bpm, double beatsPerQuarterNote){
		double n = 60000000.0/(double)bpm; 
		//n is now us per beat
		//n /= beatsPerQuarterNote;
		return (int)Math.round(n);
	}
	
	public static int uspqn2bpm(int uspqn, double beatsPerQuarterNote){
		//double n = uspqn * beatsPerQuarterNote;
		//n = n/60000000.0;
		//n = 1.0/n;
		double n = 60000000.0/(double)uspqn; 
		return (int)Math.round(n);
	}
	
	public boolean writeInfo(BufferedWriter bw){
		try{
			bw.write("MIDI File Structure ===============\n");
			bw.write("Midi Type: " + this.MIDI_type + "\n");
			bw.write("Division Type: ");
			if (this.divMode) bw.write("SMPTE\n");
			else bw.write("Ticks per Beat\n");
			if (this.divMode){
				bw.write("\tFrames Per Second: " + (this.FramesPerSec * -1) + "\n"); 
				bw.write("\tTicks Per Frame: " + (this.TicksPerFrame) + "\n"); 
			}
			else{
				bw.write("\tTicks Per Quarter Note: " + (this.TicksPerQNote) + "\n"); 
			}
			bw.write("Internal Name: " + this.internalName + "\n");
			if (this.contents != null){
				int nTr = this.contents.getTracks().length;
				bw.write("Number of Tracks: " + nTr + "\n");
				bw.write("\n");
				for (int i = 0; i < nTr; i++){
					bw.write("\t ----- Track " + (i + 1) + "\n");
					Track t = this.contents.getTracks()[i];
					int nEv = t.size();
					for (int j = 0; j < nEv; j++){
						MidiEvent e = t.get(j);
						bw.write("\t\t");
						bw.write("[" + e.getTick() + "] ");
						int stat = e.getMessage().getStatus();
						if (stat < 0x10) bw.write("0");
						bw.write(Integer.toHexString(stat) + " | ");
						int mlen = e.getMessage().getLength();
						for (int k = 1; k < mlen; k++){
							int d = Byte.toUnsignedInt(e.getMessage().getMessage()[k]);
							if (d < 0x10) bw.write("0");
							bw.write(Integer.toHexString(d) + " ");
						}
						bw.write("\n");
					}
					bw.write("\n");
				}
			}
			else{
				bw.write("MIDI object contains no sequence.\n");
			}
		}
		catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public String toString(){
		String s = "";
		s += "MIDI File Structure ===============\n";
		s += "Midi Type: " + this.MIDI_type + "\n";
		s += "Division Type: ";
		if (this.divMode) s += "SMPTE\n";
		else s += "Ticks per Beat\n";
		if (this.divMode){
			s += "\tFrames Per Second: " + (this.FramesPerSec * -1) + "\n"; 
			s += "\tTicks Per Frame: " + (this.TicksPerFrame) + "\n"; 
		}
		else{
			s += "\tTicks Per Quarter Note: " + (this.TicksPerQNote) + "\n"; 
		}
		s += "Internal Name: " + this.internalName + "\n";
		if (this.contents != null){
			int nTr = this.contents.getTracks().length;
			s += "Number of Tracks: " + nTr + "\n";
			s += "\n";
			for (int i = 0; i < nTr; i++){
				s += "\t ----- Track " + (i + 1) + "\n";
				Track t = this.contents.getTracks()[i];
				int nEv = t.size();
				for (int j = 0; j < nEv; j++){
					MidiEvent e = t.get(j);
					s += "\t\t";
					s += "[" + e.getTick() + "] ";
					int stat = e.getMessage().getStatus();
					if (stat < 0x10) s += "0";
					s += Integer.toHexString(stat) + " | ";
					int mlen = e.getMessage().getLength();
					for (int k = 1; k < mlen; k++){
						int d = Byte.toUnsignedInt(e.getMessage().getMessage()[k]);
						if (d < 0x10) s += "0";
						s += Integer.toHexString(d) + " ";
					}
					s += "\n";
				}
				s += "\n";
			}
		}
		else{
			s += "MIDI object contains no sequence.\n";
		}
		return s;
	}
	
	public static String getNoteName(int midiNote){
		String note = NOTES[Math.abs(midiNote%12)];
		int octave = (midiNote/12) - 1;
		return note + octave;
	}
	
}
