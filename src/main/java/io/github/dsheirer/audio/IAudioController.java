package io.github.dsheirer.audio;

import io.github.dsheirer.audio.playback.AudioOutput;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.mixer.MixerChannelConfiguration;

import java.util.List;

/**
 * Interface for controlling one or more audio channels
 */
public interface IAudioController
{
	/* Current Mixer and MixerChannel configuration */
	public void setMixerChannelConfiguration( MixerChannelConfiguration entry ) throws AudioException;
	public MixerChannelConfiguration getMixerChannelConfiguration() throws AudioException;
	
	/* Audio Output(s) */
	public List<AudioOutput> getAudioOutputs();

	/* Controller Audio Event Listener */
	public void addControllerListener( Listener<AudioEvent> listener );
	public void removeControllerListener( Listener<AudioEvent> listener );
}
