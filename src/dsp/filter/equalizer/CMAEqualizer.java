package dsp.filter.equalizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import sample.complex.ComplexSample;

public class CMAEqualizer implements Listener<ComplexSample>
{
	private final static Logger mLog = LoggerFactory.getLogger( CMAEqualizer.class );

	public static final int TAP_COUNT = 11;
	
	private float mModulus;
	private float mMu;
	private ComplexSample mError = new ComplexSample( 0.0f, 0.0f );
	
	private ComplexSample[] mTaps = new ComplexSample[ TAP_COUNT ];
	private ComplexSample[] mBuffer = new ComplexSample[ TAP_COUNT ];
	
	private int mBufferPointer = 0;
	
	private Listener<ComplexSample> mListener;
	
	public CMAEqualizer( float modulus, float mu )
	{
		mModulus = modulus;
		mMu = mu;

		/* Set first tap to 1,0 as a start */
		mTaps[ 0 ] = new ComplexSample( 1.0f, 0.0f );
		
		for( int x = 1; x < TAP_COUNT; x++ )
		{
			mTaps[ x ] = new ComplexSample( 0.0f, 0.0f );
			mBuffer[ x ] = new ComplexSample( 0.0f, 0.0f );
		}
	}
	
	private void updateTaps( ComplexSample sample )
	{
		/* Calculate the error.  The ideal sample magnitude is unity (1.0).  
		 * When the sample is not at unity, the error vector points in the 
		 * opposite direction with a magnitude equal to 1 - sample magnitude.*/
		mError = ComplexSample.multiply( sample, sample.norm() - mModulus );

		/* Ensure we don't exceed unity */
		mError.clip( 1.0f );

		for( int x = 0; x < TAP_COUNT; x++ )
		{
			// tap -= mu * conj(sample) * error
			
			mTaps[ x ] = ComplexSample.subtract( mTaps[ x ], 
					 ComplexSample.multiply( ComplexSample.multiply( 
							 mBuffer[ x ].conjugate(), mError ), mMu ) );
		}
	}
	
	@Override
	public void receive( ComplexSample sample )
	{
		mBuffer[ mBufferPointer ] = sample;
		
		mBufferPointer++;
		
		mBufferPointer = mBufferPointer % TAP_COUNT;
		
		ComplexSample equalized = filter();
		
		updateTaps( equalized );

		if( mListener != null )
		{
			mListener.receive( equalized );
		}
		
		mLog.debug( "Tap 0:" + mTaps[ 0 ].toString() + " error:" + mError.toString() );
	}
	
	private ComplexSample filter()
	{
		ComplexSample sum = new ComplexSample( 0.0f, 0.0f );
		
		for( int x = 0; x < TAP_COUNT; x++ )
		{
			sum = ComplexSample.add( sum, ComplexSample.multiply( 
				mBuffer[ ( x + mBufferPointer ) % TAP_COUNT ], 
				mTaps[ TAP_COUNT - x - 1 ] ) );
		}

		return sum;
	}
	
	public void setListener( Listener<ComplexSample> listener )
	{
		mListener = listener;
	}
	
}
