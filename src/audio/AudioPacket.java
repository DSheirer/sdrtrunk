package audio;

public class AudioPacket
{
	private String mSource;
	private Type mType;
	private byte[] mAudioData;
	private int mPriority;
	
	public AudioPacket( String source, Type type )
	{
		mSource = source;
		mType = type;
	}
	
	public AudioPacket( String source, byte[] audio, int priority )
	{
		this( source, Type.AUDIO );

		mAudioData = audio;
		mPriority = priority;
	}
	
	public String getSource()
	{
		return mSource;
	}
	
	public Type getType()
	{
		return mType;
	}
	
	public byte[] getAudioData()
	{
		return mAudioData;
	}
	
	public int getPriority()
	{
		return mPriority;
	}

	public enum Type
	{
		AUDIO,
		END;
	}
}
