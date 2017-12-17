package io.github.dsheirer.dsp.filter.fir.real;

import io.github.dsheirer.sample.real.RealSampleListener;

public class RealFIRFilter_R_R extends RealFIRFilter implements RealSampleListener
{
	private RealSampleListener mListener;
	
	/**
	 * Float FIR filter with streaming float provider and listener interfaces.
	 * 
	 * @param coefficients
	 * @param gain
	 */
	public RealFIRFilter_R_R( float[] coefficients, float gain )
	{
		super( coefficients, gain );
	}

	@Override
	public void dispose()
	{
		super.dispose();
		
		mListener = null;
	}

	@Override
	public void receive( float sample )
	{
		if( mListener != null )
		{
			mListener.receive( filter( sample ) );
		}
	}
	
	public float[] filter( float[] samples )
	{
		for( int x = 0; x < samples.length; x++ )
		{
			samples[ x ] = filter( samples[ x ] );
		}
		
		return samples;
	}

	public void setListener( RealSampleListener listener )
	{
		mListener = listener;
	}

	public void removeListener()
	{
		mListener = null;
	}
}
