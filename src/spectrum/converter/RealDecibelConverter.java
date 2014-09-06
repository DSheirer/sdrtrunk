package spectrum.converter;

public class RealDecibelConverter extends DFTResultsConverter
{
	/**
	 * Converts the output of the JTransforms FloatFFT_1D.realForward()
	 * calculation into a normalized power spectrum in decibels, per description
	 * in Lyons, Understanding Digital Signal Processing, 3e, page 141.
	 */
	@Override
    public void receive( float[] results )
    {
		float dc = Math.abs( results[ 0 ] );

		/* Ensure we have power in the DC power bin (bin 0), otherwise don't
		 * dispatch the results */
		if( dc != 0 )
		{
			/* Note: we're only processing 1/4 of the output samples */
			float[] processed = new float[ results.length / 4 + 1 ];
			
			for( int x = 0; x < processed.length - 2; x ++ )
			{
				int index = ( x + 1 ) * 2;

				double magnitude = ( ( results[ index ] * results[ index ] ) +
						   ( results[ index + 1 ] * results[ index + 1 ] ) );
				
				processed[ x ] = 10.0f * 
						(float)( Math.log10( magnitude / dc ) );
			}

			processed[ processed.length - 1 ] = processed[ 0 ];

			dispatch( processed );
		}
    }
}
