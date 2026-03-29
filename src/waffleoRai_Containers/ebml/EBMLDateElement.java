package waffleoRai_Containers.ebml;

import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import waffleoRai_Utils.FileBuffer;

public class EBMLDateElement extends EBMLElement{
	
	private static final long SEC_PER_HR = 3600L;
	
	private static final long SEC_PER_YEAR = (SEC_PER_HR * 24L) * 365L;
	private static final long SEC_PER_LEAP_YEAR = (SEC_PER_HR * 24L) * 366L;
	
	private static final int TOTAL_YEAR_OFFSET = 31;
	private static final int LEAP_YEAR_COUNT = 8;
	private static final int NON_LEAP_YEAR_COUNT = TOTAL_YEAR_OFFSET - LEAP_YEAR_COUNT;
	
	private static final long EPOCH_OFFSET = (LEAP_YEAR_COUNT * SEC_PER_LEAP_YEAR) + (NON_LEAP_YEAR_COUNT * SEC_PER_YEAR);
	
	private static final String UTC_TZ = "UTC";
	
	private ZonedDateTime dValue;
	
	public EBMLDateElement(int elementId, long rawValue) {
		super.UID = elementId;
		dValue = EBMLDateElement.readEbmlTime(rawValue);
	}
	
	public EBMLDateElement(int elementId, ZonedDateTime value) {
		super.UID = elementId;
		dValue = value;
		if(value == null) setToNow();
	}
	
	public ZonedDateTime getValue() {return dValue;}
	public void setValue(ZonedDateTime val) {dValue = val;}
	
	public void setToNow() {
		dValue = ZonedDateTime.now();
	}
	
	public long getDataSize() {return 8L;}
	public int getSerializedSizeFieldSize() {return 1;}
	public long getSerializedTotalSize() {return 9 + EBMLCommon.calcVLQLength(UID);}
	
	public long serializeTo(FileBuffer target) {
		if(target == null) return 0L;
		int idlen = super.serializeUIDTo(target);
		target.addToFile((byte)0x88);
		target.addToFile(writeEbmlTime(dValue));
		return idlen + 1 + 8;
	}
	
	public static ZonedDateTime readEbmlTime(long value) {
		long div = 1000000000L;
		long nanoOffset = value % div;
		value /= div;
		
		//Add 31 years (So can bring to 1970 epoch second)
		value += EPOCH_OFFSET;
		
		ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochSecond(value, nanoOffset), ZoneId.of(UTC_TZ));
		return zdt;
	}
	
	public static long writeEbmlTime(ZonedDateTime timestamp) {
		if(timestamp == null) return 0L;
		//If not UTC, convert to UTC
		if(timestamp.getOffset().getTotalSeconds() != 0) {
			timestamp = ZonedDateTime.ofInstant(timestamp.toInstant(), ZoneId.of(UTC_TZ));
		}
		
		long nano = timestamp.getNano();
		long val = timestamp.toEpochSecond();
		
		val -= EPOCH_OFFSET;
		val *= 1000000000L;
		val += nano;
		
		return val;
	}
	
	public void writeXMLNode(Writer output, EBMLFieldDef def, EBML_XMLWriterState state) throws IOException{
		//TODO Add custom format eventually
		if(output == null) return;
		if(state == null) return;
		String elId = null;
		if(def != null) {
			elId = def.stringId;
		}
		else elId = String.format("EBML%X", UID);
		
		output.write(state.indent + "<" + elId);
		output.write(String.format(" Date=\"%d/%02d/%02d\"", dValue.getYear(), dValue.getMonthValue(), dValue.getDayOfMonth()));
		output.write(String.format(" Time=\"%02d:%02d:%02d.%d\"", dValue.getHour(), dValue.getMinute(), dValue.getSecond(), dValue.getNano()));
		output.write(String.format(" Timezone=\"%s\"", dValue.getZone().getId()));
		output.write("/>\n");
	}

}
