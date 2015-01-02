package decode.p25.message.tdu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.P25Message;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.LinkControlOpcode;
import decode.p25.reference.Vendor;
import decode.p25.reference.VendorLinkControlOpcode;
import edac.CRC;
import edac.Galois24;
import edac.ReedSolomon_24_12_13;

public class TDULinkControlMessage extends P25Message
{
	public final static Logger mLog = LoggerFactory.getLogger( TDULinkControlMessage.class );

	public static final int[] LC_HEX_0 = { 64,65,66,67,68,69 };
	public static final int[] LC_HEX_1 = { 70,71,72,73,74,75 };
	public static final int[] LC_HEX_2 = { 88,89,90,91,92,93 };
	public static final int[] LC_HEX_3 = { 94,95,96,97,98,99 };
	public static final int[] LC_HEX_4 = { 112,113,114,115,116,117 };
	public static final int[] LC_HEX_5 = { 118,119,120,121,122,123 };
	public static final int[] LC_HEX_6 = { 136,137,138,139,140,141 };
	public static final int[] LC_HEX_7 = { 142,143,144,145,146,147 };
	public static final int[] LC_HEX_8 = { 160,161,162,163,164,165 };
	public static final int[] LC_HEX_9 = { 166,167,168,169,170,171 };
	public static final int[] LC_HEX_10 = { 184,185,186,187,188,189 };
	public static final int[] LC_HEX_11 = { 190,191,192,193,194,195 };
	public static final int[] RS_HEX_0 = { 208,209,210,211,212,213 };
	public static final int[] RS_HEX_1 = { 214,215,216,217,218,219 };
	public static final int[] RS_HEX_2 = { 232,233,234,235,236,237 };
	public static final int[] RS_HEX_3 = { 238,239,240,241,242,243 };
	public static final int[] RS_HEX_4 = { 256,257,258,259,260,261 };
	public static final int[] RS_HEX_5 = { 262,263,264,265,266,267 };
	public static final int[] RS_HEX_6 = { 280,281,282,283,284,285 };
	public static final int[] RS_HEX_7 = { 286,287,288,289,290,291 };
	public static final int[] RS_HEX_8 = { 304,305,306,307,308,309 };
	public static final int[] RS_HEX_9 = { 310,311,312,313,314,315 };
	public static final int[] RS_HEX_10 = { 328,329,330,331,332,333 };
	public static final int[] RS_HEX_11 = { 334,335,336,337,338,339 };
	
	public static final int ENCRYPTION_FLAG = 64;
	public static final int IMPLICIT_VENDOR_FLAG = 65;
	public static final int[] OPCODE = { 66,67,68,69,70,71 };
	public static final int[] VENDOR = { 72,73,74,75,88,89,90,91 };
	
	public static final ReedSolomon_24_12_13 mReedSolomonDecoder = 
						new ReedSolomon_24_12_13();
	
	public TDULinkControlMessage( BitSetBuffer message, DataUnitID duid,
            AliasList aliasList )
    {
	    super( message, duid, aliasList );

	    /* NID CRC is checked in the message framer so a constructed message
	     * means it passed the CRC */
	    mCRC = new CRC[ 3 ];
	    mCRC[ 0 ] = CRC.PASSED;
	    
	    checkCRC();
    }

	/**
	 * Constructs a TDULinkControl message from an existing message and bypasses
	 * the CRC calculation.
	 */
	protected TDULinkControlMessage( TDULinkControlMessage message )
	{
		super( message.getSourceMessage(), DataUnitID.TDULC, message.getAliasList() );

		mCRC = message.getCRC();
	}
	
