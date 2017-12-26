package io.github.dsheirer.controller.channel;

import io.github.dsheirer.sample.Listener;

public interface IChannelEventProvider
{
	public void setChannelEventListener( Listener<ChannelEvent> listener );
	public void removeChannelEventListener();
}
