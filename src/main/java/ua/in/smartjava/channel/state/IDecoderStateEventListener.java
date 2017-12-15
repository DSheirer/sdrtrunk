package ua.in.smartjava.channel.state;

import ua.in.smartjava.sample.Listener;

public interface IDecoderStateEventListener
{
	public Listener<DecoderStateEvent> getDecoderStateListener();
}
