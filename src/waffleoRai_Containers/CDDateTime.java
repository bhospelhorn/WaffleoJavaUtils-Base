package waffleoRai_Containers;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import waffleoRai_Utils.BufferReference;
import waffleoRai_Utils.FileBuffer;

public class CDDateTime {
	
	private int year = 2000;
	private int month = 1;
	private int day = 1;
	private int hour = 0;
	private int minute = 0;
	private int second = 0;
	private int frame = 0;
	private int timezone = 0; //Hour offset from GMT??
	
	public static CDDateTime now(){
		ZonedDateTime time = ZonedDateTime.now();
		CDDateTime cdtime = new CDDateTime();
		cdtime.year = time.getYear();
		cdtime.month = time.getMonthValue();
		cdtime.day = time.getDayOfMonth();
		cdtime.hour = time.getHour();
		cdtime.minute = time.getMinute();
		cdtime.second = time.getSecond();
	
		int nano = time.getNano();
		cdtime.frame = (int)Math.round(((double)nano/1000000000.0) * 75.0);
		cdtime.timezone = time.getOffset().getTotalSeconds() / 3600;
		
		return cdtime;
	}
	
	public static CDDateTime readFromVolumeDescriptor(BufferReference in){
		try{
			CDDateTime datetime = new CDDateTime();
			datetime.year = Integer.parseInt(in.nextASCIIString(4));
			datetime.month = Integer.parseInt(in.nextASCIIString(2));
			datetime.day = Integer.parseInt(in.nextASCIIString(2));
			datetime.hour = Integer.parseInt(in.nextASCIIString(2));
			datetime.minute = Integer.parseInt(in.nextASCIIString(2));
			datetime.second = Integer.parseInt(in.nextASCIIString(2));
			datetime.frame = Integer.parseInt(in.nextASCIIString(2));
			datetime.timezone = (int)in.nextByte();
			
			return datetime;
		}
		catch(NumberFormatException ex){
			ex.printStackTrace();
		}
		return null;
	}
	
	public static CDDateTime readFromFileEntry(BufferReference in){
		CDDateTime datetime = new CDDateTime();
		datetime.year = 1900 + Byte.toUnsignedInt(in.nextByte());
		datetime.month = Byte.toUnsignedInt(in.nextByte());
		datetime.day = Byte.toUnsignedInt(in.nextByte());
		datetime.hour = Byte.toUnsignedInt(in.nextByte());
		datetime.minute = Byte.toUnsignedInt(in.nextByte());
		datetime.second = Byte.toUnsignedInt(in.nextByte());
		datetime.frame = 0;
		datetime.timezone = (int)in.nextByte();
		
		return datetime;
	}
	
	public FileBuffer serializeForCD(){
		FileBuffer serdat = new FileBuffer(17, true);
		serdat.printASCIIToFile(String.format("%04d", year));
		serdat.printASCIIToFile(String.format("%02d", month));
		serdat.printASCIIToFile(String.format("%02d", day));
		serdat.printASCIIToFile(String.format("%02d", hour));
		serdat.printASCIIToFile(String.format("%02d", minute));
		serdat.printASCIIToFile(String.format("%02d", second));
		serdat.printASCIIToFile(String.format("%02d", frame));
		serdat.addToFile((byte)timezone);
		return serdat;
	}
	
	public int getYear(){return year;}
	public int getMonth(){return month;}
	public int getDay(){return day;}
	public int getHour(){return hour;}
	public int getMinute(){return minute;}
	public int getSecond(){return second;}
	public int getFrame(){return frame;}
	public int getTimezone(){return timezone;}
	
	public void setYear(int val){year = val;}
	public void setMonth(int val){month = val;}
	public void setDay(int val){day = val;}
	public void setHour(int val){hour = val;}
	public void setMinute(int val){minute = val;}
	public void setSecond(int val){second = val;}
	public void setFrame(int val){frame = val;}
	public void setTimezone(int val){timezone = val;}
	
	public ZonedDateTime toZonedDateTime(){
		int nano = (int)(Math.round(((double)frame/75.0) * 1000000000.0));
		ZoneId zone = ZoneId.ofOffset("GMT", ZoneOffset.ofHours(timezone));
		return ZonedDateTime.of(year, month, day, 
				hour, minute, second, nano, zone);
	}
	
	public String toString(){
		ZonedDateTime zdt = toZonedDateTime();
		return zdt.format(DateTimeFormatter.ISO_DATE);
	}

}
