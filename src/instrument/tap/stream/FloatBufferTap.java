package instrument.tap.stream;

import sample.Listener;
import sample.real.RealBuffer;

public class FloatBufferTap extends FloatTap implements Listener<RealBuffer>
{
	private Listener<RealBuffer> mListener;
	
	public FloatBufferTap( String name, int delay, float sampleRateRatio )
	{
		super( name, delay, sampleRateRatio );
	}

	@Override
	public void receive( RealBuffer buffer )
	{
		if( mListener != null )
		{
			mListener.receive( buffer );
		}

		for( float sample: buffer.getSamples() )
		{
			receive( sample );
		}
	}
	
	public void setListener( Listener<RealBuffer> listener )
    {
		mListener = listener;
    }

    public void removeListener( Listener<RealBuffer> listener )
    {
		mListener = null;
    }
}
