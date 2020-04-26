package waffleoRai_Sound;

public interface RandomAccessSound extends Sound{

	public int getSample(int channel, int frame);
	
	/**
	 * Gets the requested frame as an array of bytes.
	 * Byte order for multi-byte samples is Little-Endian.
	 * Nybble order for 4-bit samples is defined by container.
	 * @param frame Frame index.
	 * @return Byte array representing frame data, or null if could not be retrieved.
	 */
	public byte[] frame2Bytes(int frame);
	
	//public void normalizeAmplitude();
	
}
