package decode.p25;

import java.util.BitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bits.BitSetBuffer;

/**
 * Utility class to deinterleave P25 packet blocks of 196-bit length.
 */
public class P25Interleave
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( P25Interleave.class );

	private static int[] MATRIX = new int[] { 0,1,2,3,16,17,18,19,32,33,34,35,
			48,49,50,51,64,65,66,67,80,81,82,83,96,97,98,99,112,113,114,115,
			128,129,130,131,144,145,146,147,160,161,162,163,176,177,178,179,
			192,193,194,195,4,5,6,7,20,21,22,23,36,37,38,39,52,53,54,55,68,
			69,70,71,84,85,86,87,100,101,102,103,116,117,118,119,132,133,
			134,135,148,149,150,151,164,165,166,167,180,181,182,183,8,9,10,
			11,24,25,26,27,40,41,42,43,56,57,58,59,72,73,74,75,88,89,90,91,
			104,105,106,107,120,121,122,123,136,137,138,139,152,153,154,155,
			168,169,170,171,184,185,186,187,12,13,14,15,28,29,30,31,44,45,
			46,47,60,61,62,63,76,77,78,79,92,93,94,95,108,109,110,111,124,
			125,126,127,140,141,142,143,156,157,158,159,172,173,174,175,188,
			189,190,191 };

	/**
	 * Deinterleaves the 196-bit block in message, identified by start and end
	 * bit positions.  Note: end index (exclusive) should be one more than the 
	 * last bit in the block.
	 * 
	 * @param message - source message to deinterleave
	 * @param start - starting bit index for the block
	 * @param end - ending bit index for the block, plus 1
	 */
	public static void deinterleave( BitSetBuffer message, int start, int end )
	{
		BitSet original = message.get( start, end );

		/* Clear block bits in source message */
		message.clear( start, end );

		/* Iterate only the set bits in the original message and apply 
		 * the deinterleave -- we don't have to evaluate the 0 bits */
		for (int i = original.nextSetBit( 0 ); 
				 i >= 0 && i < MATRIX.length; 
				 i = message.nextSetBit( i + 1 ) ) 
		{
			message.set( start + MATRIX[ i ] );
		}
	}
}
