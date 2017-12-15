package dsp.filter.dc;

import sample.Listener;
import sample.real.RealBuffer;

public class DCRemovalFilter_RB extends DCRemovalFilter 
					implements Listener<RealBuffer>
{
	protected Listener<RealBuffer> mListener;
	
	public DCRemovalFilter_RB( float ratio )
	{
		super( ratio );
	}
	
	public void setListener( Listener<RealBuffer> listener )
	{
		mListener = listener;
	}
	
	public void removeListener()
	{
		mListener = null;
	}
	
	public void dispose()
	{
		mListener = null;
	}
	
	public float[] filter( float[] samples )
	{
		for( int x = 0; x < samples.length; x++ )
		{
			samples[ x ] = filter( samples[ x ] );
		}
		
		return samples;
	}
	
	public RealBuffer filter( RealBuffer buffer )
	{
		float[] samples = buffer.getSamples();
		
		for( int x = 0; x < samples.length; x++ )
		{
			samples[ x ] = filter( samples[ x ] );
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
}
