package dsp.filter.dc;

import sample.Listener;
import sample.real.RealBuffer;

public class DCRemovalFilter_RealBuffer implements Listener<RealBuffer>
{
	private Listener<RealBuffer> mListener;
	/**
	 * Removes DC bias present across the samples in the buffer by calculating
	 * the mean DC component and subtracting that component from each of the 
	 * samples.
	 */
	public DCRemovalFilter_RealBuffer()
	{
	}
	
	public void dispose()
	{
		mListener = null;
	}
	
	public RealBuffer filter( RealBuffer buffer )
	{
		double accumulator = 0.0d;
		
		for( float sample: buffer.getSamples() )
		{
			accumulator += sample;
		}

		float mean = (float)( accumulator / (double)buffer.getSamples().length );

		for( float sample: buffer.getSamples() )
		{
			sample -= mean;
		}
		
		return buffer;
	}

	@Override
	public void receive( RealBuffer buffer )
	{
		if( mListener != null )
		{
			mListener.receive( filter( buffer ) );
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
