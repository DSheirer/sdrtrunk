package io.github.dsheirer.channel.state;

import io.github.dsheirer.sample.Listener;

public interface IDecoderStateEventListener
{
	public Listener<DecoderStateEvent> getDecoderStateListener();
}
