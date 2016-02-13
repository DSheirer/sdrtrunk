package audio.output;

import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.FloatControl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Broadcaster;
import sample.Listener;
import audio.AudioEvent;
import audio.AudioEvent.Type;
import audio.AudioPacket;
import audio.metadata.AudioMetadata;

public abstract class AudioOutput implements Listener<AudioPacket>
{
	private final static Logger mLog = LoggerFactory.getLogger( AudioOutput.class );
	
	private AtomicBoolean mSquelched = new AtomicBoolean( false );
	private Listener<AudioMetadata> mAudioMetadataListener;

	protected Broadcaster<AudioEvent> mAudioEventBroadcaster = new Broadcaster<>();
	protected LinkedTransferQueue<AudioPacket> mBuffer = new LinkedTransferQueue<>();
	protected ScheduledExecutorService mExecutorService;
	protected boolean mCanProcessAudio;
	protected long mLastActivity;

	public void reset()
	{
		broadcast( new AudioEvent( AudioEvent.Type.AUDIO_STOPPED, 
				getChannelName() ) );
	}
	
	public void dispose()
	{
		mCanProcessAudio = false;

		mBuffer.clear();

		if( mExecutorService != null )
		{
			mExecutorService.shutdown();
		}

		mAudioEventBroadcaster.dispose();
		mAudioEventBroadcaster = null;
		mAudioMetadataListener = null;
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
	
	public void setSquelched( boolean squelched )
	{
		mSquelched.set( squelched );
		
		broadcast( new AudioEvent( squelched ? Type.AUDIO_SQUELCHED : 
			Type.AUDIO_UNSQUELCHED, getChannelName() ) );
	}
	
	public boolean isSquelched()
	{
		return mSquelched.get();
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
