package ua.in.smartjava.controller.channel;

import ua.in.smartjava.sample.Listener;

public interface IChannelEventProvider
{
	public void setChannelEventListener( Listener<ChannelEvent> listener );
	public void removeChannelEventListener();
}
