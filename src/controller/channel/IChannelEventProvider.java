package controller.channel;

import sample.Listener;

public interface IChannelEventProvider
{
	public void setChannelEventListener( Listener<ChannelEvent> listener );
	public void removeChannelEventListener();
}
