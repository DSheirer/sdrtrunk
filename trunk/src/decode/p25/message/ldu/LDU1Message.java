package decode.p25.message.ldu;

import alias.AliasList;
import bits.BinaryMessage;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.LinkControlOpcode;
import decode.p25.reference.Vendor;
import decode.p25.reference.VendorLinkControlOpcode;
import edac.CRC;
import edac.Hamming10;
import edac.ReedSolomon_63_47_17;

public class LDU1Message extends LDUMessage
{
	public static final int ENCRYPTION_FLAG = 352;
	public static final int IMPLICIT_VENDOR_FLAG = 353;
	public static final int[] OPCODE = { 354,355,356,357,362,363 };
	public static final int[] VENDOR = { 364,365,366,367,372,373,374,375 };
	
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
	public static final int[] RS_HEX_0 = { 904,905,906,907,908,909 };
	public static final int[] RS_HEX_1 = { 914,915,916,917,918,919 };
	public static final int[] RS_HEX_2 = { 924,925,926,927,928,929 };
	public static final int[] RS_HEX_3 = { 934,935,936,937,938,939 };
	public static final int[] RS_HEX_4 = { 1088,1089,1090,1091,1092,1093 };
	public static final int[] RS_HEX_5 = { 1098,1099,1100,1101,1102,1103 };
	public static final int[] RS_HEX_6 = { 1108,1109,1110,1111,1112,1113 };
	public static final int[] RS_HEX_7 = { 1118,1119,1120,1121,1122,1123 };
	public static final int[] RS_HEX_8 = { 1272,1273,1274,1275,1276,1277 };
	public static final int[] RS_HEX_9 = { 1282,1283,1284,1285,1286,1287 };
	public static final int[] RS_HEX_10 = { 1292,1293,1294,1295,1296,1297 };
	public static final int[] RS_HEX_11 = { 1302,1303,1304,1305,1306,1307 };

	/* Reed-Solomon(24,12,13) code protects the link control word.  Maximum
	 * correctable errors are: Hamming Distance(13) / 2 = 6  */
	public static final ReedSolomon_63_47_17 mReedSolomonDecoder = 
			new ReedSolomon_63_47_17( 6 );

	public LDU1Message( BinaryMessage message, DataUnitID duid,
            AliasList aliasList )
    {
	    super( message, duid, aliasList );

	    
	    /* NID CRC is checked in the message framer, thus a constructed message
	     * means it passed the CRC */
	    mCRC = new CRC[ 3 ];
	    mCRC[ 0 ] = CRC.PASSED;
	    
	    checkCRC();
    }
	
	/**
	 * Subclass constructor from an existing LDU1Message
	 */
	protected LDU1Message( LDU1Message message )
	{
		super( message.getSourceMessage(), DataUnitID.LDU1, message.getAliasList() );
		
		mCRC = message.mCRC;
	}

	@Override
	public boolean isEncrypted()
	{
		return mMessage.get( ENCRYPTION_FLAG );
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
        
        input[  0 ] = mMessage.getInt( RS_HEX_11 );
        input[  1 ] = mMessage.getInt( RS_HEX_10 );
        input[  2 ] = mMessage.getInt( RS_HEX_9 );
        input[  3 ] = mMessage.getInt( RS_HEX_8 );
        input[  4 ] = mMessage.getInt( RS_HEX_7 );
        input[  5 ] = mMessage.getInt( RS_HEX_6 );
        input[  6 ] = mMessage.getInt( RS_HEX_5 );
        input[  7 ] = mMessage.getInt( RS_HEX_4 );
        input[  8 ] = mMessage.getInt( RS_HEX_3 );
        input[  9 ] = mMessage.getInt( RS_HEX_2 );
        input[ 10 ] = mMessage.getInt( RS_HEX_1 );
        input[ 11 ] = mMessage.getInt( RS_HEX_0 );
        
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
		
		sb.append( getMessageStub() );
		sb.append( " " );
		sb.append( mMessage.toString() );
		
		return sb.toString();
	}
	
    public String getMessageStub()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( super.getMessageStub() );
		
		if( isEncrypted() )
		{
			sb.append( "ENCRYPTED" );
		}
		else
		{
			if( isImplicitFormat() || getVendor() == Vendor.STANDARD )
			{
				sb.append( getOpcode().getLabel() );
			}
			else
			{
				sb.append( "VENDOR:" + getVendor().getLabel() + " " );
				sb.append( getVendorOpcode().getLabel() );
			}
		}

	    return sb.toString();
    }

	public boolean isImplicitFormat()
	{
		return mMessage.get( IMPLICIT_VENDOR_FLAG );
	}
	
	public LinkControlOpcode getOpcode()
	{
		return LinkControlOpcode.fromValue( mMessage.getInt( OPCODE ) );
	}

	public VendorLinkControlOpcode getVendorOpcode()
	{
		return VendorLinkControlOpcode.fromValue( mMessage.getInt( OPCODE ) );
	}
	
	public Vendor getVendor()
	{
		if( isImplicitFormat() )
		{
			return Vendor.STANDARD;
		}
		
		return Vendor.fromValue( mMessage.getInt( VENDOR ) );
	}
}
