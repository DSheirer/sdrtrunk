package ua.in.smartjava.channel.state;

import ua.in.smartjava.sample.Listener;

public interface IDecoderStateEventProvider
{
	public void setDecoderStateListener( Listener<DecoderStateEvent> listener );
	public void removeDecoderStateListener();
}
