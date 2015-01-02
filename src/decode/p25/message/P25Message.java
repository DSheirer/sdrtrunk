package decode.p25.message;

import java.text.SimpleDateFormat;

import map.Plottable;
import message.Message;
import alias.Alias;
import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.reference.DataUnitID;
import edac.CRC;

public class P25Message extends Message
{
    public enum DuplexMode { HALF, FULL };
    public enum SessionMode { PACKET, CIRCUIT };
	
	public static final int[] NAC = { 0,1,2,3,4,5,6,7,8,9,10,11 };
	public static final int[] DUID = { 12,13,14,15 };
	public static final int[] BCH = { 16,17,18,19,20,21,22,23,24,25,26,27,28,29,
		30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,
		54,55,56,57,58,59,60,61,62,63 };
	
	protected SimpleDateFormat mTimeDurationFormat = 
							new SimpleDateFormat( "HH:mm:ss.SSS");
	
	protected BitSetBuffer mMessage;
	protected DataUnitID mDUID;
	protected AliasList mAliasList;
	
	protected CRC[] mCRC;
	
	public P25Message( BitSetBuffer message, 
	                   DataUnitID duid,
	                   AliasList aliasList )
	{
		super();
		
		mMessage = message;
		mDUID = duid;
		mAliasList = aliasList;
	}
	
	public BitSetBuffer getSourceMessage()
	{
		return mMessage;
	}
	
	public AliasList getAliasList()
	{
		return mAliasList;
	}
	
	public String getNAC()
	{
		return mMessage.getHex( NAC, 3 );
	}
	
	public DataUnitID getDUID()
	{
		return mDUID;
	}
	
	@Override
    public String getErrorStatus()
    {
		if( mCRC == null )
		{
			return "UNKNOWN";
		}
		
		return CRC.format( mCRC );
    }

	@Override
    public String getMessage()
    {
		StringBuilder sb = new StringBuilder();
		
		sb.append( "NAC:" );
		sb.append( getNAC() );
		sb.append( " " );
		sb.append( getDUID().getLabel() );
		
	    return sb.toString();
    }

	@Override
    public String getBinaryMessage()
    {
	    return mMessage.toString();
    }

	@Override
    public String getProtocol()
    {
	    return "P25 Phase 1";
    }

	@Override
    public String getEventType()
    {
	    return mDUID.name();
    }

	@Override
    public String getFromID()
    {
	    return null;
    }

	@Override
    public Alias getFromIDAlias()
    {
        if( mAliasList != null )
        {
            return mAliasList.getTalkgroupAlias( getFromID() );
        }
        
        return null;
    }

	@Override
    public String getToID()
    {
	    return null;
    }

	@Override
    public Alias getToIDAlias()
    {
	    if( mAliasList != null )
	    {
	        return mAliasList.getTalkgroupAlias( getToID() );
	    }
	    
	    return null;
    }

	@Override
    public Plottable getPlottable()
    {
	    return null;
    }

	@Override
    public String toString()
    {
	    return getMessage();
    }

	@Override
    public boolean isValid()
    {
		if( mCRC != null )
		{
			for( CRC crc: mCRC )
			{
				if( crc == CRC.FAILED_CRC )
				{
					return false;
				}
			}
		}
		
		return true;
    }

	/**
	 * Calculates the frequency of the uplink channel using the channel number
	 * and the IdentifierUpdate message.
	 * 
	 * @param iden - Identifier Update message
	 * @param channel - channel number
	 * @return - frequency in Hertz
	 */
	protected static long calculateUplink( IdentifierProvider iden, int channel )
	{
		long downlink = calculateDownlink( iden, channel );
		
		if( downlink > 0 && iden != null )
		{
			return downlink + iden.getTransmitOffset();
		}
		
		return 0;
	}
	
	/**
	 * Calculates the frequency of the downlink channel using the channel number
	 * and the IdentifierUpdate message.
	 * 
	 * @param iden - Identifier Update message
	 * @param channel - channel number
	 * @return - frequency in Hertz
	 */
	protected static long calculateDownlink( IdentifierProvider iden, int channel )
	{
		if( iden != null )
		{
			return iden.getBaseFrequency() + 
					( channel * iden.getChannelSpacing() );
		}
		
		return 0;
	}
	
}
