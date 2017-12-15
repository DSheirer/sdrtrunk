package ua.in.smartjava.message;

import ua.in.smartjava.sample.Listener;

public interface IMessageProvider
{
	public void setMessageListener( Listener<Message> listener );
	public void removeMessageListener();
}
