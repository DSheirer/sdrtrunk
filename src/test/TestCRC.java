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
package test;

import java.util.BitSet;

import crc.CRCLTR;


public class TestCRC
{
	/**
	 * @param args
	 */
	public static void main( String[] args )
	{
		log( "Starting ... " );
		
//		String a = "10101100001001110011111111111000010111100";
//		String b = "10101100000110100011010000000011000101101";
//		String c = "10101100001001110011111111111001010110001";
//		String d = "10101100000100101100000000000110011001001";
//		String e = "10101100000110000000110000101100000011010";

		String a = "00111101000011111111111001110010000110101";
		String b = "10110100011000000001011000101100000110101";
		String c = "10001101010011111111111001110010000110101";
		String d = "10010011001100000000000110100100000110101";
		String e = "01011000000110100001100000001100000110101";

		for( int poly = 0; poly < 64; poly++ )
		{
			BitSet m1 = load( a );
			int residual1 = applyChecksum( m1, poly );

			BitSet m2 = load( b );
			int residual2 = applyChecksum( m2, poly );
			
			BitSet m3 = load( c );
			int residual3 = applyChecksum( m3, poly );

			BitSet m4 = load( d );
			int residual4 = applyChecksum( m4, poly );

			BitSet m5 = load( e );
			int residual5 = applyChecksum( m5, poly );

			log( "Poly:" + poly + ">> M1:" + residual1 + 
								  " M2:" + residual2 +
								  " M3:" + residual3 +
								  " M4:" + residual4 +
								  " M5:" + residual5 );
		}

		log( "Done!" );
	}

	private static void getChecksum( int bit )
	{
		BitSet m = getInitialFill();
		m.set( bit );
		
		int psn = bit;
		
		while( psn < 34 )
		{
//			log( "Before:" + format( m, 41 ) );

//			BitSet poly = getPoly( psn );
//			log( "  Poly:" + format( poly, 41 ) + "\n" );

//			m.xor( poly );

			psn = m.nextSetBit( psn + 1 );
		}

		log( "Result:" + format( m, 41 ) + " Bit:" + bit + " Hex:" + getHex( m.get( 34, 41 ), 7) );
	}

	//new
//	private static boolean test( BitSet m, int poly, int initialFill )
//	{
//		
//	}

	//New
	private static int applyChecksum( BitSet m, int poly )
	{
		int psn = m.nextSetBit( 0 );
		
		while( psn != -1 && psn < 35 )
		{
//			log( "Before:" + format( m, 41 ) );

			BitSet mask = getPoly( poly, psn );
//			log( "  Poly:" + format( mask, 41 ) + "\n" );

			m.xor( mask );

			psn = m.nextSetBit( psn + 1 );
		}
		
//		log( "Result:" + format( m, 41 ) + " Hex:" + getHex( m.get( 34, 41 ), 7) );
		
		return getInt( m.get( 34, 41 ), 7 );
	}

	//new
	private static BitSet getInitialFill( int value )
	{
		BitSet b = new BitSet();
		
		for( int x = 0; x < 7; x++ )
		{
			if( ( ( 1<<( 6 - x ) ) & value ) > 0 )
			{
				b.set( 34 + x );
			}
		}
		
		return b;
	}

	//new
	private static BitSet getPoly( int poly, int startPsn )
	{
		//Load poly 1xxx xxx1 into new BitSet starting at startPsn
		BitSet b = new BitSet();
		b.set( startPsn );
		b.set( startPsn + 7 );                    //changed
		
		for( int x = 0; x < 6; x++ )              //changed
		{
			if( ( ( 1<<( 5 - x ) ) & poly ) > 0 ) //changed
			{
				b.set( startPsn + x + 1 );
			}
		}
		return b;
	}

	private static BitSet getInitialFill()
	{
		//Loads poly 1100 0011 into new BitSet starting at startPsn
		BitSet b = new BitSet();

		b.set( 39 );
		b.set( 34 );
		
		return b;
	}
	
	private static String getChars( String c, int count )
	{
		StringBuilder sb = new StringBuilder();

		for( int x = 0; x < count; x++ )
		{
			sb.append( c );
		}
		
		return sb.toString();
	}
	
	private static void applyFlips( BitSet m, BitSet c, int[] psns )
	{
		for( int x = 0; x < psns.length; x++ )
		{
			flip( m, c, psns[ x ] );
		}
	}
	
	private static BitSet diff( BitSet b1, BitSet b2 )
	{
		b1.xor(  b2 );
		
		return b1;
	}
	
	private static void flip( BitSet m, BitSet c, int psn )
	{
		m.flip( psn );
		apply( c, CRCLTR.getChecks()[ psn ] );
	}
	
	private static void apply( BitSet c, byte mask )
	{
		BitSet maskBits = load( mask, true );
		
		c.xor( maskBits );
	}
	
	private static BitSet load( byte b, boolean fullByte )
	{
		BitSet retVal = new BitSet();

		if( fullByte )
		{
			for( int x = 0; x < 8; x++ )
			{
				if( ( b & ( 1 << x ) ) > 0 )
				{
					retVal.set( 7 - x );
				}
			}
		}
		else
		{
			for( int x = 0; x < 7; x++ )
			{
				if( ( b & ( 1 << x ) ) > 0 )
				{
					retVal.set( 6 - x );
				}
			}
		}

		return retVal;
	}
	
	private static void logDiff( BitSet m1, BitSet m2, BitSet c1, BitSet c2  )
	{
		log( "M1:" + format( m1, 24 ) + " Cr1:" + format( c1, 7 ) + " Hex:" + getHex( c1, 7 ) );
		log( "M2:" + format( m2, 24 ) + " Cr2:" + format( c2, 7 ) + " Hex:" + getHex( c2, 7 ) );

		m1.xor( m2 );
		c1.xor( c2 );
		
		log( "DI:" + format( m1, 24 ) + " Crc:" + format( c1, 7 ) + " Hex:" + getHex( c1, 7 ) );
	}
	
	private static String getHex( BitSet b, int length )
	{
		return String.format( "%02X", getInt( b, length ) );
	}
	
    private static int getInt( BitSet b, int length )
    {
    	int retVal = 0;
    	
    	for( int x = 0; x < length; x++ )
    	{
    		if( b.get( x ) )
    		{
    			retVal += 1<<( length - 1 - x );
    		}
    	}
    	
    	return retVal;
    }


	private static BitSet load( String bits )
	{
		BitSet retVal = new BitSet();
		
		for( int x = 0; x < bits.length() - 9; x++ )  //changed** -1
		{
			if( bits.substring( x, x + 1 ).contentEquals( "1" ) )
			{
				retVal.set( x );
			}
		}
		
		return retVal;
	}

	private static String format( BitSet bitset, int length )
	{
		StringBuilder sb = new StringBuilder();
		for( int x = 0; x < length; x++ )
		{
			if( bitset.get( x ) )
			{
				sb.append( "1" );
			}
			else
			{
				sb.append( "0" );
			}
		}
		return sb.toString();
	}

	private static void log( BitSet m, BitSet c, String header )
	{
		log( header + ":" + format( m, 24 ) + " Cr1:" + format( c, 7 ) + " Hex:" + getHex( c, 7 ) );
	}
	
	private static void log( String message )
	{
		System.out.println( message );
	}
}
