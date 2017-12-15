package message;

import sample.Listener;

public interface IMessageProvider
{
	public void setMessageListener( Listener<Message> listener );
	public void removeMessageListener();
}
