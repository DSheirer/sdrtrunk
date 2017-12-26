package io.github.dsheirer.module.decode.p25;

import io.github.dsheirer.bits.BinaryMessage;

public interface C4FMFrameListener
{
	/**
	 * Listener interface to receive the output of the C4FMMessageFramer.
	 * 
	 * @param buffer - framed message without the sync pattern
	 * @param inverted - flag indicating if the message was received inverted
	 */
	public void receive( BinaryMessage buffer, boolean inverted );

}
