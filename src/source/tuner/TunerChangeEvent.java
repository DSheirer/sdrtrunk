package source.tuner;

public class TunerChangeEvent
{
	private Tuner mTuner;
	private Event mEvent;
	
	public TunerChangeEvent( Tuner tuner, Event event )
	{
		mTuner = tuner;
		mEvent = event;
	}
	
	public Tuner getTuner()
	{
		return mTuner;
	}

	public Event getEvent()
	{
		return mEvent;
	}
	
	public enum Event
	{
		ADD,
		REMOVE,
		CHANNEL_COUNT,
		FREQUENCY,
		SAMPLE_RATE,
		SPECTRAL_DISPLAY;
	}
}
