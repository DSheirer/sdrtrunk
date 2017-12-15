package module.decode.p25.message.hdu;

import module.decode.p25.message.P25Message;
import module.decode.p25.reference.DataUnitID;
import module.decode.p25.reference.Encryption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alias.AliasList;
import bits.BinaryMessage;
import edac.CRC;
import edac.Golay18;
import edac.ReedSolomon_63_47_17;

public class HDUMessage extends P25Message
{
	private final static Logger mLog = LoggerFactory.getLogger( HDUMessage.class );

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

	public static final int[] GOLAY_WORD_STARTS = { 64,82,100,118,136,154,172,
		190,208,226,244,262,280,298,316,334,352,370,388,406,424,442,460,478,
		496,514,532,550,568,586,604,622,640,658,676,694 };

	public static final int[] CW_HEX_0 = { 64,65,66,67,68,69 };
	public static final int[] CW_HEX_1 = { 82,83,84,85,86,87 };
	public static final int[] CW_HEX_2 = { 100,101,102,103,104,105 };
	public static final int[] CW_HEX_3 = { 118,119,120,121,122,123 };
	public static final int[] CW_HEX_4 = { 136,137,138,139,140,141 };
	public static final int[] CW_HEX_5 = { 154,155,156,157,158,159 };
	public static final int[] CW_HEX_6 = { 172,173,174,175,176,177 };
	public static final int[] CW_HEX_7 = { 190,191,192,193,194,195 };
	public static final int[] CW_HEX_8 = { 208,209,210,211,212,213 };
	public static final int[] CW_HEX_9 = { 226,227,228,229,230,231 };
	public static final int[] CW_HEX_10 = { 244,245,246,247,248,249 };
	public static final int[] CW_HEX_11 = { 262,263,264,265,266,267 };
	public static final int[] CW_HEX_12 = { 280,281,282,283,284,285 };
	public static final int[] CW_HEX_13 = { 298,299,300,301,302,303 };
	public static final int[] CW_HEX_14 = { 316,317,318,319,320,321 };
	public static final int[] CW_HEX_15 = { 334,335,336,337,338,339 };
	public static final int[] CW_HEX_16 = { 352,353,354,355,356,357 };
	public static final int[] CW_HEX_17 = { 370,371,372,373,374,375 };
	public static final int[] CW_HEX_18 = { 388,389,390,391,392,393 };
	public static final int[] CW_HEX_19 = { 406,407,408,409,410,411 };
	public static final int[] RS_HEX_0 = { 424,425,426,427,428,429 };
	public static final int[] RS_HEX_1 = { 442,443,444,445,446,447 };
	public static final int[] RS_HEX_2 = { 460,461,462,463,464,465 };
	public static final int[] RS_HEX_3 = { 478,479,480,481,482,483 };
	public static final int[] RS_HEX_4 = { 496,497,498,499,500,501 };
	public static final int[] RS_HEX_5 = { 514,515,516,517,518,519 };
	public static final int[] RS_HEX_6 = { 532,533,534,535,536,537 };
	public static final int[] RS_HEX_7 = { 550,551,552,553,554,555 };
	public static final int[] RS_HEX_8 = { 568,569,570,571,572,573 };
	public static final int[] RS_HEX_9 = { 586,587,588,589,590,591 };
	public static final int[] RS_HEX_10 = { 604,605,606,607,608,609 };
	public static final int[] RS_HEX_11 = { 622,623,624,625,626,627 };
	public static final int[] RS_HEX_12 = { 640,641,642,643,644,645 };
	public static final int[] RS_HEX_13 = { 658,659,660,661,662,663 };
	public static final int[] RS_HEX_14 = { 676,677,678,679,680,681 };
	public static final int[] RS_HEX_15 = { 694,695,696,697,698,699 };
	
	/* Reed-Solomon(36,20,17) code protects the header word.  Maximum
	 * correctable errors are: Hamming Distance(17) / 2 = 8  */
	public static final ReedSolomon_63_47_17 mReedSolomonDecoder = 
			new ReedSolomon_63_47_17( 8 );
	
	public HDUMessage( BinaryMessage message, DataUnitID duid,
            AliasList aliasList )
    {
	    super( message, duid, aliasList );

	    /* NID CRC is checked in the message framer, thus a constructed message
	     * means it passed the CRC */
	    mCRC = new CRC[ 3 ];
	    mCRC[ 0 ] = CRC.PASSED;
	    
	    checkCRC();
    }
	
