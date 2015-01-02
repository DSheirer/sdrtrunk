package decode.p25;

import java.util.BitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bits.BitSetBuffer;

/**
 * Utility class to process interleave of P25 Voice and Data messages.
 */
public class P25Interleave
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( P25Interleave.class );

    private static int[] DATA_INTERLEAVE = new int[] { 0,1,2,3,52,53,54,55,100,
    	101,102,103,148,149,150,151,4,5,6,7,56,57,58,59,104,105,106,107,152,153,
    	154,155,8,9,10,11,60,61,62,63,108,109,110,111,156,157,158,159,12,13,14,
    	15,64,65,66,67,112,113,114,115,160,161,162,163,16,17,18,19,68,69,70,71,
    	116,117,118,119,164,165,166,167,20,21,22,23,72,73,74,75,120,121,122,123,
    	168,169,170,171,24,25,26,27,76,77,78,79,124,125,126,127,172,173,174,175,
    	28,29,30,31,80,81,82,83,128,129,130,131,176,177,178,179,32,33,34,35,84,
    	85,86,87,132,133,134,135,180,181,182,183,36,37,38,39,88,89,90,91,136,
    	137,138,139,184,185,186,187,40,41,42,43,92,93,94,95,140,141,142,143,188,
    	189,190,191,44,45,46,47,96,97,98,99,144,145,146,147,192,193,194,195,48,
    	49,50,51 };

	private static int[] DATA_DEINTERLEAVE = new int[] { 0,1,2,3,16,17,18,19,32,
		33,34,35,48,49,50,51,64,65,66,67,80,81,82,83,96,97,98,99,112,113,114,115,
		128,129,130,131,144,145,146,147,160,161,162,163,176,177,178,179,192,193,
		194,195,4,5,6,7,20,21,22,23,36,37,38,39,52,53,54,55,68,69,70,71,84,85,
		86,87,100,101,102,103,116,117,118,119,132,133,134,135,148,149,150,151,
		164,165,166,167,180,181,182,183,8,9,10,11,24,25,26,27,40,41,42,43,56,57,
		58,59,72,73,74,75,88,89,90,91,104,105,106,107,120,121,122,123,136,137,
		138,139,152,153,154,155,168,169,170,171,184,185,186,187,12,13,14,15,28,
		29,30,31,44,45,46,47,60,61,62,63,76,77,78,79,92,93,94,95,108,109,110,
		111,124,125,126,127,140,141,142,143,156,157,158,159,172,173,174,175,188,
		189,190,191 };
	
	private static int[] VOICE_DEINTERLEAVE = new int[] { 0,24,48,72,96,120,25,
		1,73,49,121,97,2,26,50,74,98,122,27,3,75,51,123,99,4,28,52,76,100,124,
		29,5,77,53,125,101,6,30,54,78,102,126,31,7,79,55,127,103,8,32,56,80,104,
		128,33,9,81,57,129,105,10,34,58,82,106,130,35,11,83,59,131,107,12,36,60,
		84,108,132,37,13,85,61,133,109,14,38,62,86,110,134,39,87,15,63,135,111,
		16,40,64,88,112,136,41,17,89,65,137,113,18,42,66,90,114,138,43,19,91,
		67,139,115,20,44,68,92,116,140,45,21,93,69,141,117,22,46,70,94,119,142,
		47,23,95,71,143,118 };

	private static int[] VOICE_INTERLEAVE = new int[] { 0,7,12,19,24,31,36,43,
		48,55,60,67,72,79,84,91,96,103,108,115,120,127,132,139,1,6,13,18,25,30,
		37,42,49,54,61,66,73,78,85,90,97,102,109,114,121,126,133,138,2,9,14,21,
		26,33,38,45,50,57,62,69,74,81,86,93,98,105,110,117,122,129,134,141,3,8,
		15,20,27,32,39,44,51,56,63,68,75,80,87,92,99,104,111,116,123,128,135,
		140,4,11,16,23,28,35,40,47,52,59,64,71,76,83,88,95,100,107,112,119,124,
		131,143,136,5,10,17,22,29,34,41,46,53,58,65,70,77,82,89,94,101,106,113,
		118,125,130,137,142 };
	/**
	 * Deinterleaves the 196-bit block in message, identified by start and end
	 * bit positions.  Note: end index (exclusive) should be one more than the 
	 * last bit in the block.
	 * 
	 * @param message - source message to deinterleave
	 * @param start - starting bit index for the block
	 * @param end - ending bit index for the block, plus 1
	 */
	public static BitSetBuffer deinterleaveData( BitSetBuffer message, 
			int start, int end )
	{
		return deinterleave( DATA_DEINTERLEAVE, message, start, end );
	}
	
	public static BitSetBuffer deinterleaveVoice( BitSetBuffer message, 
			int start, int end )
	{
		return deinterleave( VOICE_DEINTERLEAVE, message, start, end );
	}
	
	public static BitSetBuffer deinterleave( int[] pattern, BitSetBuffer message, 
			int start, int end )
	{
		BitSet original = message.get( start, end );

		/* Clear block bits in source message */
		message.clear( start, end );

		/* Iterate only the set bits in the original message and apply 
		 * the deinterleave -- we don't have to evaluate the 0 bits */
		for (int i = original.nextSetBit( 0 ); 
				 i >= 0 && i < pattern.length; 
				 i = original.nextSetBit( i + 1 ) ) 
		{
			message.set( start + pattern[ i ] );
		}
		
		return message;
	}

    /**
     * Interleaves the 196-bit block in message, identified by start and end
     * bit positions.  Note: end index (exclusive) should be one more than the 
     * last bit in the block.
     * 
     * @param message - source message to interleave
     * @param start - starting bit index for the block
     * @param end - ending bit index for the block, plus 1
     */
    public static BitSetBuffer interleaveData( BitSetBuffer message, 
    		int start, int end )
    {
    	return interleave( DATA_INTERLEAVE, message, start, end );
    }
    
    public static BitSetBuffer interleaveVoice( BitSetBuffer message, 
    		int start, int end )
    {
    	return interleave( VOICE_INTERLEAVE, message, start, end );
    }
    
    public static BitSetBuffer interleave( int[] pattern, BitSetBuffer message, 
    		int start, int end )
    {
        BitSet original = message.get( start, end );
        
       /* Clear block bits in source message */
        message.clear( start, end );

        /* Iterate only the set bits in the original message and apply 
         * the deinterleave -- we don't have to evaluate the 0 bits */
        for (int i = original.nextSetBit( 0 ); 
                 i >= 0 && i < pattern.length; 
                 i = original.nextSetBit( i + 1 ) ) 
        {
            message.set( start + pattern[ i ] );
        }
        
        return message;
    }

    public static void main( String[] args )
    {
    	TrellisHalfRate halfrate = new TrellisHalfRate();
    	
    	int start = 0;
    	int end = 196;
    	String interleaved = "1000100100100011001000100010110010001000100010010000100010011100100101001000001000100010001011000110010001111011110010001000100010001000100010000011011100101011001000101000100010001000100010110101";

    	BitSetBuffer b = new BitSetBuffer( end );
    	
    	for( int x = 0; x < end; x++ )
    	{
    		if( interleaved.substring( x, x + 1 ).contentEquals( "1" ) )
    		{
    			b.set( x );
    		}
    	}
    	
    	mLog.debug( "Interleaved:" + interleaved );
    	mLog.debug( "     Buffer:" + b.toString() );

    	P25Interleave.deinterleaveData( b, start, end );
    		
    	mLog.debug( "Deinterleav:" + b.toString() );

    	halfrate.decode( b, start, end );
    	
    	mLog.debug( "Trellis Off:" + b.toString() );
    }
}
