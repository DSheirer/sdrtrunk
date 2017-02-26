package dsp.filter.interpolator;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.complex.Complex;

public class RealInterpolator extends Interpolator
{
	private final static Logger mLog = LoggerFactory.getLogger( RealInterpolator.class );

	private float mGain;
	
	public RealInterpolator( float gain )
	{
		mGain = gain;
	}
	/**
	 * Calculates an interpolated value from the 8 samples beginning at the 
	 * offset index.  Uses the mu value to determine which of 128 filter kernels
	 * to use in order to closely approximate the ideal frequency response for 
	 * the value of mu, ranging from 0.0 to 1.0, indicating the interpolated
	 * position in the sample set ranging from offset to offset + 7.
	 * 
	 * @param samples - sample array of length at least offset + 7
	 * 
	 * @param mu - interpolated sample position between 0 and 1.0 
	 * 
	 * @return - interpolated sample value
	 */
	public float filter( float[] samples, int offset, float mu )
	{
		/* Ensure we have enough samples in the array */
		Validate.isTrue(samples.length >= offset + 7);

		/* Identify the filter bank that corresponds to mu */
		int index = (int)( NSTEPS * mu );
		
		float accumulator = ( TAPS[ index ][ 7 ] * samples[ offset ] );
		accumulator += ( TAPS[ index ][ 6 ] * samples[ offset + 1 ] );
		accumulator += ( TAPS[ index ][ 5 ] * samples[ offset + 2 ] );
		accumulator += ( TAPS[ index ][ 4 ] * samples[ offset + 3 ] );
		accumulator += ( TAPS[ index ][ 3 ] * samples[ offset + 4 ] );
		accumulator += ( TAPS[ index ][ 2 ] * samples[ offset + 5 ] );
		accumulator += ( TAPS[ index ][ 1 ] * samples[ offset + 6 ] );
		accumulator += ( TAPS[ index ][ 0 ] * samples[ offset + 7 ] );
		
		return accumulator * mGain;
	}
	
	public Complex filter( float[] iSamples, float[] qSamples, int offset, float mu )
	{
		float i = filter( iSamples, offset, mu );
		float q = filter( qSamples, offset, mu );
		
		return new Complex( i, q );
	}
}
