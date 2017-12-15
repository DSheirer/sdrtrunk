package message;

import sample.Listener;

public interface IMessageListener
{
	public Listener<Message> getMessageListener();
}
