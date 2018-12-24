package io.github.dsheirer.controller.channel;

import io.github.dsheirer.sample.Listener;

public interface IChannelEventProvider
{
	void setChannelEventListener( Listener<ChannelEvent> listener );
	void removeChannelEventListener();
}
