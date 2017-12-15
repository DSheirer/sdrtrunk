package ua.in.smartjava.edac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ua.in.smartjava.bits.BinaryMessage;

public class Hamming10
{
	private final static Logger mLog = LoggerFactory.getLogger( Hamming10.class );
	
	private static int[] CHECKSUMS = new int[] { 0xE,0xD,0xB,0X7,0x3,0xC };

	/**
	 * Performs error detection and correction of any single-bit errors.  This 
	 * is a truncated version of the Hamming15 class.
	 * 
	 * @param frame - binary frame containing a Hamming(10,6,3) protected field
	 * 
	 * @param startIndex - offset to the first bit of the field
	 * 
	 * @return - 0 = no errors 
	 * 			 1 = a single-bit error was corrected
	 */
	public static int checkAndCorrect( BinaryMessage frame, int startIndex )
	{
		int syndrome = getSyndrome( frame, startIndex );

		switch( syndrome )
		{
			case 0:
				return 0;
			case 1:
				frame.flip( startIndex + 9 ); //Parity 1
				return 1;
			case 2:
				frame.flip( startIndex + 8 ); //Parity 2
				return 1;
			case 3:
				frame.flip( startIndex + 4 ); //Data 2
				return 1;
			case 4:
				frame.flip( startIndex + 7 ); //Parity 4
				return 1;
			case 5:
				return 2;
			case 6:
				return 2;
			case 7:
				frame.flip( startIndex + 3 ); //Data 3
				return 1;
			case 8:
				frame.flip( startIndex + 6 ); //Parity 8
				return 1;
			case 9:
				return 2;
			case 10:
				return 2;
			case 11:
				frame.flip( startIndex + 2 ); //Data 4
				return 1;
			case 12:
				frame.flip( startIndex + 5 ); //Data 1
				return 1;
			case 13:
				frame.flip( startIndex + 1 ); //Data 5
				return 1;
			case 14:
				frame.flip( startIndex + 0 ); //Data 6
				return 1;
			case 15:
				return 2;
		}

		/* We'll never get to here */
		return 2;
	}

	/**
	 * Calculates the checksum (Parity 8,4,2,1) for data (6 <> 1 ) ua.in.smartjava.bits.
	 * @param frame - frame containing hamming(10) protected word
	 * @param startIndex - start bit index of the hamming protected word
	 * @return parity value, 0 - 15
	 */
	private static int calculateChecksum( BinaryMessage frame, int startIndex )
	{
		int calculated = 0; //Starting value

		/* Iterate the set ua.in.smartjava.bits and XOR running checksum with lookup value */
		for (int i = frame.nextSetBit( startIndex ); 
				 i >= startIndex && i < startIndex + 6; 
				 i = frame.nextSetBit( i+1 ) ) 
		{
			calculated ^= CHECKSUMS[ i - startIndex ];
		}
		
		return calculated;
	}

	/**
	 * Calculates the syndrome - xor of the calculated checksum and the actual
	 * checksum.
	 * 
	 * @param frame - binary frame containing a hamming(15,11,4) protected word
	 * @param startIndex - of bit 0 of the hamming protected word
	 * @return - 0 (no errors) or 1 (single bit error corrected)
	 */
	private static int getSyndrome( BinaryMessage frame, int startIndex )
	{
		int calculated = calculateChecksum( frame, startIndex );
		
		int checksum = frame.getInt( startIndex + 6, startIndex + 9 );

		return ( checksum ^ calculated );
	}
}
