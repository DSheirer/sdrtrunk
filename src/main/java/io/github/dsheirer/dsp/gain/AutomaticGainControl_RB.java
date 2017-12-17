package io.github.dsheirer.dsp.gain;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.real.RealBuffer;

public class AutomaticGainControl_RB extends AutomaticGainControl 
					implements Listener<RealBuffer>
{
	private Listener<RealBuffer> mListener;
	
	public AutomaticGainControl_RB()
	{
	}
	
	public void dispose()
	{
		mListener = null;
	}
	
	/**
	 * Applies AGC to a buffer of real (float) samples
	 */
	public RealBuffer process( RealBuffer buffer )
	{
		float[] samples = buffer.getSamples();

		for( int x = 0; x < samples.length; x++ )
		{
			samples[ x ] = process( samples[ x ] );
		}
		
		return buffer;
	}
	
	@Override
	public void receive( RealBuffer buffer )
	{
		if( mListener != null )
		{
			mListener.receive( process( buffer ) );
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
