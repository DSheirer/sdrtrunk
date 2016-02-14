package audio.output;

import java.util.concurrent.LinkedTransferQueue;

import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Broadcaster;
import sample.Listener;
import audio.AudioEvent;
import audio.AudioPacket;
import audio.metadata.AudioMetadata;
import controller.ThreadPoolManager;

public abstract class AudioOutput implements Listener<AudioPacket>, LineListener
{
	private final static Logger mLog = LoggerFactory.getLogger( AudioOutput.class );
	
	private Listener<AudioMetadata> mAudioMetadataListener;
	protected Broadcaster<AudioEvent> mAudioEventBroadcaster = new Broadcaster<>();
	protected LinkedTransferQueue<AudioPacket> mBuffer = new LinkedTransferQueue<>();
	protected ThreadPoolManager mThreadPoolManager;
	protected boolean mCanProcessAudio;
	protected long mLastActivity;

	public AudioOutput( ThreadPoolManager threadPoolManager )
	{
		mThreadPoolManager = threadPoolManager;
	}
	
	public void reset()
	{
		broadcast( new AudioEvent( AudioEvent.Type.AUDIO_STOPPED, 
				getChannelName() ) );
	}
	
	public void dispose()
	{
		mCanProcessAudio = false;

		mBuffer.clear();

		mAudioEventBroadcaster.dispose();
		mAudioEventBroadcaster = null;
		mAudioMetadataListener = null;
		
		mThreadPoolManager = null;
	}
	
	public abstract String getChannelName();
	
	public abstract void setMuted( boolean muted );
	public abstract boolean isMuted();
	
	public abstract FloatControl getGainControl();
	public abstract boolean hasGainControl();
	
	protected void broadcast( AudioMetadata metadata )
	{
		if( mAudioMetadataListener != null )
		{
			mAudioMetadataListener.receive( metadata );
		}
	}
	
	protected void broadcast( AudioEvent audioEvent )
	{
		mAudioEventBroadcaster.broadcast( audioEvent );
	}
	
	/**
	 * Registers a single listener to receive audio start and audio stop events
	 */
	public void addAudioEventListener( Listener<AudioEvent> listener )
	{
		mAudioEventBroadcaster.addListener( listener );
	}
	
	public void removeAudioEventListener( Listener<AudioEvent> listener )
	{
		mAudioEventBroadcaster.removeListener( listener );
	}
	
	/**
	 * Registers a single listener to receive the audio metadata from each 
	 * audio packet
	 */
	public void setAudioMetadataListener( Listener<AudioMetadata> listener )
	{
		mAudioMetadataListener = listener;
	}
	
	public void removeAudioMetadataListener()
	{
		mAudioMetadataListener = null;
	}

	/**
	 * Timestamp when most recent buffer was received
	 */
	public long getLastBufferReceived()
	{
		return mLastActivity;
	}

	@Override
	public void receive( AudioPacket packet )
	{
		if( mCanProcessAudio )
		{
			mLastActivity = System.currentTimeMillis();
			
			mBuffer.add( packet );
		}
	}
}