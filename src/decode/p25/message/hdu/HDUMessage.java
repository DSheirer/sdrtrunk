package decode.p25.message.hdu;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.P25Message;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Encryption;
import edac.CRC;

public class HDUMessage extends P25Message
{
	public static final int[] MESSAGE_INDICATOR_A = { 64,65,66,67,68,69,82,83,
		84,85,86,87,100,101,102,103,104,105,118,119,120,121,122,123,136,137,138,
		139,140,141,154,155,156,157,158,159 };
	public static final int[] MESSAGE_INDICATOR_B = { 172,173,174,175,176,177,
		190,191,192,193,194,195,208,209,210,211,212,213,226,227,228,229,230,231,
		244,245,246,247,248,249,262,263,264,265,266,267 };
	public static final int[] VENDOR_ID = { 280,281,282,283,284,285,298,299 };
	public static final int[] ALGORITHM_ID = { 300,301,302,303,316,317,318,319 };
	public static final int[] KEY_ID = { 320,321,334,335,336,337,338,339,352,
		353,354,355,356,357,370,371	};
	public static final int[] TALKGROUP_ID = { 372,373,374,375,388,389,390,391,
		392,393,406,407,408,409,410,411 };
	
	public HDUMessage( BitSetBuffer message, DataUnitID duid,
            AliasList aliasList )
    {
	    super( message, duid, aliasList );

	    /* NID CRC is checked in the message framer, thus a constructed message
	     * means it passed the CRC */
	    mCRC = new CRC[ 2 ];
	    mCRC[ 0 ] = CRC.PASSED;
	    
	    checkCRC();
    }
	
	private void checkCRC()
	{
		/* Do Golay(18,6,8) check here */
		/* Do RS(36,20,17) check here */
	}
	
	@Override
    public String getMessage()
    {
		StringBuilder sb = new StringBuilder();
		
		sb.append( "NAC:" );
		sb.append( getNAC() );
		sb.append( " " );
		sb.append( getDUID().getLabel() );

		sb.append( " TALKGROUP:" + getTalkgroupID() );
		
		Encryption encryption = getEncryption();
		
		if( encryption != Encryption.UNENCRYPTED )
		{
			sb.append( " ENCRYPTION:" );
			sb.append( encryption.name() );
			sb.append( " KEY ID:" + getKeyID() );
			sb.append( " IV:" + getEncryptionInitializationVector() );
		}
		
		sb.append( " " );
		sb.append( mMessage.toString() );
		
	    return sb.toString();
    }
	
	public String getEncryptionInitializationVector()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( mMessage.getHex( MESSAGE_INDICATOR_A, 9 ) );
		sb.append( mMessage.getHex( MESSAGE_INDICATOR_B, 9 ) );

		return sb.toString();
	}
	
	public Encryption getEncryption()
	{
		return Encryption.fromValue( mMessage.getInt( ALGORITHM_ID ) );
	}
	
	public int getKeyID()
	{
		return mMessage.getInt( KEY_ID );
	}
	
	public String getTalkgroupID()
	{
		return mMessage.getHex( TALKGROUP_ID, 4 );
	}
	
	/* Temporary override */
	public boolean isValid()
	{
		return true;
	}

}
