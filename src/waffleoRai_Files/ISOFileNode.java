package waffleoRai_Files;

import java.io.IOException;

import waffleoRai_Utils.DirectoryNode;
import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.FileNode;
import waffleoRai_Utils.MultiFileBuffer;

/*
 * UPDATES
 * 
 * 2020.06.15 | 1.0.0
 * 		Initial Documentation
 * 2020.06.17 | 1.1.0
 * 		Size should be in bytes, not sectors. (Might not fill last sector).
 * 2020.06.21 | 1.2.0
 * 		Added data loaders that only load file part
 */

/**
 * A FileNode subclass for referencing files in a CD image (eg. ISO file) that
 * references offsets by sector (noting sector type) instead of byte offset.
 * This way, sector checksum data can be included or excluded as needed.
 * @author Blythe Hospelhorn
 * @version 1.2.0
 * @since June 21, 2020
 */
public class ISOFileNode extends FileNode{
	
	/* --- Instance Variables --- */
	
	//Offset and size of superclass are used, but are measured instead in sectors
	private int sector_head_size; //Size in bytes before sector data
	private int sector_data_size;
	private int sector_foot_size; //Size in bytes after sector data
	
	/* --- Construction --- */

	/**
	 * Construct a new ISOFileNode with the provided parent and file name.
	 * @param parent Parent directory. May use null if there is none.
	 * @param name Filename to initialize node with.
	 * @since 1.0.0
	 */
	public ISOFileNode(DirectoryNode parent, String name) {
		super(parent, name);
		//Default ISO values for head and foot...
		sector_head_size = 0x10;
		sector_data_size = 0x800;
		sector_foot_size = 0x120;
	}
	
	/* --- Getters --- */
	
	/**
	 * Get the total size of a sector on the CD image this node
	 * references. This includes the data and any EC headers or footers.
	 * @return Total size in bytes of a sector on referenced CD image.
	 * @since 1.0.0
	 */
	public int getSectorTotalSize(){
		return sector_data_size + sector_head_size + sector_foot_size;
	}
	
	/**
	 * Get the size in bytes of the data section on a sector of the
	 * CD image this node is referencing. This excludes any EC header
	 * or footer data.
	 * @return Size in bytes of data in a single sector.
	 * @since 1.0.0
	 */
	public int getSectorDataSize(){return sector_data_size;}
	
	/**
	 * Get the size in bytes of the header on a sector of the
	 * CD image this node is referencing.
	 * @return Size in bytes of header of a single sector.
	 * @since 1.0.0
	 */
	public int getSectorHeadSize(){return sector_head_size;}
	
	/**
	 * Get the size in bytes of the footer on a sector of the
	 * CD image this node is referencing.
	 * @return Size in bytes of footer of a single sector.
	 * @since 1.0.0
	 */
	public int getSectorFootSize(){return sector_foot_size;}
	
	/**
	 * Get the number of sectors required to hold this file.
	 * @return File size in CD sectors.
	 * @since 1.1.0
	 */
	public int getLengthInSectors(){
		int raw_bytelen = (int)super.getLength(); //Size in bytes
		int sec_size = this.getSectorDataSize();
		int sec_len = raw_bytelen/sec_size;
		if(raw_bytelen % sec_size != 0) sec_len++;
		
		return sec_len;
	}
	
	/* --- Setters --- */
	
	/**
	 * Set the sector header, footer, and data sizes to match
	 * the specifications for CD-XA Mode 2, Form 1. (Used in PSX discs)
	 */
	public void setMode2Form1(){
		sector_head_size = 0x18;
		sector_data_size = 0x800;
		sector_foot_size = 0x118;
	}
	
	/**
	 * Set the sector header, footer, and data sizes to match
	 * the specifications for CD-XA Mode 2, Form 2. (Used in PSX discs)
	 */
	public void setMode2Form2(){
		sector_head_size = 0x18;
		sector_data_size = 0x914;
		sector_foot_size = 0x4;
	}

	/**
	 * Set the sector header, footer, and data sizes to match the 
	 * specifications of an audio only sector on a standard CD image.
	 * @since 1.0.0
	 */
	public void setAudioMode(){
		sector_head_size = 0;
		sector_data_size = 0x930;
		sector_foot_size = 0;
	}
	
	/**
	 * Directly set the sector header, footer, and data sizes.
	 * @param head Header size in bytes.
	 * @param dat Data size in bytes.
	 * @param foot Footer size in bytes.
	 * @since 1.0.0
	 */
	public void setSectorDataSizes(int head, int dat, int foot){
		this.sector_head_size = head;
		this.sector_data_size = dat;
		this.sector_foot_size = foot;
	}
	
	/* --- Data Handling --- */
	
	protected void copyDataTo(FileNode copy){
		super.copyDataTo(copy);
		if(copy instanceof ISOFileNode){
			ISOFileNode other = (ISOFileNode)copy;
			other.sector_data_size = this.sector_data_size;
			other.sector_foot_size = this.sector_foot_size;
			other.sector_head_size = this.sector_head_size;
		}
	}
	
