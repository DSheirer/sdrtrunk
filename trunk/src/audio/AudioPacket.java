package audio;

import java.nio.ByteBuffer;

public class AudioPacket
{
	private ByteBuffer mAudioBuffer;
	private int mPriority;
	
	public AudioPacket()
	{
	}
	
	public void setAudioBuffer( ByteBuffer audioBuffer )
	{
		mAudioBuffer = audioBuffer;
	}
	
	public ByteBuffer getAudioBuffer()
	{
		return mAudioBuffer;
	}

}
