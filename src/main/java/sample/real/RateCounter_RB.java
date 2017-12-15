package sample.real;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;

public class RateCounter_RB implements Listener<RealBuffer>
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( RateCounter_RB.class );

	private Listener<RealBuffer> mListener;
	private long mCount;
	private long mStartTime = -1;
	
	private String mName;
	
	public RateCounter_RB()
	{
		mName = "UNNAMED";
	}
	
	public RateCounter_RB( String name )
	{
		mName = name;
	}
	
	@Override
	public void receive( RealBuffer buffer )
	{
		if( mStartTime == -1 )
		{
			mStartTime = System.currentTimeMillis();
		}
		
		mCount += buffer.getSamples().length;

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
	
	public void setListener( Listener<RealBuffer> listener )
	{
		mListener = listener;
	}
}
