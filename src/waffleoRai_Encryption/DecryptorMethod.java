package waffleoRai_Encryption;

public interface DecryptorMethod {
	
	public byte[] decrypt(byte[] input, long offval);
	public void adjustOffsetBy(long value);
	
	public int getInputBlockSize();
	public int getOutputBlockSize();
	public int getPreferredBufferSizeBlocks();
	
	/**
	 * Given a position relative to the start of an input block,
	 * get the equivalent position relative to the start of the output
	 * block.
	 * @param inputBlockOffset Input offset relative to start of input block.
	 * @return Output offset relative to start of output block.
	 */
	public long getOutputBlockOffset(long inputBlockOffset);

}
