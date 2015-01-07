package decode.p25.message.ldu;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.P25Interleave;
import decode.p25.message.P25Message;
import decode.p25.reference.DataUnitID;
import edac.CRC;

public class LDUMessage extends P25Message
{
	public static final int IMBE_1 = 64;
	public static final int IMBE_2 = 208;
	public static final int IMBE_3 = 392;
	public static final int IMBE_4 = 576;
	public static final int IMBE_5 = 760;
	public static final int IMBE_6 = 944;
	public static final int IMBE_7 = 1128;
	public static final int IMBE_8 = 1312;
	public static final int IMBE_9 = 1488;
	
	public static final int[] LOW_SPEED_DATA = { 1456,1457,1458,1459,1460,1461,
		1462,1463,1472,1473,1474,1475,1476,1477,1478,1479 };

	public LDUMessage( BitSetBuffer message, DataUnitID duid,
            AliasList aliasList )
    {
	    super( message, duid, aliasList );

	    deinterleaveVoiceFrames();

	    /* NID CRC is checked in the message framer, thus a constructed message
	     * means it passed the CRC */
	    mCRC = new CRC[ 2 ];
	    mCRC[ 0 ] = CRC.PASSED;
    }
	
	@Override
    public String getMessage()
    {
	    return getMessageStub();
    }
	
    public String getMessageStub()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( "NAC:" );
		sb.append( getNAC() );
		sb.append( " " );
		sb.append( getDUID().getLabel() );
		sb.append( " VOICE LSD:" );
		sb.append( getLowSpeedData() );
		sb.append( " " );
		
		return sb.toString();
    }
	
	public String getLowSpeedData()
	{
		return mMessage.getHex( LOW_SPEED_DATA, 4 );
	}

	private void deinterleaveVoiceFrames()
	{
		P25Interleave.deinterleaveVoice( mMessage, IMBE_1, IMBE_1 + 144 );
		P25Interleave.deinterleaveVoice( mMessage, IMBE_2, IMBE_2 + 144 );
		P25Interleave.deinterleaveVoice( mMessage, IMBE_3, IMBE_3 + 144 );
		P25Interleave.deinterleaveVoice( mMessage, IMBE_4, IMBE_4 + 144 );
		P25Interleave.deinterleaveVoice( mMessage, IMBE_5, IMBE_5 + 144 );
		P25Interleave.deinterleaveVoice( mMessage, IMBE_6, IMBE_6 + 144 );
		P25Interleave.deinterleaveVoice( mMessage, IMBE_7, IMBE_7 + 144 );
		P25Interleave.deinterleaveVoice( mMessage, IMBE_8, IMBE_8 + 144 );
		P25Interleave.deinterleaveVoice( mMessage, IMBE_9, IMBE_9 + 144 );
	}
	
	/* Temporary override */
	public boolean isValid()
	{
		return true;
	}
}
