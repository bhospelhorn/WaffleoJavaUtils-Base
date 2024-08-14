package waffleoRai_Containers;

import java.io.IOException;
import java.util.Collection;

import waffleoRai_Containers.CDTable.CDInvalidRecordException;
import waffleoRai_Containers.XATable.XAEntry;
import waffleoRai_Containers.XATable.XASectorHeader;
import waffleoRai_Files.tree.ISOFileNode;
import waffleoRai_Utils.FileBuffer;
//import waffleoRai_jpsx.PSXCDTable;
import waffleoRai_Utils.FileBuffer.UnsupportedFileTypeException;
import waffleoRai_Utils.MultiFileBuffer;

/*
 * UPDATES
 * 
 * 2017.11.02 | 1.1.0 -> 1.2.0
 * 	Fixed parsing issues
 * 2017.11.05 | 1.2.0 -> 1.2.1
 * 	Added empty constructor for child classes
 * 2017.11.05 | 1.2.1 -> 1.3.0
 * 	Added further accessibility, especially for child classes
 * 2020.06.16 | 1.3.0 -> 2.0.0
 * 	Updated to use DirectoryNode instead of VirDirectory
 * 2024.07.22 | 2.0.0 -> 2.1.0
 * 	Debug issues with file nodes
 * 	Made loading raw sectors less clunky
 * 2024.07.23 | 2.1.0 -> 2.1.1
 * 	Switched raw mode off in string path constructor
 */

/**
 * Child class of ISO9660 image extended to include information from the eXtended Architecture
 * specification. Includes specialized handling of Mode 2 sectors.
 * @author Blythe Hospelhorn
 * @version 2.1.1
 * @since July 23, 2024
 */
public class ISOXAImage extends ISO9660Image {
	
	public static final String METAKEY_VOLUMEIDENT = "ISO_VOL_IDENT";

	private XATable table;
	
	/* --- Construction --- */
	
	/**
	 * Construct a fully parsed ISO-XA image from a sector-parsed ISO image. Save path to input file for easier
	 * data loading later.
	 * @param path Path to ISO image file on local file system.
	 * @throws CDInvalidRecordException If there is an error reading a record in a directory table.
	 * @throws IOException If there is an error creating streaming buffers.
	 * @throws UnsupportedFileTypeException If there is an error parsing primary volume descriptor.
	 * @since 2.1.0
	 */
	public ISOXAImage(String path) throws CDInvalidRecordException, IOException, UnsupportedFileTypeException {
		super();
		super.sourceFile = path;
		//Read ISO
		FileBuffer buff = FileBuffer.createBuffer(path, true);
		ISO iso = new ISO(buff, false);
		this.table = new XATable(iso);
		super.readInformation(iso);
		generateRootDirectory(iso);
	}
	
	/**
	 * Construct a fully parsed ISO-XA image from a sector-parsed ISO image.
	 * @param myISO Image to parse and extract files from.
	 * @throws CDInvalidRecordException If there is an error reading a record in a directory table.
	 * @throws IOException If there is an error creating streaming buffers.
	 * @throws UnsupportedFileTypeException If there is an error parsing primary volume descriptor.
	 */
	public ISOXAImage(ISO myISO) throws CDInvalidRecordException, IOException, UnsupportedFileTypeException {
		super();
		this.table = new XATable(myISO);
		//table.printMe();
		super.readInformation(myISO);
		//generateRootDirectory(myISO, this.table);
		generateRootDirectory(myISO);
	}
	
	/**
	 * Construct an empty ISO-XA image with instantiated root directory, but a null XA table.
	 * <br> Table must be instantiated if wish to use.
	 */
	protected ISOXAImage()
	{
		super();
		table = new XATable();
	}
	
	protected void generateRootDirectory(ISO myISO) throws IOException {
		Collection<XAEntry> c = table.getXAEntries();
		//if (eventContainer != null) eventContainer.fireNewEvent(EventType.IMG9660_TBLLISTED, c.size());
		String vident = getVolumeIdent().replace(" ", "");
		root.setMetadataValue(METAKEY_VOLUMEIDENT, vident);
		String raw_dir = "cdraw";
		for (XAEntry e : c) {
			//if (!e.isDirectory() && e.getStartBlock() < myISO.getNumberSectorsRelative()) 
			if (!e.isDirectory()) {
				//Uses full paths in name
				ISOFileNode node = new ISOFileNode(null, "");
				node.setOffset(e.getStartBlock());
				//node.setLength(e.getSizeInSectors());
				if(e.isMode2()){
					if(e.isForm2()) node.setMode2Form2();
					else node.setMode2Form1();
				}
				if(e.isCDDA()) node.setAudioMode();
				node.setLength(e.getFileSize()); //Setting the mode snaps file size to sectors. We want the ACTUAL file size.
				//System.err.println("Found entry: " + e.getName());
				//System.err.println("Entry file size: 0x" + Long.toHexString(e.getFileSize()));
				String addpath = null;
				if(e.isRawFile()) addpath = raw_dir + "\\" + e.getName();
				else addpath = vident + "\\" + e.getName();
				node.setSourcePath(super.sourceFile);
				root.addChildAt(addpath, node);
			}
		}
	}
	
