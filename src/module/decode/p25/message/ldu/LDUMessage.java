package module.decode.p25.message.ldu;

import java.util.ArrayList;
import java.util.List;

import module.decode.p25.message.P25Message;
import module.decode.p25.reference.DataUnitID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alias.AliasList;
import bits.BinaryMessage;

public abstract class LDUMessage extends P25Message
{
	private final static Logger mLog = LoggerFactory.getLogger( LDUMessage.class );

	public static final int IMBE_FRAME_1 = 64;
	public static final int IMBE_FRAME_2 = 208;
	public static final int IMBE_FRAME_3 = 392;
	public static final int IMBE_FRAME_4 = 576;
	public static final int IMBE_FRAME_5 = 760;
	public static final int IMBE_FRAME_6 = 944;
	public static final int IMBE_FRAME_7 = 1128;
	public static final int IMBE_FRAME_8 = 1312;
	public static final int IMBE_FRAME_9 = 1488;

	public static final int[] LOW_SPEED_DATA = { 1456,1457,1458,1459,1460,1461,
		1462,1463,1472,1473,1474,1475,1476,1477,1478,1479 };

	public LDUMessage( BinaryMessage message, DataUnitID duid,
            AliasList aliasList )
    {
	    super( message, duid, aliasList );
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

	public boolean isValid()
	{
//		return mCRC[ 2 ] != null && mCRC[ 2 ] != CRC.FAILED_CRC;
		return true;
	}

	/**
	 * Returns a 162 byte array containing 9 IMBE voice frames of 18-bytes 
	 * (144-bits) each.  Each frame is intact as transmitted and requires 
	 * deinterleaving, error correction, derandomizing, etc.
	 */
	public List<byte[]> getIMBEFrames()
	{
		List<byte[]> frames = new ArrayList<byte[]>();
		
		frames.add( mMessage.get( IMBE_FRAME_1, IMBE_FRAME_1 + 144 ).toByteArray() );
		frames.add( mMessage.get( IMBE_FRAME_2, IMBE_FRAME_2 + 144 ).toByteArray() );
		frames.add( mMessage.get( IMBE_FRAME_3, IMBE_FRAME_3 + 144 ).toByteArray() );
		frames.add( mMessage.get( IMBE_FRAME_4, IMBE_FRAME_4 + 144 ).toByteArray() );
		frames.add( mMessage.get( IMBE_FRAME_5, IMBE_FRAME_5 + 144 ).toByteArray() );
		frames.add( mMessage.get( IMBE_FRAME_6, IMBE_FRAME_6 + 144 ).toByteArray() );
		frames.add( mMessage.get( IMBE_FRAME_7, IMBE_FRAME_7 + 144 ).toByteArray() );
		frames.add( mMessage.get( IMBE_FRAME_8, IMBE_FRAME_8 + 144 ).toByteArray() );
		frames.add( mMessage.get( IMBE_FRAME_9, IMBE_FRAME_9 + 144 ).toByteArray() );

		return frames;
	}
}
