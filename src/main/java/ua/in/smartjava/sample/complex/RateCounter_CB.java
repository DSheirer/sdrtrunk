package ua.in.smartjava.sample.complex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ua.in.smartjava.sample.Listener;

public class RateCounter_CB implements Listener<ComplexBuffer>
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( RateCounter_CB.class );

	private Listener<ComplexBuffer> mListener;
	private long mCount;
	private long mStartTime = -1;
	
	private String mName;
	
	public RateCounter_CB()
	{
		mName = "UNNAMED";
	}
	
	public RateCounter_CB( String name )
	{
		mName = name;
	}
	
	@Override
	public void receive( ComplexBuffer buffer )
	{
		if( mStartTime == -1 )
		{
			mStartTime = System.currentTimeMillis();
		}
		
		mCount += ( buffer.getSamples().length / 2 );

		long elapsed = System.currentTimeMillis() - mStartTime;
		
		if( elapsed != 0 )
		{
			mLog.debug( "[" + mName + "] Sample Rate: " + ( mCount / elapsed ) + " kHz" );
		}
		
		if( mListener != null )
		{
			mListener.receive( buffer );
		}
	}
	
	public void setListener( Listener<ComplexBuffer> listener )
	{
		mListener = listener;
	}
}
