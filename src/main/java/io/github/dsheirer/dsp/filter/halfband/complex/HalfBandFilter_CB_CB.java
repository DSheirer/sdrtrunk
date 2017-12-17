package io.github.dsheirer.dsp.filter.halfband.complex;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexBuffer;
import io.github.dsheirer.dsp.filter.halfband.real.HalfBandFilter;

public class HalfBandFilter_CB_CB implements Listener<ComplexBuffer>
{
	private HalfBandFilter mIFilter;
	private HalfBandFilter mQFilter;
	
	private boolean mDecimate;
	private boolean mDecimateFlag;
	
	private Listener<ComplexBuffer> mListener;
	
	/**
	 * 
	 * @param coefficients
	 * @param gain
	 * @param decimate
	 */
	public HalfBandFilter_CB_CB( float[] coefficients, float gain, boolean decimate )
	{
		mIFilter = new HalfBandFilter( coefficients, gain );
		mQFilter = new HalfBandFilter( coefficients, gain );
		
		mDecimate = decimate;
	}
	
	public void dispose()
	{
		mIFilter.dispose();
		mQFilter.dispose();
		
		mListener = null;
	}

	@Override
	public void receive( ComplexBuffer buffer )
	{
		if( mListener != null )
		{
			if( mDecimate )
			{
				float[] samples = buffer.getSamples();
				
				int half = samples.length / 2;

				float[] decimated;
				
				if( half % 2 == 0 || mDecimateFlag )
				{
					decimated = new float[ half ];
				}
				else
				{
					/* If inbound buffer size is odd-length, then we have to
					 * adjust when the first operation is non-decimation, since
					 * that will produce an outbound buffer 1 sample larger */
					decimated = new float[ half + 1 ];
				}
				
				int decimatedPointer = 0;

				for( int x = 0; x < samples.length; x += 2  )
				{
					/* Insert the sample but don't filter */
					if( mDecimateFlag )
					{
						mIFilter.insert( samples[ x ] );
						mQFilter.insert( samples[ x + 1 ] );
					}
					else
					{
						decimated[ decimatedPointer++ ] = mIFilter.filter( samples[ x ] );
						decimated[ decimatedPointer++ ] = mQFilter.filter( samples[ x + 1 ] );
					}

					/* Toggle the decimation flag for every sample */
					mDecimateFlag = !mDecimateFlag;
				}
				
				if( mListener != null )
				{
					mListener.receive( new ComplexBuffer( decimated ) );
				}
			}
			else
			{
				/* Non Decimate - filter each of the values and return the buffer */
				float[] samples = buffer.getSamples();
				
				for( int x = 0; x < samples.length; x += 2 )
				{
					samples[ x ] = mIFilter.filter( samples[ x ] );
					samples[ x + 1 ] = mQFilter.filter( samples[ x + 1 ] );
				}

				if( mListener != null )
				{
					mListener.receive( buffer );
				}
			}
		}
	}

	public void setListener( Listener<ComplexBuffer> listener )
	{
		mListener = listener;
	}
	
	public void removeListener()
	{
		mListener = null;
	}
}
