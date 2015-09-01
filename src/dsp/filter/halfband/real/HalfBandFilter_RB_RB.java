package dsp.filter.halfband.real;

import sample.Listener;
import sample.real.RealBuffer;

public class HalfBandFilter_RB_RB extends HalfBandFilter implements Listener<RealBuffer>
{
	private boolean mDecimate;
	private boolean mDecimateFlag;
	private Listener<RealBuffer> mListener;
	
	/**
	 * 
	 * @param coefficients
	 * @param gain
	 * @param decimate
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
				
				mListener.receive( new RealBuffer( decimated ) );
			}
			else
			{
				/* Non Decimate - filter each of the values and return the buffer */
				float[] samples = buffer.getSamples();
				
				for( int x = 0; x < samples.length; x++ )
				{
					samples[ x ] = filter( samples[ x ] );
				}
				
				mListener.receive( buffer );
			}
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
