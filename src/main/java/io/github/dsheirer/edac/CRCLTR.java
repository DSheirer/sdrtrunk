/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */
package io.github.dsheirer.edac;

import io.github.dsheirer.message.MessageDirection;
import java.util.BitSet;

/**
 * LTR CRC checksum utility
 * 
 * LTR message blocks are 41 bits in length as follows:
 *    0 -  9: Sync
 * 	 10 - 34: message bits
 *   35 - 41: CRC-7 check bits
 *        
 * LTR uses a CRC-7 (0xFD) with an initial fill of 0x00.
 * 
 * CRC-7 Generating Polynomial: x7 + x6 + x5 + x4 + x3 + x2 + 1 (0xFD)
 */
public class CRCLTR
{
	private static byte[] sCHECKSUMS = new byte[]
	{	
		0x38, //Area
		0x1C, //Channel 4
		0x0E, //Channel 3
		0x46, //Channel 2
		0x23, //Channel 1
		0x51, //Channel 0
		0x68, //Home 4
		0x75, //Home 3
		0x7A, //Home 2
		0x3D, //Home 1
		0x1F, //Home 0
		0x4F, //Group 7
		0x26, //Group 6
		0x52, //Group 5
		0x29, //Group 4
		0x15, //Group 3
		0x0B, //Group 2
		0x45, //Group 1
		0x62, //Group 0
		0x31, //Free 4
		0x19, //Free 3
		0x0D, //Free 2
		0x07, //Free 1
		0x43  //Free 0 
	};

	/**
	 * Determines if message bits 10 - 34 pass the LTR CRC checksum
	 * contained in bits 35 - 41, using a lookup table of CRC checksum values
	 * derived from the CRC-7 value.
	 */
	public static CRC check( BitSet msg, MessageDirection direction )
	{
		CRC crc = CRC.UNKNOWN;
		
		int calculated = getCalculatedChecksum( msg );
		int transmitted = getTransmittedChecksum( msg );

		/* Normal CRC only for Outbound Status Word */
		if( direction == MessageDirection.OSW )
		{
			if( calculated == transmitted )
			{
				return CRC.PASSED;
			}
		}
		/* Normal or Inverted CRC for Inbound Status Word */
		else
		{
			if( ( calculated ^ 127 ) == transmitted )
			{
				return CRC.PASSED_INV;
			}
			else if( calculated == transmitted )
			{
				return CRC.PASSED;
			}
		}

		return CRC.FAILED_CRC;
	}

	public static String getCRCReason(BitSet msg, MessageDirection direction)
	{
		int calculated = getCalculatedChecksum( msg );
		int transmitted = getTransmittedChecksum( msg );
		return "CALC: " + Integer.toHexString(calculated).toUpperCase() +
				" TRANS: " + Integer.toHexString(transmitted).toUpperCase();
	}
	
	public static byte[] getChecks()
	{
		return sCHECKSUMS;
	}

	public static int getCalculatedChecksum( BitSet msg  )
	{
		int calculated = 0;
		
		//Iterate bits that are set and XOR running checksum with lookup value
		for (int i = msg.nextSetBit( 9 ); i >= 9 && i < 33; i = msg.nextSetBit( i+1 ) ) 
		{
			calculated ^= sCHECKSUMS[ i - 9 ];
		}
		
		return calculated;
	}
	
	/**
	 * Returns the integer value of the 7 bit crc checksum
	 */
    public static int getTransmittedChecksum( BitSet msg )
    {
    	int retVal = 0;
    	
    	for( int x = 0; x < 7; x++ )
    	{
    		if( msg.get( x + 33 ) )
    		{
    			retVal += 1<<( 6 - x );
    		}
    	}
    	
    	return retVal;
    }

    /**
     * Determines the errant bit positions and returns them in an array of 
     * integer bit positions.
     * 
     * Note: currently only detects single-bit errors
     * 
     * @param msg to be checked for errors
     * @return - array of integer positions of bits that need flipped
     */
    public static int[] findBitErrors( BitSet msg )
    {
    	int[] retVal = null;
    	
    	int checksum = getTransmittedChecksum( msg );
    	
		//Remove the initial fill value (1)
		checksum ^= 1;
		
		//Iterate set message bits, removing their respective checksum value
    	//from the transmitted checksum, to arrive at the remainder
		for (int i = msg.nextSetBit( 0 ); i >= 0 && i < 47; i = msg.nextSetBit( i+1 ) ) 
		{
			checksum ^= sCHECKSUMS[ i ];
		}
		
		//If at this point the checksum is 0, then the errant bit is the parity
		//bit
		if( checksum == 0 )
		{
			msg.flip( 62 );
		}
		//Otherwise, try to lookup the syndrome for a single bit error
		else
		{
			for( int x = 0; x < 47; x++ )
			{
				if( checksum == sCHECKSUMS[ x ] )
				{
					//return this bit position
					retVal = new int[ 1 ];
					retVal[ 0 ] = x;
				}
			}
		}

		return retVal;
    }
}
