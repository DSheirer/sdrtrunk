package decode.p25.message;

public interface IdentifierProvider
{

	/**
	 * (Band) Identifier
	 */
	public abstract int getIdentifier();

	/**
	 * Channel spacing in hertz
	 */
	public abstract long getChannelSpacing();

	/**
	 * Base frequency in hertz
	 */
	public abstract long getBaseFrequency();


	/**
	 * Channel bandwidth in hertz
	 */
    public abstract int getBandwidth();

    /**
     * Transmit offset in hertz
     */
    public abstract long getTransmitOffset();

}