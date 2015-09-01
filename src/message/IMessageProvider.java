package message;

import sample.Listener;

public interface IMessageProvider
{
	public void addMessageListener( Listener<Message> listener );
	public void removeMessageListener( Listener<Message> listener );
}