	private void checkCRC()
	{
		/* Golay( 18,6,18 ) error detection and correction */
		for( int index: GOLAY_WORD_STARTS )
		{
			Golay18.checkAndCorrect( mMessage, index );
		}
		
    	mCRC[ 1 ] = CRC.PASSED;

		/* Reed-Solomon( 36,20,17 ) error detection and correction
		 * Check the Reed-Solomon parity bits. The RS decoder expects the link 
		 * control data and reed solomon parity hex codewords in reverse order.  
		 * The RS(24,12,13) code used by P25 removes the left-hand 47 data hex 
		 * words, so we replace them with zeros. */
        int[] input = new int [63];
        int[] output = new int [63];
        
        input[  0 ] = mMessage.getInt( RS_HEX_15 );
        input[  1 ] = mMessage.getInt( RS_HEX_14 );
        input[  2 ] = mMessage.getInt( RS_HEX_13 );
        input[  3 ] = mMessage.getInt( RS_HEX_12 );
        input[  4 ] = mMessage.getInt( RS_HEX_11 );
        input[  5 ] = mMessage.getInt( RS_HEX_10 );
        input[  6 ] = mMessage.getInt( RS_HEX_9 );
        input[  7 ] = mMessage.getInt( RS_HEX_8 );
        input[  8 ] = mMessage.getInt( RS_HEX_7 );
        input[  9 ] = mMessage.getInt( RS_HEX_6 );
        input[ 10 ] = mMessage.getInt( RS_HEX_5 );
        input[ 11 ] = mMessage.getInt( RS_HEX_4 );
        input[ 12 ] = mMessage.getInt( RS_HEX_3 );
        input[ 13 ] = mMessage.getInt( RS_HEX_2 );
        input[ 14 ] = mMessage.getInt( RS_HEX_1 );
        input[ 15 ] = mMessage.getInt( RS_HEX_0 );
        
        input[ 16 ] = mMessage.getInt( CW_HEX_19 );
        input[ 17 ] = mMessage.getInt( CW_HEX_18 );
        input[ 18 ] = mMessage.getInt( CW_HEX_17 );
        input[ 19 ] = mMessage.getInt( CW_HEX_16 );
        input[ 20 ] = mMessage.getInt( CW_HEX_15 );
        input[ 21 ] = mMessage.getInt( CW_HEX_14 );
        input[ 22 ] = mMessage.getInt( CW_HEX_13 );
        input[ 23 ] = mMessage.getInt( CW_HEX_12 );
        input[ 24 ] = mMessage.getInt( CW_HEX_11 );
        input[ 25 ] = mMessage.getInt( CW_HEX_10 );
        input[ 26 ] = mMessage.getInt( CW_HEX_9 );
        input[ 27 ] = mMessage.getInt( CW_HEX_8 );
        input[ 28 ] = mMessage.getInt( CW_HEX_7 );
        input[ 29 ] = mMessage.getInt( CW_HEX_6 );
        input[ 30 ] = mMessage.getInt( CW_HEX_5 );
        input[ 31 ] = mMessage.getInt( CW_HEX_4 );
        input[ 32 ] = mMessage.getInt( CW_HEX_3 );
        input[ 33 ] = mMessage.getInt( CW_HEX_2 );
        input[ 34 ] = mMessage.getInt( CW_HEX_1 );
        input[ 35 ] = mMessage.getInt( CW_HEX_0 );
        /* indexes 36 - 62 are defaulted to zero */

        boolean irrecoverableErrors = mReedSolomonDecoder.decode( input, output );

        if( irrecoverableErrors )
        {
        	mCRC[ 2 ] = CRC.FAILED_CRC;
        }
        else
        {
        	mCRC[ 2 ] = CRC.PASSED;
        	
        	/* Only fix the codeword data hex codewords */
        	repairHexCodeword( input, output, 16, CW_HEX_19 );
        	repairHexCodeword( input, output, 17, CW_HEX_18 );
        	repairHexCodeword( input, output, 18, CW_HEX_17 );
        	repairHexCodeword( input, output, 19, CW_HEX_16 );
        	repairHexCodeword( input, output, 20, CW_HEX_15 );
        	repairHexCodeword( input, output, 21, CW_HEX_14 );
        	repairHexCodeword( input, output, 22, CW_HEX_13 );
        	repairHexCodeword( input, output, 23, CW_HEX_12 );
        	repairHexCodeword( input, output, 24, CW_HEX_11 );
        	repairHexCodeword( input, output, 25, CW_HEX_10 );
        	repairHexCodeword( input, output, 26, CW_HEX_9 );
        	repairHexCodeword( input, output, 27, CW_HEX_8 );
        	repairHexCodeword( input, output, 28, CW_HEX_7 );
        	repairHexCodeword( input, output, 29, CW_HEX_6 );
        	repairHexCodeword( input, output, 30, CW_HEX_5 );
        	repairHexCodeword( input, output, 31, CW_HEX_4 );
        	repairHexCodeword( input, output, 32, CW_HEX_3 );
        	repairHexCodeword( input, output, 33, CW_HEX_2 );
        	repairHexCodeword( input, output, 34, CW_HEX_1 );
        	repairHexCodeword( input, output, 35, CW_HEX_0 );
        }
	}
	
	private void repairHexCodeword( int[] input, int[] output, int index, int[] indexSet )
	{
		if( input[ index ] != output[ index ] )
		{
			mMessage.load( indexSet[ 0 ], 6, output[ index ] );
			mCRC[ 2 ] = CRC.CORRECTED;
		}
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
			
			if( encryption == Encryption.UNKNOWN )
			{
				sb.append( "UNK ALGO ID:" );
				sb.append( mMessage.getInt( ALGORITHM_ID ) );
			}
			else
			{
				sb.append( encryption.name() );
			}
			sb.append( " KEY ID:" + getKeyID() );
			sb.append( " MSG INDICATOR:" + getMessageIndicator() );
		}
		else
		{
			sb.append( " UNENCRYPTED" );
		}
		
		sb.append( " " );
		sb.append( mMessage.toString() );
		
	    return sb.toString();
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
	
	public boolean isEncryptedAudio()
	{
		return getEncryption() != Encryption.UNENCRYPTED;
	}
	
	public int getKeyID()
	{
		return mMessage.getInt( KEY_ID );
	}
	
	public String getTalkgroupID()
	{
		return mMessage.getHex( TALKGROUP_ID, 4 );
	}
	
	@Override
	public String getToID()
	{
		return getTalkgroupID();
	}
	
	public boolean isValid()
	{
		return mCRC[ 2 ] != null && mCRC[ 2 ] != CRC.FAILED_CRC;
	}
}
