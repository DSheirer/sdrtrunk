package audio.output;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.FloatControl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Broadcaster;
import sample.Listener;
import audio.AudioEvent;
import audio.AudioPacket;
import audio.metadata.AudioMetadata;

public abstract class AudioOutput implements Listener<AudioPacket>
{
	private final static Logger mLog = LoggerFactory.getLogger( AudioOutput.class );
	
	private AtomicBoolean mSquelched = new AtomicBoolean( false );
	private Listener<AudioMetadata> mAudioMetadataListener;
	protected Broadcaster<AudioEvent> mAudioEventBroadcaster = new Broadcaster<>();

	public void dispose()
	{
		mAudioEventBroadcaster.dispose();
		mAudioEventBroadcaster = null;
		mAudioMetadataListener = null;
	}
	
	public abstract String getChannelName();
	
	public abstract BooleanControl getMuteControl();
	public abstract boolean hasMuteControl();
	
	public abstract FloatControl getGainControl();
	public abstract boolean hasGainControl();
	
	protected void broadcast( AudioMetadata metadata )
	{
		if( mAudioMetadataListener != null )
		{
			mAudioMetadataListener.receive( metadata );
		}
	}
	
	public void setSquelched( boolean squelched )
	{
		mSquelched.set( squelched );
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
}
