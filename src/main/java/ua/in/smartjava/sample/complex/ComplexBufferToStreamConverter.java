package ua.in.smartjava.sample.complex;

import ua.in.smartjava.sample.Listener;

/**
 * Real ua.in.smartjava.sample ua.in.smartjava.buffer to stream converter
 */
public class ComplexBufferToStreamConverter implements Listener<ComplexBuffer>
{
	private ComplexSampleListener mListener;
	
	public void dispose()
	{
		mListener = null;
	}
	
	@Override
	public void receive( ComplexBuffer buffer )
	{
		if( mListener != null )
		{
			float[] samples = buffer.getSamples();
			
			for( int x = 0; x < samples.length; x += 2 )
			{
				mListener.receive( samples[ x ], samples[ x + 1 ] );
			}
		}
	}

	public void setListener( ComplexSampleListener listener )
	{
		mListener = listener;
	}
}
