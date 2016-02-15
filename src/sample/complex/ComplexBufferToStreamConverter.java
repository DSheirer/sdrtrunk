package sample.complex;

import module.decode.p25.P25_LSMDecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;

/**
 * Real sample buffer to stream converter
 */
public class ComplexBufferToStreamConverter implements Listener<ComplexBuffer>
{
	private final static Logger mLog = LoggerFactory.getLogger( ComplexBufferToStreamConverter.class );

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
