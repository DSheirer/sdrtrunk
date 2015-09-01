package controller.channel;

public class ChannelEvent
{
	private Channel mChannel;
	private Event mEvent;

	/**
	 * ChannelEvent - event describing any changes to channels
	 * @param channel - channel that changed
	 * @param event - change event
	 */
	public ChannelEvent( Channel channel, Event event )
	{
		mChannel = channel;
		mEvent = event;
	}
	
	public Channel getChannel()
	{
		return mChannel;
	}
	
	public Event getEvent()
	{
		return mEvent;
	}
	
	/**
	 * Channel Events - used to specify channel events and changes to channel
	 * configurations and settings.
	 */
	public enum Event
	{
		CHANGE_ALIAS_LIST,
		CHANGE_DECODER,
        CHANGE_EVENT_LOGGER,
		CHANGE_NAME,
		CHANGE_SELECTED,
		CHANGE_SITE,
		CHANGE_RECORDER,
		CHANGE_SOURCE,
		CHANGE_SYSTEM,

        CHANNEL_ADDED,
        CHANNEL_DELETED,
        
        CHANNEL_ENABLED,
        CHANNEL_DISABLED,

		CHANNEL_PROCESSING_STARTED,
		CHANNEL_PROCESSING_STOPPED,

		CHANNEL_STATE_RESET,
		
		TRAFFIC_CHANNEL_ADDED,
		TRAFFIC_CHANNEL_DELETED;
	}
}
