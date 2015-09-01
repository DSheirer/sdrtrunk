package sample.real;

import sample.Listener;

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

	public void setListener( RealSampleListener listener )
	{
		mListener = listener;
	}
}
