package ua.in.smartjava.dsp.filter.halfband.real;

import ua.in.smartjava.sample.Listener;
import ua.in.smartjava.sample.real.RealBuffer;

public class HalfBandFilter_RB_RB extends HalfBandFilter implements Listener<RealBuffer>
{
	private boolean mDecimate;
	private boolean mDecimateFlag;
	private Listener<RealBuffer> mListener;
	
	/**
	 * Halfband ua.in.smartjava.filter for real buffers with optional decimation by two.
	 * 
	 * If this ua.in.smartjava.filter is used in non-decimating mode, the ua.in.smartjava.filter will overwrite
	 * the inbound ua.in.smartjava.sample ua.in.smartjava.buffer with the filtered samples.  This will cause
	 * issues if the buffers are being used across several processes are sharing 
	 * the ua.in.smartjava.buffer, since this ua.in.smartjava.filter will modify the contents of the object, so
	 * consider making a copy of the ua.in.smartjava.buffer prior to feeding the ua.in.smartjava.buffer to this
	 * ua.in.smartjava.filter.
	 * 
	 * If used in decimating mode, a new (half-length) ua.in.smartjava.buffer will be created
	 * for the filtered samples.  
	 * 
	 * @param coefficients - ua.in.smartjava.filter kernel
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
					/* If inbound ua.in.smartjava.buffer size is odd-length, then we have to
					 * adjust when the first operation is non-decimation, since
					 * that will produce an outbound ua.in.smartjava.buffer 1 ua.in.smartjava.sample larger */
					decimated = new float[ half + 1 ];
				}
				
				int decimatedPointer = 0;

				for( float sample: samples )
				{
					/* Insert the ua.in.smartjava.sample but don't ua.in.smartjava.filter */
					if( mDecimateFlag )
					{
						insert( sample );
					}
					else
					{
						decimated[ decimatedPointer++ ] = filter( sample );
					}

					/* Toggle the decimation flag for every ua.in.smartjava.sample */
					mDecimateFlag = !mDecimateFlag;
				}
				
				return new RealBuffer( decimated );
			}
			else
			{
				/* Non Decimate - ua.in.smartjava.filter each of the values and return the ua.in.smartjava.buffer */
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
