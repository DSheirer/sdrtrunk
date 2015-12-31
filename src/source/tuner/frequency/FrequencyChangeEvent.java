package source.tuner.frequency;

public class FrequencyChangeEvent
{
	public enum Event
	{
		FREQUENCY_CHANGE_NOTIFICATION,
		FREQUENCY_CORRECTION_CHANGE_NOTIFICATION,
		CHANNEL_FREQUENCY_CORRECTION_CHANGE_NOTIFICATION,
		CHANNEL_FREQUENCY_CORRECTION_CHANGE_REQUEST,
		SAMPLE_RATE_CHANGE_NOTIFICATION;
	}

	private Event mEvent;
	private Number mValue;
	
	public FrequencyChangeEvent( Event event, Number value )
	{
		mEvent = event;
		mValue = value;
	}
	
	public Event getEvent()
	{
		return mEvent;
	}
	
	public Number getValue()
	{
		return mValue;
	}
}
