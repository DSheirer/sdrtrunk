package io.github.dsheirer.controller.channel;

import io.github.dsheirer.sample.Listener;

public interface IChannelEventListener
{
	public Listener<ChannelEvent> getChannelEventListener();
}
