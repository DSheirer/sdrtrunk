package gui;

import java.text.NumberFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controller.channel.ChannelEvent;
import controller.channel.ChannelEventListener;

public class ChannelMemoryLogger implements Runnable, ChannelEventListener
{
	private final static Logger mLog = LoggerFactory.getLogger( ChannelMemoryLogger.class );

	public int mChannelCount;
	public int mChannelProcessingCount;
	public int mTrafficChannelCount;
	
	@Override
	public void run()
	{
		final Runtime runtime = Runtime.getRuntime();

		final NumberFormat format = NumberFormat.getInstance();

		StringBuilder sb = new StringBuilder();

		sb.append( "CHAN TOT:" + mChannelCount );
		sb.append( " TRFC:" + mTrafficChannelCount );
		sb.append( " PROC:" + mChannelProcessingCount );

		long maxMemory = runtime.maxMemory();
		long allocatedMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();

		sb.append(" MEM FREE: " + format.format(freeMemory / 1024) );
		sb.append(" ALLOC: " + format.format(allocatedMemory / 1024) );
		sb.append(" MAX: " + format.format(maxMemory / 1024) );
		sb.append(" TOTAL: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024) );
		
		mLog.debug( sb.toString() );
	}

	@Override
	public void channelChanged( ChannelEvent event )
	{
		switch( event.getEvent() )
		{
			case CHANNEL_ADDED:
				mChannelCount++;
				break;
			case CHANNEL_DELETED:
				mChannelCount--;
				break;
			case CHANNEL_DISABLED:
				break;
			case CHANNEL_ENABLED:
				break;
			case CHANNEL_PROCESSING_STARTED:
				mChannelProcessingCount++;
				break;
			case CHANNEL_PROCESSING_STOPPED:
				mChannelProcessingCount--;
				break;
			case CHANNEL_STATE_RESET:
				break;
			case TRAFFIC_CHANNEL_ADDED:
				mTrafficChannelCount++;
				break;
			case TRAFFIC_CHANNEL_DELETED:
				mTrafficChannelCount--;
				break;
			default:
				break;
		
		}
	}
}
