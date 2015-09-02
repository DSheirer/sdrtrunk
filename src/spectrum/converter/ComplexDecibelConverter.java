package spectrum.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComplexDecibelConverter extends DFTResultsConverter
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( DFTResultsConverter.class );

	/**
	 * Converts the output of the JTransforms FloatFFT_1D.complexForward()
	 * calculation into a normalized power spectrum in decibels, per description
	 * in Lyons, Understanding Digital Signal Processing, 3e, page 141.
	 */
	@Override
    public void receive( float[] results )
    {
		float[] processed = new float[ results.length / 2 ];

		/* Output from JTransforms 3.0 has the lower and upper halves of the
		 * spectrum swapped. */
		int half = processed.length / 2;
		
		for( int x = 0; x < results.length; x += 2 )
		{
			float magnitude = (float)Math.sqrt( results[ x ] * 
					results[ x ] + results[ x + 1 ] * results[ x + 1 ] );
			
			/* Place upper half of magnitudes in lower half of results, vice-versa */
			if( x / 2 >= half )
			{
				processed[ x / 2 - half ] = magnitude;
			}
			else
			{
				processed[ x / 2 + half ] = magnitude;
			}
		}

		/* Use the DC component as the largest magnitude value to keep the 
		 * display from jumping when a strong bursting signal exists */
		float largestMagnitude = processed[ half ];
		
		if( largestMagnitude > 0 )
		{
			/* Convert magnitudes to normized power spectrum dBm */
			for( int i = 0; i < processed.length; i++ )
			{
				processed[ i ] = 20.0f * 
						(float)Math.log10( processed[ i ] / largestMagnitude );
			}
			
			dispatch( processed );
		}
    }
}
