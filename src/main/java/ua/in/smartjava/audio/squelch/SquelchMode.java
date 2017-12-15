package ua.in.smartjava.audio.squelch;

/**
 * Squelch state for a decoding ua.in.smartjava.channel.  The state determines the handling of
 * any ua.in.smartjava.audio packets processed by the ua.in.smartjava.channel state.
 */
public enum SquelchMode
{
	/**
	 * Automatic: decoded ua.in.smartjava.message activity dictates squelch state and determines
	 * if ua.in.smartjava.audio is passed
	 */
	AUTOMATIC,
	
	/**
	 * Manually applied squelch noise level threshold determines if ua.in.smartjava.audio is
	 * passed.
	 */
	MANUAL,
	
	/**
	 * No Squelch - no squelch is applied and all ua.in.smartjava.audio is passed
	 */
	NONE;
}
