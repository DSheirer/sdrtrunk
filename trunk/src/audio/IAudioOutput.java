package audio;

import audio.SquelchListener.SquelchState;

public interface IAudioOutput
{
	/**
	 * Squelch / Unsquelch state for this channel 
	 */
	public abstract void setSquelch( SquelchState state );

	/**
	 * Audio playback defines if this channel is currently playing on the 
	 * system speakers. 
	 */
	public abstract void setAudioPlaybackEnabled( boolean enabled );

	/**
	 * Dispose of any system resources for this audio output
	 */
	public abstract void dispose();

	/**
	 * Sets the audio to normal or inverted output 
	 */
	public abstract void setAudioType( AudioType type );

}