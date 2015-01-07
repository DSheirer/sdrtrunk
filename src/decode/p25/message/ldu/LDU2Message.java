package decode.p25.message.ldu;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Encryption;
import edac.CRC;

public class LDU2Message extends LDUMessage
{
//  public static final int[] OCTET_0 = { 352,353,354,355,356,357,362,363 };	
//	public static final int[] OCTET_1 = { 364,365,366,367,372,373,374,375 };
//	public static final int[] OCTET_2 = { 376,377,382,383,384,385,386,387 };
//	public static final int[] OCTET_3 = { 536,537,538,539,540,541,546,547 };
//	public static final int[] OCTET_4 = { 548,549,550,551,556,557,558,559 };
//	public static final int[] OCTET_5 = { 560,561,566,567,568,569,570,571 };
//	public static final int[] OCTET_6 = { 720,721,722,723,724,725,730,731 };
//	public static final int[] OCTET_7 = { 732,733,734,735,740,741,742,743 };
//	public static final int[] OCTET_8 = { 744,745,750,751,752,753,754,755 };

	public static final int[] MESSAGE_INDICATOR_A = { 352,353,354,355,356,357,
		362,363,364,365,366,367,372,373,374,375,376,377,382,383,384,385,386,387,
		536,537,538,539,540,541,546,547,548,549,550,551	};
	public static final int[] MESSAGE_INDICATOR_B = { 556,557,558,559,560,561,
		566,567,568,569,570,571,720,721,722,723,724,725,730,731,732,733,734,735,
		740,741,742,743,744,745,750,751,752,753,754,755 };
	public static final int[] ALGORITHM_ID = { 904,905,906,907,908,909,914,915 };
	public static final int[] KEY_ID = { 916,917,918,919,924,925,926,927,928,
		929,934,935,936,937,938,939 };

	public LDU2Message( BitSetBuffer message, DataUnitID duid,
            AliasList aliasList )
    {
	    super( message, duid, aliasList );

	    /* NID CRC is checked in the message framer, thus a constructed message
	     * means it passed the CRC */
	    mCRC = new CRC[ 2 ];
	    mCRC[ 0 ] = CRC.PASSED;
    }
	
	@Override
    public String getMessage()
    {
		StringBuilder sb = new StringBuilder();
		
		sb.append( super.getMessageStub() );
		
		Encryption encryption = getEncryption();
		
		if( encryption != Encryption.UNENCRYPTED )
		{
			sb.append( "ENCRYPTION:" );
			sb.append( encryption.name() );
			sb.append( " KEY ID:" + getKeyID() );
			sb.append( " MSG INDICATOR:" + getMessageIndicator() );
		}
		else
		{
			sb.append( "UNENCRYPTED" );
		}

		sb.append( " " );
		sb.append( mMessage.toString() );
		
	    return sb.toString();
    }
	
	/* Temporary override */
	public boolean isValid()
	{
		return true;
	}
	
	public String getMessageIndicator()
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
	
	

}
