package channel.state;

import controller.channel.Channel.ChannelType;

/**
 * Allows decoders to change the call timeout setting in the channel state
 */
public class ChangeChannelTimeoutEvent extends DecoderStateEvent
{
	private ChannelType mChannelType;
	private long mCallTimeout;
	
	public ChangeChannelTimeoutEvent( Object source, ChannelType channelType, long timeout )
	{
		super( source, Event.CHANGE_CALL_TIMEOUT, State.IDLE );
		mChannelType = channelType;
		mCallTimeout = timeout;
	}
	
	public ChannelType getChannelType()
	{
		return mChannelType;
	}
	
	public long getCallTimeout()
	{
		return mCallTimeout;
	}
}