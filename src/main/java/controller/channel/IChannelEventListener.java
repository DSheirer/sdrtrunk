package controller.channel;

import sample.Listener;

public interface IChannelEventListener
{
	public Listener<ChannelEvent> getChannelEventListener();
}
