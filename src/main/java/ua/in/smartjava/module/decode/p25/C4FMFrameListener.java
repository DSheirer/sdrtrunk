package ua.in.smartjava.module.decode.p25;

import ua.in.smartjava.bits.BinaryMessage;

public interface C4FMFrameListener
{
	/**
	 * Listener interface to receive the output of the C4FMMessageFramer.
	 * 
	 * @param buffer - framed ua.in.smartjava.message without the sync pattern
	 * @param inverted - flag indicating if the ua.in.smartjava.message was received inverted
	 */
	public void receive( BinaryMessage buffer, boolean inverted );

}
