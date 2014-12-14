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

import java.util.BitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bits.BitSetBuffer;

/**
 * P25 Data Unit ID CRC check utility.  Corrects any single bit errors in 
 * either the message or the crc checksum.
 */
public class CRCP25NetworkID
{
	private final static Logger mLog = LoggerFactory.getLogger( CRCP25NetworkID.class );
	
	private static final int MESSAGE_START = 0;
	private static final int CRC_START = 16;
	private static final int CRC_END = 64;

	private static long[] CHECKSUMS = new long[] 
	{
		0xCD930BDD3B2Al, //NAC Bit 11 (MSB)
		0xAB5A8E33A6BEl, //NAC Bit 10
		0x983E4CC4E874l, //NAC Bit 9
		0x4C1F2662743Al, //NAC Bit 8
		0xEB9C98EC0136l, //NAC Bit 7
		0xB85D47AB3BB0l, //NAC Bit 6
		0x5C2EA3D59DD8l, //NAC Bit 5
		0x2E1751EACEECl, //NAC Bit 4
		0x170BA8F56776l, //NAC Bit 3
		0xC616DFA78890l, //NAC Bit 2
		0x630B6FD3C448l, //NAC Bit 1
		0x3185B7E9E224l, //NAC Bit 0 (LSB)
		0x18C2DBF4F112l, //DUID Bit 3 (MSB)
		0xC1F2662743A2l, //DUID Bit 2
		0xAD6A38CE9AFBl, //DUID Bit 1
		0x9B2617BA7657l, //DUID Bit 0 (LSB)
		
		//Single bit errors in the CRC Checksum
		0x800000000000l,
		0x400000000000l,
		0x200000000000l,
		0x100000000000l,
		0x080000000000l,
		0x040000000000l,
		0x020000000000l,
		0x010000000000l,
		0x008000000000l,
		0x004000000000l,
		0x002000000000l,
		0x001000000000l,
		0x000800000000l,
		0x000400000000l,
		0x000200000000l,
		0x000100000000l,
		0x000080000000l,
		0x000040000000l,
		0x000020000000l,
		0x000010000000l,
		0x000008000000l,
		0x000004000000l,
		0x000002000000l,
		0x000001000000l,
		0x000000800000l,
		0x000000400000l,
		0x000000200000l,
		0x000000100000l,
		0x000000080000l,
		0x000000040000l,
		0x000000020000l,
		0x000000010000l,
		0x000000008000l,
		0x000000004000l,
		0x000000002000l,
		0x000000001000l,
		0x000000000800l,
		0x000000000400l,
		0x000000000200l,
		0x000000000100l,
		0x000000000080l,
		0x000000000040l,
		0x000000000020l,
		0x000000000010l,
		0x000000000008l,
		0x000000000004l,
		0x000000000002l,
		0x000000000001l,
	};

	/**
	 * 
	 */
	public static CRC checkAndCorrect( BitSetBuffer message )
	{
		long calculated = 0; //Starting value

		/* Iterate the set bits and XOR running checksum with lookup value */
		for (int i = message.nextSetBit( MESSAGE_START ); 
				 i >= MESSAGE_START && i < CRC_START; 
				 i = message.nextSetBit( i+1 ) ) 
		{
			calculated ^= CHECKSUMS[ i - MESSAGE_START ];
		}

		long checksum = getChecksum( message );
		
		if( calculated == checksum )
		{
			return CRC.PASSED;
		}
		else
		{
			int errorLocation = findBitError( calculated ^ checksum );
			
			if( errorLocation >= 0 )
			{
				message.flip( errorLocation );
				
				return CRC.CORRECTED;
			}
		}

		return CRC.FAILED_CRC;
	}

	/**
	 * Returns the long value of the 48 bit crc checksum and parity bit
	 */
    public static long getChecksum( BitSet msg )
    {
    	long checksum = 0;
    	
    	for( int x = CRC_START; x < CRC_END; x++ )
    	{
    		if( msg.get( x ) )
    		{
    			checksum += Long.rotateLeft( 1, CRC_END - 1 - x );
    		}
    	}
    	
    	return checksum;
    }

    /**
     * Identifies any single bit error position that matches the checksum error.
     */
    public static int findBitError( long checksumError )
    {
    	/* One bit errors */
		for( int x = 0; x < 64; x++ )
		{
			if( CHECKSUMS[ x ] == checksumError )
			{
				return x;
			}
		}

		return -1;
    }
}
