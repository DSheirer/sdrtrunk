package source.tuner.frequency;

public class FrequencyChangeEvent
{
	public enum Event
	{
		NOTIFICATION_FREQUENCY_CHANGE,
		NOTIFICATION_FREQUENCY_CORRECTION_CHANGE,
		NOTIFICATION_CHANNEL_FREQUENCY_CORRECTION_CHANGE,
		NOTIFICATION_SAMPLE_RATE_CHANGE,
		
		REQUEST_CHANNEL_FREQUENCY_CORRECTION_CHANGE;
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
