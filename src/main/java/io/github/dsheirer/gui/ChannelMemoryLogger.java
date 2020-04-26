package io.github.dsheirer.gui;

import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.ChannelEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;

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

		sb.append("CHAN TOT:").append(mChannelCount);
		sb.append(" TRFC:").append(mTrafficChannelCount);
		sb.append(" PROC:").append(mChannelProcessingCount);

		long maxMemory = runtime.maxMemory();
		long allocatedMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();

		sb.append(" MEM FREE: ").append(format.format(freeMemory / 1024));
		sb.append(" ALLOC: ").append(format.format(allocatedMemory / 1024));
		sb.append(" MAX: ").append(format.format(maxMemory / 1024));
		sb.append(" TOTAL: ").append(format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024));
		
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
