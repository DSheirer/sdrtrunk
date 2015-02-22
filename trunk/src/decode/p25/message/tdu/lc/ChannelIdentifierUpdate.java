package decode.p25.message.tdu.lc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decode.p25.message.IBandIdentifier;
import decode.p25.reference.LinkControlOpcode;

public class ChannelIdentifierUpdate extends TDULinkControlMessage
									 implements IBandIdentifier
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( ChannelIdentifierUpdate.class );

	public static final int[] IDENTIFIER = { 72,73,74,75 };
	public static final int[] BANDWIDTH = { 88,89,90,91,92,93,94,95,96 };
	public static final int[] TRANSMIT_OFFSET = { 97,98,99,112,113,114,115,116,
		117 };
	public static final int[] CHANNEL_SPACING = { 118,119,120,121,122,123,136,
		137,138,139 };
	public static final int[] BASE_FREQUENCY = { 140,141,142,143,144,145,146,
		147,160,161,162,163,164,165,166,167,168,169,170,171,184,185,186,
		187,188,189,190,191,192,193,194,195 };
	
	public ChannelIdentifierUpdate( TDULinkControlMessage source )
	{
		super( source );
	}
	
    @Override
    public String getEventType()
    {
        return LinkControlOpcode.CHANNEL_IDENTIFIER_UPDATE.getDescription();
    }

	@Override
	public String getMessage()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( getMessageStub() );
		
        sb.append( " IDEN:" + getIdentifier() );

        sb.append( " BASE:" + getBaseFrequency() );
        
        sb.append( " BW:" + getBandwidth() );
        
        sb.append( " SPACING:" + getChannelSpacing() );
        
        sb.append( " OFFSET:" + getTransmitOffset() );
        
		return sb.toString();
	}

	@Override
	public int getIdentifier()
	{
		return mMessage.getInt( IDENTIFIER );
	}

	/**
     * Channel bandwidth in hertz
     */
    @Override
    public int getBandwidth()
    {
    	return mMessage.getInt( BANDWIDTH ) * 125;
    }
	
    @Override
    public long getChannelSpacing()
    {
    	return mMessage.getLong( CHANNEL_SPACING ) * 125l;
    }

    @Override
    public long getBaseFrequency()
    {
        return mMessage.getLong( BASE_FREQUENCY ) * 5l;
    }

	@Override
    public long getTransmitOffset()
    {
		return  -1 * mMessage.getLong( TRANSMIT_OFFSET ) * 250000l;
    }
}