	public FileBuffer loadData(long stpos, long len) throws IOException{
		
		//Recalculate to sector coordinates
		int sec_size = this.getSectorTotalSize();
		int sec_dat_size = this.getSectorDataSize();
		long stsec = stpos/sec_dat_size; //Start sector index
		long sec_stoff = stpos%sec_dat_size; // Position relative to sector data start
		
		long edoff = stpos + len;
		long edsec = edoff/sec_dat_size; //Index of sector that contains end position
		long sec_edoff = edoff%sec_dat_size; // Position relative to sector data start
		
		long seccount = edsec - stsec;
		if(sec_edoff != 0) seccount++;
		
		FileBuffer rawsecs = loadRawData(stsec, seccount);
		MultiFileBuffer dat = new MultiFileBuffer((int)seccount);
		long cpos = 0;
		for(int s = 0; s < seccount; s++){
			long st = 0;
			long ed = sec_dat_size;
			if(s == 0) st = sec_stoff;
			if(s == (seccount - 1)) ed= sec_edoff;
			if(ed == 0) break;
			
			st += this.getSectorHeadSize(); ed += this.getSectorHeadSize();
			dat.addToFile(rawsecs, cpos + st, cpos + ed);
			cpos += sec_size;
		}
		
		return dat;
	}
	
	public FileBuffer loadData() throws IOException{
		//This strips any header/footer data
		//System.err.println("ISOFileNode.loadData || Called!");
		FileBuffer raw = loadRawData();
		if(sector_head_size == 0 && sector_foot_size == 0) return raw;
		
		//Calculate data end
		int seclen = this.getLengthInSectors();
		long fulldat = (seclen-1) * this.getSectorDataSize();
		long bytelen = this.getLength();
		long partdat = bytelen - fulldat; //Number of data bytes in final sector
		
		//Strip
		int sec_size = this.getSectorTotalSize();
		long cpos = 0; long datend = sector_head_size + sector_data_size;
		MultiFileBuffer dat = new MultiFileBuffer(seclen);
		for(int s = 0; s < seclen-1; s++){
			long st = cpos + sector_head_size;
			long ed = cpos + datend;
			//System.err.println("Extracting sector " + s + ": 0x" + Long.toHexString(st) + " - 0x" + Long.toHexString(ed));
			dat.addToFile(raw, st, ed);
			cpos += sec_size;
		}
		
		long st = cpos + sector_head_size;
		long ed = st + partdat;
		//System.err.println("Extracting sector " + (seclen-1) + " (final): 0x" + Long.toHexString(st) + " - 0x" + Long.toHexString(ed));
		dat.addToFile(raw, st, ed);
		cpos += sec_size;
		//System.err.println("ISOFileNode.loadData || Size of buffer returned: 0x" + Long.toHexString(dat.getFileSize()));
		return dat;
	}
	
	 /** Load ALL data in the specified sectors, including sector header/footer data, referenced by this node
	 * into a single FileBuffer.
	 * @param sec_off Offset in sectors relative to the start of this file to begin reading.
	 * @param sec_len Number of sectors to load. If this added to the offset sector exceeds the length
	 * of the file, this method will return only what is in this file.
	 * @return FileBuffer containing raw data referenced by file - all full sectors sequential.
	 * @throws IOException If the data cannot be loaded from disk.
	 * @since 1.2.0
	 */
	public FileBuffer loadRawData(long sec_off, long sec_len) throws IOException{
		if(sec_len <= 0) return null;
		String path = getSourcePath();
		
		//Chain together desired sectors.
		int sec_size = this.getSectorTotalSize();
		long maxlen = this.getLengthInSectors() - sec_off;
		if(sec_len > maxlen) sec_len = maxlen;
		
		long stpos = sec_size * (this.getOffset() + sec_off);
		long edpos = stpos + (sec_size*sec_len);
		
		return FileBuffer.createBuffer(path, stpos, edpos);
	}
	
	/**
	 * Load ALL data, including sector header/footer data, referenced by this node
	 * into a single FileBuffer.
	 * @return FileBuffer containing raw data referenced by file - all full sectors sequential.
	 * @throws IOException If the data cannot be loaded from disk.
	 * @since 1.0.0
	 */
	public FileBuffer loadRawData() throws IOException{
		String path = getSourcePath();
		//FileBuffer imgdat = FileBuffer.createBuffer(path);
		//ISO iso = new ISO(imgdat, true);
		
		//Chain together desired sectors.
		int sec_size = this.getSectorTotalSize();
		int sec_len = getLengthInSectors();
		
		long stpos = sec_size * this.getOffset();
		long edpos = stpos + (sec_size*sec_len);
		
		//System.err.println("Loading... 0x" + Long.toHexString(stpos) + " - 0x" + Long.toHexString(edpos));
		
		return FileBuffer.createBuffer(path, stpos, edpos);
	}
	
	/* --- View --- */
	
	public String getLocationString(){
		int endsec = (int)getOffset() + this.getLengthInSectors();
		return "sec " + this.getOffset() + " - " + endsec;
	}
	
	/* --- Debug --- */
	
	public void printMeToStdErr(int indents)
	{
		StringBuilder sb = new StringBuilder(128);
		for(int i = 0; i < indents; i++) sb.append("\t");
		String tabs = sb.toString();
		
		long bytelen = this.getLength();
		long secsize = this.getSectorDataSize();
		long secct = bytelen/secsize;
		if(bytelen % secsize != 0) secct++;
		
		System.err.println(tabs + "->" + this.getFileName() + " (sec " + this.getOffset() + " - " + (this.getOffset() + secct - 1) + " -- 0x" + Long.toHexString(bytelen) + " bytes)");
	}
	
	
}
