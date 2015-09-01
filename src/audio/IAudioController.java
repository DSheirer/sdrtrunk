package audio;

import java.util.List;

import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.FloatControl;

import sample.Listener;
import audio.metadata.AudioMetadata;

/**
 * Interface for controlling one or more audio channels
 */
public interface IAudioController
{
	public List<String> getAudioChannels();
	
	public BooleanControl getMuteControl( String channel ) throws AudioException;
	
	public boolean hasMuteControl( String channel );
	
	public FloatControl getGainControl( String channel ) throws AudioException;
	
	public boolean hasGainControl( String channel );
	
	public void addAudioEventListener( String channel, Listener<AudioEvent> listener ) 
			throws AudioException;
	
	public void removeAudioEventListener( String channel, Listener<AudioEvent> listener );
	
	public void setAudioMetadataListener( String channel, Listener<AudioMetadata> listener );
	
	public void removeAudioMetadataListener( String channel );
	
	public void setConfigurationChangeListener( Listener<AudioEvent> listener );
}
