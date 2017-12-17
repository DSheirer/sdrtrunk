package io.github.dsheirer.source.tuner.configuration;

public class TunerConfigurationEvent
{
	private TunerConfiguration mTunerConfiguration;
	private Event mEvent;
	
	public TunerConfigurationEvent( TunerConfiguration configuration, Event event )
	{
		mTunerConfiguration = configuration;
		mEvent = event;
	}
	
	public TunerConfiguration getConfiguration()
	{
		return mTunerConfiguration;
	}

	public Event getEvent()
	{
		return mEvent;
	}
	
	public enum Event
	{
		ADD,
		REMOVE,
		CHANGE;	
	}
}
