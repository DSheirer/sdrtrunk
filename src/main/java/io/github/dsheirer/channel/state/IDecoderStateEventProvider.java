package io.github.dsheirer.channel.state;

import io.github.dsheirer.sample.Listener;

public interface IDecoderStateEventProvider
{
	void setDecoderStateListener( Listener<DecoderStateEvent> listener );
	void removeDecoderStateListener();
}
