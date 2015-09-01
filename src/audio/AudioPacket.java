package audio;

import audio.metadata.AudioMetadata;

public class AudioPacket
{
	private Type mType;
	private float[] mAudioData;
	private AudioMetadata mAudioMetadata;
	
	public AudioPacket( Type type )
	{
		mType = type;
	}
	
	public AudioPacket( float[] audio, AudioMetadata metadata )
	{
		this( Type.AUDIO );

		mAudioData = audio;
		mAudioMetadata = metadata;
	}
	
	public AudioMetadata getMetadata()
	{
		return mAudioMetadata;
	}
	
	public void setMetadata( AudioMetadata metadata )
	{
		mAudioMetadata = metadata;
	}
	
	public Type getType()
	{
		return mType;
	}
	
	public float[] getAudioData()
	{
		return mAudioData;
	}
	
	public enum Type
	{
		AUDIO,
		END;
	}
}
