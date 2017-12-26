package io.github.dsheirer.sample.real;

import io.github.dsheirer.sample.Listener;

/**
 * Real sample buffer to stream converter
 */
public class RealBufferToStreamConverter implements Listener<RealBuffer>
{
	private RealSampleListener mListener;
	
	@Override
	public void receive( RealBuffer buffer )
	{
		if( mListener != null )
		{
			for( float sample: buffer.getSamples() )
			{
				mListener.receive( sample );
			}
		}
	}
	
	public void dispose()
	{
		mListener = null;
	}

	public void setListener( RealSampleListener listener )
	{
		mListener = listener;
	}
}
