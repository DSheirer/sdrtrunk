package dsp.filter.equalizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import sample.complex.ComplexSample;

public class CMAEqualizer implements Listener<ComplexSample>
{
	private final static Logger mLog = LoggerFactory.getLogger( CMAEqualizer.class );

	public static final int TAP_COUNT = 4;
	
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
		
		mTaps[ 0 ] = new ComplexSample( 1.0f, 1.0f );
		mBuffer[ 0 ] = new ComplexSample( 1.0f, 1.0f );
		
		for( int x = 1; x < TAP_COUNT; x++ )
		{
			mTaps[ x ] = new ComplexSample( 0.0f, 0.0f );
			mBuffer[ x ] = new ComplexSample( 1.0f, 1.0f );
		}
	}
	
	private void updateTaps()
	{
		for( int x = 0; x < TAP_COUNT; x++ )
		{
			// tap -= mu * conj(sample) * error
			
			mTaps[ x ] = ComplexSample.subtract( mTaps[ x ], 
						 ComplexSample.multiply( ComplexSample.multiply( 
								 mBuffer[ x ].conjugate(), mMu ), mError ) );
		}
	}
	
	@Override
	public void receive( ComplexSample sample )
	{
		mBuffer[ mBufferPointer ] = sample;
		
		mBufferPointer++;
		
		mBufferPointer = mBufferPointer % TAP_COUNT;
		
		ComplexSample equalized = filter();
		
		calculateError( equalized );
		
		updateTaps();

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
			sum = ComplexSample.add( sum, 
				ComplexSample.multiply( mBuffer[ x ], mTaps[ x ] ) );
		}

		return sum;
	}
	
	public void setListener( Listener<ComplexSample> listener )
	{
		mListener = listener;
	}
	
	private void calculateError( ComplexSample sample )
	{
		mError = ComplexSample.multiply( sample, sample.norm() - mModulus );
		mError.clip( 1.0f );
	}
}
