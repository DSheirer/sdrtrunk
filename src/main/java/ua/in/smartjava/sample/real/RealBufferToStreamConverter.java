package ua.in.smartjava.sample.real;

import ua.in.smartjava.sample.Listener;

/**
 * Real ua.in.smartjava.sample ua.in.smartjava.buffer to stream converter
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
