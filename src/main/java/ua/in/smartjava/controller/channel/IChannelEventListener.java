package ua.in.smartjava.controller.channel;

import ua.in.smartjava.sample.Listener;

public interface IChannelEventListener
{
	public Listener<ChannelEvent> getChannelEventListener();
}
