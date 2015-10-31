package spectrum.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealDecibelConverter extends DFTResultsConverter
{
	private static final Logger mLog = LoggerFactory.getLogger( RealDecibelConverter.class );

	private float mDynamicRangeReference;

	public RealDecibelConverter()
	{
		setSampleSize( 24.0 );
	}
	
	/**
	 * Specifies the bit depth to establish the maximum dynamic range.  All
	 * FFT bin values will be scaled according to this value.
	 */
	@Override
	public void setSampleSize( double size )
	{
		assert( 2.0 <= size && size <= 32.0 );
		
		mDynamicRangeReference = (float)Math.pow( 2.0d, size );
	}

	/**
	 * Converts the output of the JTransforms FloatFFT_1D.realForward()
	 * calculation into a normalized power spectrum in decibels, per description
	 * in Lyons, Understanding Digital Signal Processing, 3e, page 141.
	 */
	@Override
    public void receive( float[] results )
    {
		float[] processed = new float[ results.length / 4 ];

		int index = 0;
		
		for( int x = 0; x < processed.length; x ++ )
		{
			index = x * 2;
			
			float normalizedMagnitude = 10.0f * (float)Math.log10( 
				( ( results[ index ] * results[ index ] ) + 
				  ( results[ index + 1 ] * results[ index + 1 ] ) ) / 
				  		mDynamicRangeReference );

			processed[ x ] = normalizedMagnitude;
		}

		dispatch( processed );
    }
}
