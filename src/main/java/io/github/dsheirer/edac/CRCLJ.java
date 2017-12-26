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
package io.github.dsheirer.edac;

import io.github.dsheirer.bits.BinaryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.BitSet;

/**
 * LJ CRC checksum utility
 * 
 * LJ message blocks are 80 bits in length as follows:
 *    0 -  7: Bit reversals
 *    8 - 15: Sync pattern
 *   16 - 23: VRC
 *   24 - 31: LRC
 *   32 - 35: Function Code
 *   36 - 63: Address
 *   64 - 79: CRC
 *   
 * Each field is transmitted in big-endian bit order, including the CRC.  Only
 * the Function and Address fields are protected by the CRC.
 *        
 * LJ uses a CRC-16 (0x6F63) with an initial fill of 0.
 * 
 * Generating Polynomial: x14 + x13 + x11 + x10 + x9 + x8 + x6 + x5 + x1 + 1 (0x6F63)
 */
public class CRCLJ
{
	private final static Logger mLog = LoggerFactory.getLogger( CRCLJ.class );
	
	private static final int MESSAGE_START = 32;
	private static final int CRC_START = 64;

	private static int[] CHECKSUMS = new int[] 
	{
		0x26EA, //Function 0 (LSB)
		0x1375, //Function 1
		0xBE0B, //Function 2
		0xE8B4, //Function 3 (MSB)
		0x745A, //Address 0 (LSB)
		0x3A2D, //Address 1
		0xAAA7, //Address 2
		0xE2E2, //Address 3
		0x7171, //Address 4
		0x8F09, //Address 5
		0xF035, //Address 6
		0xCFAB, //Address 7
		0xD064, //Address 8
		0x6832, //Address 9
		0x3419, //Address 10
		0xADBD, //Address 11
		0xE16F, //Address 12
		0xC706, //Address 13
		0x6383, //Address 14
		0x8670, //Address 15
		0x4338, //Address 16
		0x219C, //Address 17
		0x10CE, //Address 18
		0x0867, //Address 19
		0xB382, //Address 20
		0x59C1, //Address 21
		0x9B51, //Address 22
		0xFA19, //Address 23
		0xCABD, //Address 24
		0xD2EF, //Address 25
		0xDEC6, //Address 26
		0x6F63, //Address 27 (MSB)
		
		//Single bit errors in the CRC Checksum
		0x8000, //CRC 0
		0x4000, //CRC 1
		0x2000, //CRC 2
		0x1000, //CRC 3
		0x0800, //CRC 4
		0x0400, //CRC 5
		0x0200, //CRC 6
		0x0100, //CRC 7
		0x0080, //CRC 8
		0x0040, //CRC 9
		0x0020, //CRC 10
		0x0010, //CRC 11
		0x0008, //CRC 12
		0x0004, //CRC 13
		0x0002, //CRC 14
		0x0001  //CRC 15
	};

	/**
	 * Determines if FUNCTION AND ADDRESS bits pass the LJ CRC checksum
	 * using a lookup table of CRC checksum values derived from the CRC-16 value
	 */
	public static CRC checkAndCorrect( BinaryMessage message )
	{
		int calculated = 0; //Starting value

		/* Iterate the set bits and XOR running checksum with lookup value */
		for (int i = message.nextSetBit( MESSAGE_START ); 
				 i >= MESSAGE_START && i < CRC_START; 
				 i = message.nextSetBit( i+1 ) ) 
		{
			calculated ^= CHECKSUMS[ i - MESSAGE_START ];
		}

		int checksum = getChecksum( message );
		
		if( calculated == checksum )
		{
			return CRC.PASSED;
		}
		else
		{
			int[] errors = findBitErrors( calculated ^ checksum );
			
			if( errors != null )
			{
				for( int error: errors )
				{
					message.flip( MESSAGE_START + error );
				}
				
				return CRC.CORRECTED;
			}
		}

		return CRC.FAILED_CRC;
	}

	/**
	 * Returns the integer value of the 16 bit crc checksum
	 */
    public static int getChecksum( BitSet msg )
    {
    	int retVal = 0;
    	
    	for( int x = 0; x < 16; x++ )
    	{
    		if( msg.get( x + CRC_START ) )
    		{
    			retVal += 1<<( 15 - x );
    		}
    	}
    	
    	return retVal;
    }

    /**
     * Identifies 1 bit error positions that match the checksum error value.
     */
    public static int[] findBitErrors( int checksumError )
    {
    	/* One bit errors */
		for( int x = 0; x < 48; x++ )
		{
			if( CHECKSUMS[ x ] == checksumError )
			{
				int[] errors = new int[ 1 ];

				errors[ 0 ] = x;
				
				return errors;
			}
		}

		return null;
    }
    
    public static void main( String[] args )
    {
    	BinaryMessage msg = BinaryMessage.load( "01010101101010101010000100011000000000100001000110000000001000010001100000000010" );
    	
    	CRC crc = checkAndCorrect( msg );
    	
    	mLog.debug( "CRC:" + crc.getDisplayText() );
    }
}
