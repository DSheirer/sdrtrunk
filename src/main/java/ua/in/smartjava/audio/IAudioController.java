package ua.in.smartjava.audio;

import java.util.List;

import ua.in.smartjava.sample.Listener;
import ua.in.smartjava.source.mixer.MixerChannelConfiguration;
import ua.in.smartjava.audio.output.AudioOutput;

/**
 * Interface for controlling one or more ua.in.smartjava.audio channels
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
