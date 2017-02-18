package module.decode.p25.message.tsbk.osp.control;

import module.decode.p25.reference.DataUnitID;
import module.decode.p25.reference.Opcode;
import alias.AliasList;
import bits.BinaryMessage;

public class IdentifierUpdateTDMA extends IdentifierUpdate
{
    public static final int[] CHANNEL_TYPE = { 84,85,86,87 };
    public static final int TRANSMIT_OFFSET_SIGN = 88;
    public static final int[] TRANSMIT_OFFSET = { 89,90,91,92,93,94,95,96,97,98,
    	99,100,101 };
    
    public IdentifierUpdateTDMA( BinaryMessage message, 
                             DataUnitID duid,
                             AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }
    
    @Override
    public String getEventType()
    {
    	return Opcode.IDENTIFIER_UPDATE_TDMA.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );
        sb.append(toString());

        return sb.toString();
    }

	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		sb.append( " ID:" + getIdentifier() );
		sb.append( " BASE:" + getBaseFrequency() );
		sb.append( " BW:" + getBandwidth() );
		sb.append( " SPACING:" + getChannelSpacing() );
		sb.append( " OFFSET:" + getTransmitOffset() );
		sb.append( " TYPE:" + getChannelType().toString() );

		return sb.toString();
	}

	public ChannelType getChannelType()
    {
    	return ChannelType.fromValue( mMessage.getInt( CHANNEL_TYPE) );
    }
    
    /**
     * Channel bandwidth in hertz
     */
    public int getBandwidth()
    {
    	return getChannelType().getBandwidth();
    }

    /**
     * Transmit offset in hertz
     */
    @Override
    public long getTransmitOffset()
    {
        long offset = mMessage.getLong( TRANSMIT_OFFSET ) * 
        		getChannelSpacing();
        
        if( mMessage.get( TRANSMIT_OFFSET_SIGN ) )
        {
            return offset;
        }
        else
        {
            return -offset;
        }
    }

    @Override
    public long getDownlinkFrequency(int channelNumber)
    {
        if(isTDMA())
        {
            return getBaseFrequency() + ( (channelNumber / getChannelType().getSlotsPerCarrier()) * getChannelSpacing() );
        }
        else
        {
            return super.getDownlinkFrequency(channelNumber);
        }
    }

    @Override
    public boolean isTDMA()
    {
        return getChannelType().getAccessType() == AccessType.TDMA;
    }

    public enum ChannelType
    {
    	TYPE_0( AccessType.FDMA, 12500, 1, Vocoder.HALF_RATE ),
    	TYPE_1( AccessType.FDMA, 12500, 1, Vocoder.FULL_RATE ),
    	TYPE_2( AccessType.FDMA,  6250, 1, Vocoder.HALF_RATE ),
    	TYPE_3( AccessType.TDMA, 12500, 2, Vocoder.HALF_RATE ),
    	TYPE_4( AccessType.TDMA, 25000, 4, Vocoder.HALF_RATE ),
    	TYPE_5( AccessType.TDMA, 12500, 2, Vocoder.HALF_RATE ), //HD8PSK simulcast
    	UNKNOWN( AccessType.UNKNOWN, 0, 0, Vocoder.HALF_RATE );
    	
    	private AccessType mAccessType;
    	private int mBandwidth;
    	private int mSlotsPerCarrier;
    	private Vocoder mVocoder;

    	private ChannelType( AccessType accessType, 
    						 int bandwidth, 
    						 int slots,
    						 Vocoder vocoder )
    	{
    		mAccessType = accessType;
    		mBandwidth = bandwidth;
    		mSlotsPerCarrier = slots;
    		mVocoder = vocoder;
    	}
    	
    	public String toString()
    	{
    		StringBuilder sb = new StringBuilder();
    		
    		if( this.equals( TYPE_5 ) )
    		{
    			sb.append("H-D8PSK SIMULCAST " );
    		}
    		
    		sb.append( mAccessType.name() );
    		sb.append( " BANDWIDTH:" + mBandwidth );
    		sb.append( " SLOT/CARRIER:" + mSlotsPerCarrier );
    		sb.append( " VOCODER:" + mVocoder.name() );
    		
    		return sb.toString();
    	}
    	
    	public static ChannelType fromValue( int value )
    	{
    		if( 0 <= value && value <= 5 )
    		{
    			return ChannelType.values()[ value ];
    		}
    		
    		return ChannelType.UNKNOWN;
    	}
    	
    	public AccessType getAccessType()
    	{
    		return mAccessType;
    	}
    	
    	public int getBandwidth()
    	{
    		return mBandwidth;
    	}
    	
    	public int getSlotsPerCarrier()
    	{
    		return mSlotsPerCarrier;
    	}
    	
    	public Vocoder getVocoder()
    	{
    		return mVocoder;
    	}
    }
    
    public enum AccessType
    {
    	FDMA,TDMA,UNKNOWN;
    }
    
    public enum Vocoder
    {
    	HALF_RATE, FULL_RATE;
    }
}