package decode.p25.message.ldu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alias.AliasList;
import bits.BinaryMessage;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Encryption;
import edac.CRC;
import edac.Hamming10;
import edac.ReedSolomon_63_47_17;

public class LDU2Message extends LDUMessage
{
	private final static Logger mLog = LoggerFactory.getLogger( LDU2Message.class );

	public static final int[] MESSAGE_INDICATOR_A = { 352,353,354,355,356,357,
		362,363,364,365,366,367,372,373,374,375,376,377,382,383,384,385,386,387,
		536,537,538,539,540,541,546,547,548,549,550,551	};
	public static final int[] MESSAGE_INDICATOR_B = { 556,557,558,559,560,561,
		566,567,568,569,570,571,720,721,722,723,724,725,730,731,732,733,734,735,
		740,741,742,743,744,745,750,751,752,753,754,755 };
	public static final int[] ALGORITHM_ID = { 904,905,906,907,908,909,914,915 };
	public static final int[] KEY_ID = { 916,917,918,919,924,925,926,927,928,
		929,934,935,936,937,938,939 };

	public static final int[] GOLAY_WORD_STARTS = { 
		352,362,372,382,536,546,556,566,720,730,740,750,904,914,924,934,1088,
		1098,1108,1118,1272,1282,1292,1302 };

	public static final int[] CW_HEX_0 = { 352,353,354,355,356,357 };
	public static final int[] CW_HEX_1 = { 362,363,364,365,366,367 };
	public static final int[] CW_HEX_2 = { 372,373,374,375,376,377 };
	public static final int[] CW_HEX_3 = { 382,383,384,385,386,387 };
	public static final int[] CW_HEX_4 = { 536,537,538,539,540,541 };
	public static final int[] CW_HEX_5 = { 546,547,548,549,550,551 };
	public static final int[] CW_HEX_6 = { 556,557,558,559,560,561 };
	public static final int[] CW_HEX_7 = { 566,567,568,569,570,571 };
	public static final int[] CW_HEX_8 = { 720,721,722,723,724,725 };
	public static final int[] CW_HEX_9 = { 730,731,732,733,734,735 };
	public static final int[] CW_HEX_10 = { 740,741,742,743,744,745 };
	public static final int[] CW_HEX_11 = { 750,751,752,753,754,755 };
	public static final int[] CW_HEX_12 = { 904,905,906,907,908,909 };
	public static final int[] CW_HEX_13 = { 914,915,916,917,918,919 };
	public static final int[] CW_HEX_14 = { 924,925,926,927,928,929 };
	public static final int[] CW_HEX_15 = { 934,935,936,937,938,939 };
	public static final int[] RS_HEX_0 = { 1088,1089,1090,1091,1092,1093 };
	public static final int[] RS_HEX_1 = { 1098,1099,1100,1101,1102,1103 };
	public static final int[] RS_HEX_2 = { 1108,1109,1110,1111,1112,1113 };
	public static final int[] RS_HEX_3 = { 1118,1119,1120,1121,1122,1123 };
	public static final int[] RS_HEX_4 = { 1272,1273,1274,1275,1276,1277 };
	public static final int[] RS_HEX_5 = { 1282,1283,1284,1285,1286,1287 };
	public static final int[] RS_HEX_6 = { 1292,1293,1294,1295,1296,1297 };
	public static final int[] RS_HEX_7 = { 1302,1303,1304,1305,1306,1307 };

	/* Reed-Solomon(24,16,9) code protects the encryption sync word.  Maximum
	 * correctable errors are: Hamming Distance(9) / 2 = 4  */
	public static final ReedSolomon_63_47_17 mReedSolomonDecoder = 
			new ReedSolomon_63_47_17( 4 );

