package io.github.dsheirer.channel.state;

import io.github.dsheirer.sample.Listener;

public interface IDecoderStateEventProvider
{
	public void setDecoderStateListener( Listener<DecoderStateEvent> listener );
	public void removeDecoderStateListener();
}
