package edac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bits.BitSetBuffer;

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

public class BCH_63_16_11 extends BerlekempMassey_63
{
	private final static Logger mLog = LoggerFactory.getLogger( BCH_63_16_11.class );

	/**
	 * BCH( 63,16,11) decoder
	 */
	public BCH_63_16_11()
    {
		/* TT = 11 = maximum correctable bit errors */
	    super( 11 );
    }
	
	/**
	 * Performs error detection and correction on the first 63 bits of the 
	 * message argument.  If the message is correctable, only the first 16 bits
	 * (information bits) are corrected.
	 * 
	 * @return - true = success, false = failure
	 */
	public CRC correctNID( BitSetBuffer message )
	{
		CRC status = CRC.PASSED;
		
		int[] original = message.toReverseIntegerArray();
		int[] corrected = new int[ 63 ];

		boolean irrecoverableErrors = decode( original, corrected );

		if( irrecoverableErrors )
		{
			return CRC.FAILED_CRC;
		}
		else
		{
			for( int x = 0; x < 16; x++ )
			{
				int index = 63 - x - 1;
				
				if( corrected[ index ] != original[ index ] )
				{
					status = CRC.CORRECTED;
					
					if( corrected[ index ] == 1 )
					{
						message.set( x );
					}
					else
					{
						message.clear( x );
					}
				}
			}
		}
		
		return status;
	}
	
	public static void main( String[] args )
	{
		String orig  = "001001100000110010100101011111010001010101101001111111001010110";
		String error = "010110000000110010100101011011010001011101101001101111001110110";
//                       xxxxxx                    x          x          x       x
		BitSetBuffer errorMessage = BitSetBuffer.load( error );

		BCH_63_16_11 bch = new BCH_63_16_11();

		mLog.debug( "ORIG:" + orig );
		mLog.debug( " ERR:" + errorMessage.toString() );
		mLog.debug( " " );

		CRC status = bch.correctNID( errorMessage );
		
		mLog.debug( " CRC: " + status.name() );
		mLog.debug( "CORR:" + errorMessage.toString().substring( 0, 16 ) );
		mLog.debug( "ORIG:" + orig );
	}
}