	public LDU2Message( BinaryMessage message, DataUnitID duid,
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
    	mCRC[ 1 ] = CRC.PASSED;

    	/* Hamming( 10,6,3 ) error detection and correction */
		for( int index: GOLAY_WORD_STARTS )
		{
			int errors = Hamming10.checkAndCorrect( mMessage, index );
			
			if( errors > 1 )
			{
				mCRC[ 1 ] = CRC.FAILED_CRC;
			}
		}

		/* Reed-Solomon( 24,16,9 ) error detection and correction
		 * Check the Reed-Solomon parity bits. The RS decoder expects the code 
		 * words and reed solomon parity hex codewords in reverse order.  
		 * 
		 * Since this is a truncated RS(63) codes, we pad the code with zeros */
        int[] input = new int [63];
        int[] output = new int [63];
        
        input[  0 ] = mMessage.getInt( RS_HEX_7 );
        input[  1 ] = mMessage.getInt( RS_HEX_6 );
        input[  2 ] = mMessage.getInt( RS_HEX_5 );
        input[  3 ] = mMessage.getInt( RS_HEX_4 );
        input[  4 ] = mMessage.getInt( RS_HEX_3 );
        input[  5 ] = mMessage.getInt( RS_HEX_2 );
        input[  6 ] = mMessage.getInt( RS_HEX_1 );
        input[  7 ] = mMessage.getInt( RS_HEX_0 );
        
        input[  8 ] = mMessage.getInt( CW_HEX_15 );
        input[  9 ] = mMessage.getInt( CW_HEX_14 );
        input[ 10 ] = mMessage.getInt( CW_HEX_13 );
        input[ 11 ] = mMessage.getInt( CW_HEX_12 );
        input[ 12 ] = mMessage.getInt( CW_HEX_11 );
        input[ 13 ] = mMessage.getInt( CW_HEX_10 );
        input[ 14 ] = mMessage.getInt( CW_HEX_9 );
        input[ 15 ] = mMessage.getInt( CW_HEX_8 );
        input[ 16 ] = mMessage.getInt( CW_HEX_7 );
        input[ 17 ] = mMessage.getInt( CW_HEX_6 );
        input[ 18 ] = mMessage.getInt( CW_HEX_5 );
        input[ 19 ] = mMessage.getInt( CW_HEX_4 );
        input[ 20 ] = mMessage.getInt( CW_HEX_3 );
        input[ 21 ] = mMessage.getInt( CW_HEX_2 );
        input[ 22 ] = mMessage.getInt( CW_HEX_1 );
        input[ 23 ] = mMessage.getInt( CW_HEX_0 );
        /* indexes 24 - 62 are defaulted to zero */

        boolean irrecoverableErrors = mReedSolomonDecoder.decode( input, output );

        if( irrecoverableErrors )
        {
        	mCRC[ 2 ] = CRC.FAILED_CRC;
        }
        else
        {
        	/* Since we've detected that we can correct the RS words, mark the
        	 * hamming crc as corrected as well, if it is currently failed */
        	if( mCRC[ 1 ] == CRC.FAILED_CRC )
        	{
				mCRC[ 1 ] = CRC.CORRECTED;
        	}
        	
        	mCRC[ 2 ] = CRC.PASSED;
        	
        	/* Only fix the codeword data hex codewords */
        	repairHexCodeword( input, output, 8, CW_HEX_15 );
        	repairHexCodeword( input, output, 9, CW_HEX_14 );
        	repairHexCodeword( input, output, 10, CW_HEX_13 );
        	repairHexCodeword( input, output, 11, CW_HEX_12 );
        	repairHexCodeword( input, output, 12, CW_HEX_11 );
        	repairHexCodeword( input, output, 13, CW_HEX_10 );
        	repairHexCodeword( input, output, 14, CW_HEX_9 );
        	repairHexCodeword( input, output, 15, CW_HEX_8 );
        	repairHexCodeword( input, output, 16, CW_HEX_7 );
        	repairHexCodeword( input, output, 17, CW_HEX_6 );
        	repairHexCodeword( input, output, 18, CW_HEX_5 );
        	repairHexCodeword( input, output, 19, CW_HEX_4 );
        	repairHexCodeword( input, output, 20, CW_HEX_3 );
        	repairHexCodeword( input, output, 21, CW_HEX_2 );
        	repairHexCodeword( input, output, 22, CW_HEX_1 );
        	repairHexCodeword( input, output, 23, CW_HEX_0 );
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
		
		
		Encryption encryption = getEncryption();
		
		if( encryption != Encryption.UNENCRYPTED )
		{
			sb.append( "NAC:" );
			sb.append( getNAC() );
			sb.append( " " );
			sb.append( getDUID().getLabel() );
			
			sb.append( " ENCRYPTED VOICE:" );
			sb.append( encryption.name() );
			sb.append( " KEY ID:" + getKeyID() );
			sb.append( " MSG INDICATOR:" + getMessageIndicator() );
		}
		else
		{
			sb.append( super.getMessageStub() );
			sb.append( "UNENCRYPTED" );
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
	
	public int getKeyID()
	{
		return mMessage.getInt( KEY_ID );
	}
	
	public static void main( String[] args )
	{
		String msg = "001010100000101010011010001001001001010011000111000111110111001100000010000101001011110111111000001100111100110000000010001110011011001101101101101101110011110011010000101111001111000011000010110010011110110010000100101101101001100000100000100001011100000000001110011111011000100100000000011011100000111111111101111010110111000111100011010111111110111000000000000000000000000000000000000000000100100001110110101111011000011010010100011001000101001011100101111011000001101100010101111001000100001111011111011001111110100111001100110110000000000000000000000000000000000000000000000101000001011110111010110010000100010110001100001101001110111001000111100100111011011011110101011111011000010100110111001000101000010011101010000000000000000000000000000000000000000001001000001000011011110110011110011001111100100000111000101001110111011110101001000101001100001110001100000000110000110000001011110000000101100110000011100000000000000000000000000000000111011001010000111111111111110000110110011000100101011000001111110000101000101111010101010101000100110111000100100111110010101010000100111010111010111010001011010010001011011001001001011100000101000010010010111100000110010000101010000110101100101111100110111100010001011011110101001011010100010000111000010000001001001001101000100110101011110100111100011111110101100000011011011001000001010100011001010100110000111100101011100110110011111100011011111110100111011101011111100001111011000101100011101000110001101000000000000000000000000000000000010001101101011101111010101010000111011011101001011111001110111001000010101011010000001011010111010000011011001100010110001010001000010001101101";
		
		BinaryMessage message = BinaryMessage.load( msg );

		LDU2Message ldu2 = new LDU2Message( message, DataUnitID.LDU2, null );
		
		mLog.debug( ldu2.getMessage() );
		mLog.debug( "CRC: " + CRC.format( ldu2.getCRCResults() ));
	}
}
