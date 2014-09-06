package spectrum.converter;

public class ComplexDecibelConverter extends DFTResultsConverter
{
	/**
	 * Converts the output of the JTransforms FloatFFT_1D.complexForward()
	 * calculation into a normalized power spectrum in decibels, per description
	 * in Lyons, Understanding Digital Signal Processing, 3e, page 141.
	 */
	@Override
    public void receive( float[] results )
    {
		float dc = Math.abs( results[ 0 ] );
		
		/* Ensure we have power in the DC power bin (bin 0), otherwise don't
		 * dispatch the results */
		if( dc > 0 )
		{
			float[] processed = new float[ results.length / 2 + 1 ];

			/**
			 * Output from JTransforms 3.0 has the lower and upper halves of the
			 * spectrum swapped.
			 */
			int half = processed.length / 2;
			
			for( int x = 0; x < processed.length - 2; x ++ )
			{
				int index = ( x + 1 ) * 2;

				double magnitude2 = ( ( results[ index ] * results[ index ] ) +
						   ( results[ index + 1 ] * results[ index + 1 ] ) );
				
				if( x < half )
				{
					processed[ x + half ] = 10.0f * 
							(float)( Math.log10( magnitude2 / dc ) );
				}
				else
				{
					processed[ x - half ] = 10.0f * 
							(float)( Math.log10( magnitude2 / dc ) );
				}
			}
			
			/**
			 * Place scaled dc component in last bin of processed array as 
			 * scaling reference
			 */
			processed[ results.length / 2 ] = processed[ half ];
			
			/**
			 * Repair the erroneous DC component bin by using the average of 
			 * the neighboring bins 
			 */
			processed[ half ] = ( processed[ half - 1 ] + 
								  processed[ half + 1 ] / 2.0f );
			
			dispatch( processed );
		}
    }
}
