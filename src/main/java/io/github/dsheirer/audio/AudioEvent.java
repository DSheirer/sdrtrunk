package io.github.dsheirer.audio;

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
		AUDIO_MUTED,
		AUDIO_UNMUTED,
		AUDIO_SQUELCHED,
		AUDIO_UNSQUELCHED,
		AUDIO_STARTED,
		AUDIO_STOPPED,
		AUDIO_CONTINUATION,
		AUDIO_CONFIGURATION_CHANGE_STARTED,
		AUDIO_CONFIGURATION_CHANGE_COMPLETE;
	}
}
