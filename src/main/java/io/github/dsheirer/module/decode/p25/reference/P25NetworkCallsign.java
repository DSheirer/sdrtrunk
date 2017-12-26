package io.github.dsheirer.module.decode.p25.reference;

public class P25NetworkCallsign
{
	public static final String[] RADIX_50 =  new String[] 
			{ " ", "A", "B", "C", "D", "E", "F", "G", 
		      "H", "I", "J", "K", "L", "M", "N", "O", 
		      "P", "Q", "R", "S", "T", "U", "V", "W", 
		      "X", "Y", "Z", "$", ".", "?", "0", "1", 
		      "2", "3", "4", "5", "6", "7", "8", "9" };
	/**
	 * Decodes the string callsign from which the WACN and System identifiers
	 * were derived.
	 * 
	 * Based on code by Eric Carlson at:
	 * http://www.ericcarlson.net/project-25-callsign.html
	 * 
	 * @param wacn
	 * @param systemID
	 * @return
	 */
	public static String getCallsign( int wacn, int systemID )
	{
		int n1 = wacn / 16;
		int n2 = 4096 * ( wacn % 16 ) + systemID;

		StringBuilder sb = new StringBuilder();
		
		sb.append( getLetter( (int)( n1 / 1600 ) ) );
		sb.append( getLetter( (int)( ( n1 / 40 ) % 40 ) ) );
		sb.append( getLetter( (int)( n1 % 40 ) ) );
		sb.append( getLetter( (int)( n2 / 1600 ) ) );
		sb.append( getLetter( (int)( ( n2 / 40 ) % 40 ) ) );
		sb.append( getLetter( (int)( n2 % 40 ) ) );

		return sb.toString();
	}
	
	private static String getLetter( int value )
	{
		if( 0 <= value && value < 40 )
		{
			return RADIX_50[ value ];
		}
		
		return " ";
	}
}
