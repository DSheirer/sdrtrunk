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
		CHANGE_ENABLED,
        CHANGE_EVENT_LOGGER,
		CHANGE_NAME,
		CHANGE_SITE,
		CHANGE_RECORDER,
		CHANGE_SELECTED,
		CHANGE_SOURCE,
		CHANGE_SYSTEM,

		CHANNEL_ADDED,
        CHANNEL_DELETED,

		PROCESSING_STARTED,
		PROCESSING_STOPPED,
	}
}
