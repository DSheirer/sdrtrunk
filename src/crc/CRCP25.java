/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package crc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bits.BitSetBuffer;

/**
 * P25 CRC check/correction methods
 */
public class CRCP25
{
	private final static Logger mLog = LoggerFactory.getLogger( CRCP25.class );
	
	private static final int NID_MESSAGE_START = 0;
	private static final int NID_CRC_START = 16;
	private static final int NID_CRC_LENGTH = 48;

	/**
	 * Network ID and Data Unit ID checksums
	 */
	private static long[] NID_DUID_CHECKSUMS = new long[] 
	{
		0xCD930BDD3B2Al, 0xAB5A8E33A6BEl, 0x983E4CC4E874l, 0x4C1F2662743Al,
		0xEB9C98EC0136l, 0xB85D47AB3BB0l, 0x5C2EA3D59DD8l, 0x2E1751EACEECl,
		0x170BA8F56776l, 0xC616DFA78890l, 0x630B6FD3C448l, 0x3185B7E9E224l,
		0x18C2DBF4F112l, 0xC1F2662743A2l, 0xAD6A38CE9AFBl, 0x9B2617BA7657l, 
		0x800000000000l, 0x400000000000l, 0x200000000000l, 0x100000000000l,
		0x080000000000l, 0x040000000000l, 0x020000000000l, 0x010000000000l,
		0x008000000000l, 0x004000000000l, 0x002000000000l, 0x001000000000l,
		0x000800000000l, 0x000400000000l, 0x000200000000l, 0x000100000000l,
		0x000080000000l, 0x000040000000l, 0x000020000000l, 0x000010000000l,
		0x000008000000l, 0x000004000000l, 0x000002000000l, 0x000001000000l,
		0x000000800000l, 0x000000400000l, 0x000000200000l, 0x000000100000l,
		0x000000080000l, 0x000000040000l, 0x000000020000l, 0x000000010000l,
		0x000000008000l, 0x000000004000l, 0x000000002000l, 0x000000001000l,
		0x000000000800l, 0x000000000400l, 0x000000000200l, 0x000000000100l,
		0x000000000080l, 0x000000000040l, 0x000000000020l, 0x000000000010l,
		0x000000000008l, 0x000000000004l, 0x000000000002l, 0x000000000001l
	};

	/**
	 * CRC-CCITT 16-bit checksums for a message length of 80 bits plus 16
	 * additional checksums representing CRC checksum bit errors
	 */
	public static final int[] CCITT_80_CHECKSUMS = new int[]
	{
	    0x1BCB, 0x8DE5, 0xC6F2, 0x6B69, 0xB5B4, 0x52CA, 0x2175, 0x90BA, 0x404D, 
	    0xA026, 0x5803, 0xAC01, 0xD600, 0x6310, 0x3998, 0x14DC, 0x27E, 0x92F, 
	    0x8497, 0xC24B, 0xE125, 0xF092, 0x7059, 0xB82C, 0x5406, 0x2213, 0x9109, 
	    0xC884, 0x6C52, 0x3E39, 0x9F1C, 0x479E, 0x2BDF, 0x95EF, 0xCAF7, 0xE57B, 
	    0xF2BD, 0xF95E, 0x74BF, 0xBA5F, 0xDD2F, 0xEE97, 0xF74B, 0xFBA5, 0xFDD2, 
	    0x76F9, 0xBB7C, 0x55AE, 0x22C7, 0x9163, 0xC8B1, 0xE458, 0x7A3C, 0x350E, 
	    0x1297, 0x894B, 0xC4A5, 0xE252, 0x7939, 0xBC9C, 0x565E, 0x233F, 0x919F, 
	    0xC8CF, 0xE467, 0xF233, 0xF919, 0xFC8C, 0x7656, 0x333B, 0x999D, 0xCCCE, 
	    0x6E77, 0xB73B, 0xDB9D, 0xEDCE, 0x7EF7, 0xBF7B, 0xDFBD, 0xEFDE, 0x0001, 
	    0x0002, 0x0004, 0x0008, 0x0010, 0x0020, 0x0040, 0x0080, 0x0100, 0x0200, 
	    0x0400, 0x0800, 0x1000, 0x2000, 0x4000, 0x8000
	};

