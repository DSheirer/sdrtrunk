package dsp.filter.interpolator;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.complex.ComplexSample;

public class QPSKInterpolator extends Interpolator
{
	private final static Logger mLog = LoggerFactory.getLogger( QPSKInterpolator.class );

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
	public ComplexSample filter( ComplexSample[] samples, int offset, float mu )
	{
		/* Ensure we have enough samples in the array */
		assert( samples.length >= offset + 7 );

		/* Identify the filter bank that corresponds to mu */
		int index = (int)( NSTEPS * mu );
		
		float realAccumulator = ( TAPS[ index ][ 7 ] * samples[ offset ].real() );
		realAccumulator += ( TAPS[ index ][ 6 ] * samples[ offset + 1 ].real() );
		realAccumulator += ( TAPS[ index ][ 5 ] * samples[ offset + 2 ].real() );
		realAccumulator += ( TAPS[ index ][ 4 ] * samples[ offset + 3 ].real() );
		realAccumulator += ( TAPS[ index ][ 3 ] * samples[ offset + 4 ].real() );
		realAccumulator += ( TAPS[ index ][ 2 ] * samples[ offset + 5 ].real() );
		realAccumulator += ( TAPS[ index ][ 1 ] * samples[ offset + 6 ].real() );
		realAccumulator += ( TAPS[ index ][ 0 ] * samples[ offset + 7 ].real() );
		
		float imaginaryAccumulator = ( TAPS[ index ][ 7 ] * samples[ offset ].imaginary() );
		imaginaryAccumulator += ( TAPS[ index ][ 6 ] * samples[ offset + 1 ].imaginary() );
		imaginaryAccumulator += ( TAPS[ index ][ 5 ] * samples[ offset + 2 ].imaginary() );
		imaginaryAccumulator += ( TAPS[ index ][ 4 ] * samples[ offset + 3 ].imaginary() );
		imaginaryAccumulator += ( TAPS[ index ][ 3 ] * samples[ offset + 4 ].imaginary() );
		imaginaryAccumulator += ( TAPS[ index ][ 2 ] * samples[ offset + 5 ].imaginary() );
		imaginaryAccumulator += ( TAPS[ index ][ 1 ] * samples[ offset + 6 ].imaginary() );
		imaginaryAccumulator += ( TAPS[ index ][ 0 ] * samples[ offset + 7 ].imaginary() );

		return new ComplexSample( realAccumulator, imaginaryAccumulator );
	}
}
