package io.github.dsheirer.audio.squelch;

/**
 * Squelch state for a decoding channel.  The state determines the handling of
 * any audio packets processed by the channel state.
 */
public enum SquelchMode
{
	/**
	 * Automatic: decoded message activity dictates squelch state and determines
	 * if audio is passed
	 */
	AUTOMATIC,
	
	/**
	 * Manually applied squelch noise level threshold determines if audio is
	 * passed.
	 */
	MANUAL,
	
	/**
	 * No Squelch - no squelch is applied and all audio is passed
	 */
	NONE;
}
