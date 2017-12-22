package io.github.dsheirer.dsp.filter.equalizer;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.Complex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CMAEqualizer implements Listener<Complex>
{
	private final static Logger mLog = LoggerFactory.getLogger( CMAEqualizer.class );

	public static final int TAP_COUNT = 11;
	
	private float mModulus;
	private float mMu;
	private Complex mError = new Complex( 0.0f, 0.0f );
	
	private Complex[] mTaps = new Complex[ TAP_COUNT ];
	private Complex[] mBuffer = new Complex[ TAP_COUNT ];
	
	private int mBufferPointer = 0;
	
	private Listener<Complex> mListener;
	
	public CMAEqualizer( float modulus, float mu )
	{
		mModulus = modulus;
		mMu = mu;

		/* Set first tap to 1,0 and all other taps to 0,0 as a starting point.
		 * This means that the output is solely determined by the last sample
		 * entering the filter, initially. */
		mTaps[ 0 ] = new Complex( 1.0f, 0.0f );
		
		for( int x = 1; x < TAP_COUNT; x++ )
		{
			mTaps[ x ] = new Complex( 0.0f, 0.0f );
			mBuffer[ x ] = new Complex( 0.0f, 0.0f );
		}
	}
	
	private void updateTaps( Complex sample )
	{
		/* Calculate the error.  The ideal sample magnitude is unity (1.0).
		 * When the sample is not at unity, the error vector points in the
		 * opposite direction with a magnitude equal to 1 - sample magnitude.*/
		mError = Complex.multiply( sample, sample.norm() - mModulus );

		/* Ensure we don't exceed unity */
		mError.clip( 1.0f );

		for( int x = 0; x < TAP_COUNT; x++ )
		{
			// tap -= mu * conj(sample) * error
			
			mTaps[ x ] = Complex.subtract( mTaps[ x ], 
					 Complex.multiply( Complex.multiply( 
							 mBuffer[ x ].conjugate(), mError ), mMu ) );
		}
	}
	
	@Override
	public void receive( Complex sample )
	{
		mBuffer[ mBufferPointer ] = sample;
		
		mBufferPointer++;
		
		mBufferPointer = mBufferPointer % TAP_COUNT;
		
		Complex equalized = filter();
		
		updateTaps( equalized );

		if( mListener != null )
		{
			mListener.receive( equalized );
		}
		
		mLog.debug( "Tap 0:" + mTaps[ 0 ].toString() + " error:" + mError.toString() );
	}
	
	private Complex filter()
	{
		Complex sum = new Complex( 0.0f, 0.0f );
		
		for( int x = 0; x < TAP_COUNT; x++ )
		{
			sum = Complex.add( sum, Complex.multiply( 
				mBuffer[ ( x + mBufferPointer ) % TAP_COUNT ], 
				mTaps[ TAP_COUNT - x - 1 ] ) );
		}

		return sum;
	}
	
	public void setListener( Listener<Complex> listener )
	{
		mListener = listener;
	}
	
}