	public CRC[] getCRC()
	{
		return mCRC;
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
		
		sb.append( "NAC:" );
		sb.append( getNAC() );
		sb.append( " " );
		sb.append( getDUID().getLabel() );
		sb.append( " " );
		
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

	public boolean isEncrypted()
	{
		return mMessage.get( ENCRYPTION_FLAG );
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
	
	
	/**
	 * Checks and repairs the 12 x 24-bit golay(24,12,8) codewords and then
	 * checks and repairs the 24 x 6-bit link control and reed-solomon 
	 * RS(24,12,13) parity codewords
	 */
	private void checkCRC()
	{
		/* Check the Golay codewords */
		int x = 64;

		mCRC[ 1 ] = CRC.PASSED;
		
		while( x < mMessage.size() )
		{
			boolean passes = Galois24.checkAndCorrect( mMessage, x );
			
			if( !passes )
			{
				mCRC[ 1 ] = CRC.FAILED_CRC;
			}
			
			x += 24;
		}
		
		/* Check the Reed-Solomon parity bits. The RS decoder expects the link 
		 * control data and reed solomon parity hex codewords in reverse order.  
		 * The RS(24,12,13) code used by P25 removes the left-hand 47 data hex 
		 * words, so we replace them with zeros. */
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
        input[ 12 ] = mMessage.getInt( LC_HEX_11 );
        input[ 13 ] = mMessage.getInt( LC_HEX_10 );
        input[ 14 ] = mMessage.getInt( LC_HEX_9 );
        input[ 15 ] = mMessage.getInt( LC_HEX_8 );
        input[ 16 ] = mMessage.getInt( LC_HEX_7 );
        input[ 17 ] = mMessage.getInt( LC_HEX_6 );
        input[ 18 ] = mMessage.getInt( LC_HEX_5 );
        input[ 19 ] = mMessage.getInt( LC_HEX_4 );
        input[ 20 ] = mMessage.getInt( LC_HEX_3 );
        input[ 21 ] = mMessage.getInt( LC_HEX_2 );
        input[ 22 ] = mMessage.getInt( LC_HEX_1 );
        input[ 23 ] = mMessage.getInt( LC_HEX_0 );
        /* indexes 24 - 62 are defaulted to zero */

        boolean irrecoverableErrors = mReedSolomonDecoder.decode( input, output );

        if( irrecoverableErrors )
        {
        	mCRC[ 2 ] = CRC.FAILED_CRC;
        }
        else
        {
        	mCRC[ 2 ] = CRC.PASSED;
        	
        	/* Only fix the link control data hex codewords */
        	repairHexCodeword( input, output, 12, LC_HEX_11 );
        	repairHexCodeword( input, output, 13, LC_HEX_10 );
        	repairHexCodeword( input, output, 14, LC_HEX_9 );
        	repairHexCodeword( input, output, 15, LC_HEX_8 );
        	repairHexCodeword( input, output, 16, LC_HEX_7 );
        	repairHexCodeword( input, output, 17, LC_HEX_6 );
        	repairHexCodeword( input, output, 18, LC_HEX_5 );
        	repairHexCodeword( input, output, 19, LC_HEX_4 );
        	repairHexCodeword( input, output, 20, LC_HEX_3 );
        	repairHexCodeword( input, output, 21, LC_HEX_2 );
        	repairHexCodeword( input, output, 22, LC_HEX_1 );
        	repairHexCodeword( input, output, 23, LC_HEX_0 );
        	
        	/* If the golay check failed, mark it as corrected too */
        	if( mCRC[ 1 ] == CRC.FAILED_CRC )
        	{
        		mCRC[ 1 ] = CRC.CORRECTED;
        	}
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
	
	/**
	 * Indicates if the message passed the golay and reed-solomon parity checks
	 * or was corrected.
	 */
	public boolean isValid()
	{
		// mCRC[ 0 ] - always passes (otherwise message wouldn't be constructed )
		// mCRC[ 1 ] - golay - always passes or corrected if mCRC[2] is valid
		
		return mCRC[ 2 ] == CRC.PASSED || mCRC[ 2 ] == CRC.CORRECTED;
	}
}
