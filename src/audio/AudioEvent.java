package audio;

public class AudioEvent
{
	private Type mType;
	private String mChannel;
	
	public AudioEvent( Type event, String channel )
	{
		mType = event;
		mChannel = channel;
	}
	
	public Type getType()
	{
		return mType;
	}
	
	public String getChannel()
	{
		return mChannel;
	}

	public enum Type
	{
		AUDIO_STARTED,
		AUDIO_STOPPED,
		AUDIO_CONFIGURATION_CHANGED;
	}
}
