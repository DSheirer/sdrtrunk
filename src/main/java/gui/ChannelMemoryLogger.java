package gui;

import java.text.NumberFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controller.channel.Channel.ChannelType;
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
			case NOTIFICATION_ADD:
				if( event.getChannel().getChannelType() == ChannelType.TRAFFIC )
				{
					mTrafficChannelCount++;
				}
				else
				{
					mChannelCount++;
				}
				break;
			case NOTIFICATION_DELETE:
				if( event.getChannel().getChannelType() == ChannelType.TRAFFIC )
				{
					mTrafficChannelCount--;
				}
				else
				{
					mChannelCount--;
				}
				break;
			case REQUEST_DISABLE:
				break;
			case REQUEST_ENABLE:
				break;
			case NOTIFICATION_PROCESSING_START:
				mChannelProcessingCount++;
				break;
			case NOTIFICATION_PROCESSING_STOP:
				mChannelProcessingCount--;
				break;
			case NOTIFICATION_STATE_RESET:
				break;
			default:
				break;
		}
	}
}
