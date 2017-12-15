package ua.in.smartjava.dsp.filter.fir.real;

import ua.in.smartjava.sample.Listener;
import ua.in.smartjava.sample.real.RealBuffer;

public class RealFIRFilter_RB_RB extends RealFIRFilter 
					implements Listener<RealBuffer>
{
	private Listener<RealBuffer> mListener;
	
	/**
	 * Real Buffer Float FIR ua.in.smartjava.filter with streaming float ua.in.smartjava.buffer provider and
	 * listener interfaces.
	 * 
	 * @param coefficients
	 * @param gain
	 */
	public RealFIRFilter_RB_RB( float[] coefficients, float gain )
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
	public void receive( RealBuffer buffer )
	{
		if( mListener != null )
		{
			mListener.receive( filter( buffer ) );
		}
	}

	/**
	 * Filters the ua.in.smartjava.buffer samples
	 * 
	 * @param buffer
	 * @return
	 */
	public RealBuffer filter( RealBuffer buffer )
	{
		float[] samples = buffer.getSamples();
		
		for( int x = 0; x < samples.length; x++ )
		{
			samples[ x ] = filter( samples[ x ] );
		}

		return buffer;
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
