package io.github.dsheirer.spectrum.converter;

import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealDecibelConverter extends DFTResultsConverter
{
	private static final Logger mLog = LoggerFactory.getLogger( RealDecibelConverter.class );

	public RealDecibelConverter()
	{
	}
	
	/**
	 * Converts the output of the JTransforms FloatFFT_1D.realForward()
	 * calculation into a normalized power spectrum in decibels, per description
	 * in Lyons, Understanding Digital Signal Processing, 3e, page 141.
	 * 
	 * Note: this is only calculating the lower half of the spectrum
	 */
	@Override
    public void receive( float[] results )
    {
		float dftBinSizeScalor = 1.0f / (float)results.length;
		
		float[] processed = new float[ results.length / 4 ];

		int index = 0;
		
		for( int x = 0; x < processed.length; x ++ )
		{
			index = x * 2;
			
			processed[ x ] = 20.0f * (float) FastMath.log10(
				( ( results[ index ] * results[ index ] ) + 
				  ( results[ index + 1 ] * results[ index + 1 ] ) ) * dftBinSizeScalor );
		}

		dispatch( processed );
    }
}
