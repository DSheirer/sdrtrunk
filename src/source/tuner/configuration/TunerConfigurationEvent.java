package source.tuner.configuration;

public class TunerConfigurationEvent
{
	private TunerConfiguration mTunerConfiguration;
	private TunerConfigurationAssignment mTunerConfigurationAssignment;
	private Event mEvent;
	
	public TunerConfigurationEvent( TunerConfiguration configuration, Event event )
	{
		mTunerConfiguration = configuration;
		mEvent = event;
	}
	
	public TunerConfigurationEvent( TunerConfigurationAssignment assignment, Event event )
	{
		mTunerConfigurationAssignment = assignment;
		mEvent = event;
	}
	
	public TunerConfiguration getConfiguration()
	{
		return mTunerConfiguration;
	}

	public TunerConfigurationAssignment getAssignment()
	{
		return mTunerConfigurationAssignment;
	}

	public Event getEvent()
	{
		return mEvent;
	}
	
	public enum Event
	{
		CONFIGURATION_ADD,
		CONFIGURATION_REMOVE,
		CONFIGURATION_CHANGE,
		ASSIGNMENT_ADD,
		ASSIGNMENT_REMOVE,
		ASSIGNMENT_CHANGE;
	}
}
