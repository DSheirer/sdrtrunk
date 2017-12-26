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

import java.util.BitSet;

/**
 * Fleetsync CRC checksum utility
 * 
 * Fleetsync message blocks are 64 bits in length as follows:
 * 	  0 - 47: message bits
 *   48 - 62: CRC-15 check bits
 *        63: even parity bit
 *        
 * Fleetsync uses a CRC-15 (0x6815) with an initial fill of 0x0001, coupled with
 * an even parity checkbit.
 * 
 * CRC-15 Generating Polynomial: x15 + x14 + x13 + x11 + x4 + x2 + 1 (0x6815)
 */
public class CRCFleetsync
{
	private static short[] sCHECKSUMS = new short[] {
		0x740A, // 111010000001010 Bit 0
		0x3A05, // 011101000000101 Bit 1
		0x6908, // 110100100001000 Bit 2
		0x3484, // 011010010000100 Bit 3
		0x1A42, // 001101001000010 Bit 4
		0x0D21, // 000110100100001 Bit 5
		0x729A, // 111001010011010 Bit 6
		0x394D, // 011100101001101 Bit 7
		0x68AC, // 110100010101100 Bit 8
		0x3456, // 011010001010110 Bit 9
		0x1A2B, // 001101000101011 Bit 10
		0x791F, // 111100100011111 Bit 11
		0x4885, // 100100010000101 Bit 12
		0x5048, // 101000001001000 Bit 13
		0x2824, // 010100000100100 Bit 14
		0x1412, // 001010000010010 Bit 15
		0x0A09, // 000101000001001 Bit 16
		0x710E, // 111000100001110 Bit 17
		0x3887, // 011100010000111 Bit 18
		0x6849, // 110100001001001 Bit 19
		0x402E, // 100000000101110 Bit 20
		0x2017, // 010000000010111 Bit 21
		0x6401, // 110010000000001 Bit 22
		0x460A, // 100011000001010 Bit 23
		0x2305, // 010001100000101 Bit 24
		0x6588, // 110010110001000 Bit 25
		0x32C4, // 011001011000100 Bit 26
		0x1962, // 001100101100010 Bit 27
		0x0CB1, // 000110010110001 Bit 28
		0x7252, // 111001001010010 Bit 29
		0x3929, // 011100100101001 Bit 30
		0x689E, // 110100010011110 Bit 31
		0x344F, // 011010001001111 Bit 32
		0x6E2D, // 110111000101101 Bit 33
		0x431C, // 100001100011100 Bit 34
		0x218E, // 010000110001110 Bit 35
		0x10C7, // 001000011000111 Bit 36
		0x7C69, // 111110001101001 Bit 37
		0x4A3E, // 100101000111110 Bit 38
		0x251F, // 010010100011111 Bit 39
		0x6685, // 110011010000101 Bit 40
		0x4748, // 100011101001000 Bit 41
		0x23A4, // 010001110100100 Bit 42
		0x11D2, // 001000111010010 Bit 43
		0x08E9, // 000100011101001 Bit 44
		0x707E, // 111000001111110 Bit 45
		0x383F, // 011100000111111 Bit 46
		0x6815, // 110100000010101 Bit 47
		
		//Single bit errors in the CRC Checksum
		0x4000, //Bit 48
		0x2000, //Bit 49
		0x1000, //Bit 50
		0x0800, //Bit 51
		0x0400, //Bit 52
		0x0200, //Bit 53
		0x0100, //Bit 54
		0x0080, //Bit 55
		0x0040, //Bit 56
		0x0020, //Bit 57
		0x0010, //Bit 58
		0x0008, //Bit 59
		0x0004, //Bit 60
		0x0002, //Bit 61
		0x0001  //Bit 62
	};

	/**
	 * Determines if message bits 0 - 47 pass the Fleetsync CRC checksum
	 * contained in bits 48 - 63, using a lookup table of CRC checksum values
	 * derived from the CRC-15 value, and verifies the message has even parity
	 */
	public static CRC check( BitSet msg )
	{
		CRC crc = CRC.UNKNOWN;
		
		int calculated = 1; //Starting value

		//Check even parity
		if( msg.cardinality() % 2 == 0 )
		{
			//Iterate bits that are set and XOR running checksum with lookup value
			for (int i = msg.nextSetBit( 0 ); i >= 0 && i < 48; i = msg.nextSetBit( i+1 ) ) 
			{
				calculated ^= sCHECKSUMS[ i ];
			}
			
			if( calculated == getChecksum( msg ) )
			{
				crc = CRC.PASSED;
			}
			else
			{
				crc = CRC.FAILED_CRC;
			}
		}
		else
		{
			crc = CRC.FAILED_PARITY;
		}
		
		return crc;
	}

	/**
	 * Returns the integer value of the 15 bit crc checksum
	 */
    public static int getChecksum( BitSet msg )
    {
    	int retVal = 0;
    	
    	for( int x = 0; x < 15; x++ )
    	{
    		if( msg.get( x + 48 ) )
    		{
    			retVal += 1<<( 14 - x );
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
    	
    	int checksum = getChecksum( msg );
    	
		//Remove the initial fill value (1)
		checksum ^= 1;
		
		//Iterate set message bits, removing their respective checksum value
    	//from the transmitted checksum, to arrive at the remainder
		for (int i = msg.nextSetBit( 0 ); i >= 0 && i < 48; i = msg.nextSetBit( i+1 ) ) 
		{
			checksum ^= sCHECKSUMS[ i ];
		}
		
		//If at this point the checksum is 0, then we have a parity bit error
		if( checksum == 0 )
		{
			retVal = new int[ 1 ];
			retVal[ 0 ] = 63;
		}
		//Otherwise, try to lookup the syndrome for a single bit error
		else
		{
			for( int x = 0; x < 63; x++ )
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