	/* --- Getters --- */
	
	public ISO9660Table getTable()
	{
		return this.table;
	}
	
	public FileBuffer getSectorData(int sector) throws IOException {
		if((sourceFile != null) && FileBuffer.fileExists(sourceFile)) {
			FileBuffer fullsec = getRawSector(sector);
			if(fullsec != null) {
				//Determine mode.
				int mode = Byte.toUnsignedInt(fullsec.getByte(0xf));
				if(mode == 1) {
					return fullsec.createReadOnlyCopy(0x10, ISO.F1SIZE + 0x10);
				}
				else if(mode == 2) {
					//Determine form
					int sh3 = Byte.toUnsignedInt(fullsec.getByte(0x13));
					if((sh3 & 0x20) != 0) {
						//Form 2
						return fullsec.createReadOnlyCopy(0x18, ISO.F2SIZE + 0x18);
					}
					else {
						//Form 1
						return fullsec.createReadOnlyCopy(0x18, ISO.F1SIZE + 0x18);
					}
				}
				else if(mode == 0) {
					return fullsec.createReadOnlyCopy(0x10, ISO.SECSIZE);
				}
			}
		}
		
		XAEntry e = (XAEntry)(this.getTable().getEntry(sector));
		if (e == null) return null;
		long sOff = this.getTable().calcFileOffsetOfSector(e.getName(), sector);
		FileBuffer mySec = this.getFile(e.getName());
		if (mySec == null) return null;
		if (sOff > mySec.getFileSize() || sOff < 0) return null;
		mySec = mySec.createReadOnlyCopy(sOff, this.getTable().sectorDataSize(sector));
		
		return mySec;
	}
	
	public FileBuffer getRawSector(int relativeSector) throws IOException {
		if((sourceFile != null) && FileBuffer.fileExists(sourceFile)) {
			long st = (long)relativeSector * ISO.SECSIZE;
			long ed = st + ISO.SECSIZE;
			return FileBuffer.createBuffer(sourceFile, st, ed, true);
		}
		
		//Check if mode 1 first
		if (!table.isSectorMode2(relativeSector)) return super.getRawSector(relativeSector);
		//Handle mode 2 sectors
		boolean f2 = table.isSectorForm2(relativeSector);
		int absSec = relativeSector + table.getFirstSectorIndex();
		FileBuffer secHeader = new FileBuffer(0x18, true);
		//Main header
		for (int i = 0; i < ISO.SYNC.length; i++) secHeader.addToFile(ISO.SYNC[i]);
		byte minByte = ISO.getBCDminute(absSec);
		secHeader.addToFile(minByte);
		byte sndByte = ISO.getBCDsecond(absSec);
		secHeader.addToFile(sndByte);
		byte secByte = ISO.getBCDsector(absSec);
		secHeader.addToFile(secByte);
		byte modeByte = 0x02;
		secHeader.addToFile(modeByte);
		// Subheader
		XASectorHeader sech = table.getSectorHeader(relativeSector);
		if (sech != null)
		{
			int subHeader = sech.serialize();
			secHeader.addToFile(subHeader);
			secHeader.addToFile(subHeader);
		}
		else
		{
			int subHeader = 0x00000800;
			secHeader.addToFile(subHeader);
			secHeader.addToFile(subHeader);
		}
		
		//Tail
		FileBuffer secTail;
		int tailSize = 0;
		
		if (f2) tailSize = 0x04;
		else tailSize = 0x04 + 0x114;
		
		secTail = new FileBuffer(tailSize);
		for (int i = 0; i < tailSize; i++) secTail.addToFile(ISO.ZERO);
		
		//Composite
		//FileBuffer mySector = new CompositeBuffer(3);
		FileBuffer mySector = new MultiFileBuffer(3);
		mySector.addToFile(secHeader);
		mySector.addToFile(getSectorData(relativeSector));
		mySector.addToFile(secTail);
		return mySector;
	}
	
	/**
	 * Get the same internal table as in getTable(), but without casting to an
	 * XATable required.
	 * @return Internal table as an XATable.
	 */
	public XATable getXATable(){
		return this.table;
	}

	/* --- Setters --- */
	
	
	/* --- Conversion --- */
	
	/*public PSXCDTable getPSXTable()
	{
		return new PSXCDTable(this.getXATable());
	}*/
	
	/**
	 * Print information about image to stdout.
	 */
	public void printMe()
	{
		System.out.println("ISO9660 (XA) IMAGE ================== ");
		super.printMyBasicInfo();
		this.table.printMe();
		super.printMyDirectory();
	}

}