	/**
	 * Performs CRC check and corrects single-bit errors against the message.
	 * @param message - bitsetbuffer containing a message of at least 64 bits.
	 * @return - results of CRC check and/or correction attempt
	 */
	public static CRC correctNID( BitSetBuffer message )
	{
		long calculated = 0; //Starting value

		/* Iterate the set bits and XOR running checksum with lookup value */
		for (int i = message.nextSetBit( NID_MESSAGE_START ); 
				 i >= NID_MESSAGE_START && i < NID_CRC_START; 
				 i = message.nextSetBit( i+1 ) ) 
		{
			calculated ^= NID_DUID_CHECKSUMS[ i - NID_MESSAGE_START ];
		}
		
		long checksum = getLongChecksum( message, NID_CRC_START, NID_CRC_LENGTH );

		if( calculated == checksum )
		{
			return CRC.PASSED;
		}
		else
		{
			int errorLocation = getBitError( calculated ^ checksum, 
											 NID_DUID_CHECKSUMS );
			
			if( errorLocation >= 0 )
			{
				message.flip( errorLocation + NID_MESSAGE_START );
				
				return CRC.CORRECTED;
			}
		}

		return CRC.FAILED_CRC;
	}

	/**
	 */
	public static CRC correctCCITT80( BitSetBuffer message, 
									  int messageStart,
									  int crcStart )
	{
		int calculated = 0; //Starting value

		/* Iterate the set bits and XOR running checksum with lookup value */
		for (int i = message.nextSetBit( messageStart ); 
				 i >= messageStart && i < crcStart; 
				 i = message.nextSetBit( i+1 ) ) 
		{
			calculated ^= CCITT_80_CHECKSUMS[ i - messageStart ];
		}
		
		int checksum = getIntChecksum( message, crcStart, 16 );

		int residual = calculated ^ checksum;
		
		if( residual == 0 || residual == 0xFFFF )
		{
			return CRC.PASSED;
		}
		else
		{
			int errorLocation = getBitError( residual, CCITT_80_CHECKSUMS );
			
			if( errorLocation >= 0 )
			{
				message.flip( errorLocation + messageStart );
				
				return CRC.CORRECTED;
			}
		}

		return CRC.FAILED_CRC;
	}

	
	/**
	 * Calculates the value of the message checksum as a long
	 */
    public static long getLongChecksum( BitSetBuffer message, 
    				int crcStart, int crcLength )
    {
    	int[] checksumIndexes = BitSetBuffer
    			.getFieldIndexes( crcStart, crcLength, false );

    	return message.getLong( checksumIndexes );
    }

	/**
	 * Calculates the value of the message checksum as an integer
	 */
    public static int getIntChecksum( BitSetBuffer message, 
    				int crcStart, int crcLength )
    {
    	int[] checksumIndexes = BitSetBuffer
    			.getFieldIndexes( crcStart, crcLength, false );
    	
    	return message.getInt( checksumIndexes );
    }

    /**
     * Identifies any single bit error position that matches the checksum error.
     */
    public static int getBitError( long checksumError, long[] checksums )
    {
		for( int x = 0; x < checksums.length; x++ )
		{
			if( checksums[ x ] == checksumError )
			{
				return x;
			}
		}

		return -1;
    }
    
    /**
     * Identifies any single bit error position that matches the checksum error.
     */
    public static int getBitError( int checksumError, int[] checksums )
    {
		for( int x = 0; x < checksums.length; x++ )
		{
			if( checksums[ x ] == checksumError )
			{
				return x;
			}
		}

		return -1;
    }
    
    public static void main( String[] args )
    {
    	String raw = "0010011000000111101010111111001111100001111010011110000100010010000010011001000000101101110000000000000000000000000000000000000000000000000000000001011011001100";
    	
    	BitSetBuffer message = BitSetBuffer.load( raw );
    	
    	mLog.debug( "MSG:" + message.toString() );

    	CRC results = correctCCITT80( message, 64, 144 );

    	mLog.debug( "COR:" + message.toString() );

    	mLog.debug( "Results: " + results.getDisplayText() );
    }
}
