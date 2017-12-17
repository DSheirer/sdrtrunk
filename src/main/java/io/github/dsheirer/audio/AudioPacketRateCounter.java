package io.github.dsheirer.audio;

import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudioPacketRateCounter implements Listener<AudioPacket>
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( AudioPacketRateCounter.class );

	private Listener<AudioPacket> mListener;
	private long mCount;
	private long mStartTime = -1;
	
	private String mName;
	
	public AudioPacketRateCounter()
	{
		mName = "UNNAMED";
	}
	
	public AudioPacketRateCounter( String name )
	{
		mName = name;
	}
	
	@Override
	public void receive( AudioPacket packet )
	{
		if( mStartTime == -1 )
		{
			mStartTime = System.currentTimeMillis();
		}
		
		mCount += packet.getAudioBuffer().getSamples().length;

		long elapsed = System.currentTimeMillis() - mStartTime;
		
		if( elapsed != 0 )
		{
			mLog.debug( "[" + mName + "] Audio Rate: " + ( mCount / elapsed ) + " kHz" );
		}
		
		if( mListener != null )
		{
			mListener.receive( packet );
		}
	}
	
	public void setListener( Listener<AudioPacket> listener )
	{
		mListener = listener;
	}
}
