package io.github.dsheirer.dsp.filter.halfband.real;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.real.RealBuffer;

public class HalfBandFilter_RB_RB extends HalfBandFilter implements Listener<RealBuffer>
{
	private boolean mDecimate;
	private boolean mDecimateFlag;
	private Listener<RealBuffer> mListener;
	
	/**
	 * Halfband filter for real buffers with optional decimation by two.
	 * 
	 * If this filter is used in non-decimating mode, the filter will overwrite
	 * the inbound sample buffer with the filtered samples.  This will cause
	 * issues if the buffers are being used across several processes are sharing 
	 * the buffer, since this filter will modify the contents of the object, so
	 * consider making a copy of the buffer prior to feeding the buffer to this
	 * filter.
	 * 
	 * If used in decimating mode, a new (half-length) buffer will be created
	 * for the filtered samples.  
	 * 
	 * @param coefficients - filter kernel
	 * @param gain - gain to apply to the outputs
	 * @param decimate - true for decimate by 2, or false for no decimation
	 */
	public HalfBandFilter_RB_RB( float[] coefficients, float gain, boolean decimate )
	{
		super( coefficients, gain );
		
		mDecimate = decimate;
	}
	
	public void dispose()
	{
		super.dispose();
		
		mListener = null;
	}
	
	@Override
	public void receive( RealBuffer buffer )
	{
		if( mListener != null )
		{
			mListener.receive( filter( buffer ) );
		}
	}

	public RealBuffer filter( RealBuffer buffer )
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

				for( float sample: samples )
				{
					/* Insert the sample but don't filter */
					if( mDecimateFlag )
					{
						insert( sample );
					}
					else
					{
						decimated[ decimatedPointer++ ] = filter( sample );
					}

					/* Toggle the decimation flag for every sample */
					mDecimateFlag = !mDecimateFlag;
				}
				
				return new RealBuffer( decimated );
			}
			else
			{
				/* Non Decimate - filter each of the values and return the buffer */
				float[] samples = buffer.getSamples();
				
				for( int x = 0; x < samples.length; x++ )
				{
					samples[ x ] = filter( samples[ x ] );
				}
				
				return buffer;
			}
	}

	public void setListener( Listener<RealBuffer> listener )
	{
		mListener = listener;
	}
	
	public void removeListener()
	{
		mListener = null;
	}
}
