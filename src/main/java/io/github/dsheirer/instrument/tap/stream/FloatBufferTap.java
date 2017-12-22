package io.github.dsheirer.instrument.tap.stream;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.real.RealBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FloatBufferTap extends FloatTap implements Listener<RealBuffer>
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( FloatBufferTap.class );

	private Listener<RealBuffer> mRealBufferListener;
	
	public FloatBufferTap( String name, int delay, float sampleRateRatio )
	{
		super( name, delay, sampleRateRatio );
	}

	@Override
	public void receive( RealBuffer buffer )
	{
		if( mRealBufferListener != null )
		{
			mRealBufferListener.receive( buffer );
		}

		for( float sample: buffer.getSamples() )
		{
			super.receive( sample );
		}
	}
	
	public void setListener( Listener<RealBuffer> listener )
    {
		mRealBufferListener = listener;
    }

    public void removeListener( Listener<RealBuffer> listener )
    {
		mRealBufferListener = null;
    }
}
