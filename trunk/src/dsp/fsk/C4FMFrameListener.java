package dsp.fsk;

import bits.BitSetBuffer;

public interface C4FMFrameListener
{
	/**
	 * Listener interface to receive the output of the C4FMMessageFramer.
	 * 
	 * @param buffer - framed message without the sync pattern
	 * @param inverted - flag indicating if the message was received inverted
	 */
	public void receive( BitSetBuffer buffer, boolean inverted );

}
