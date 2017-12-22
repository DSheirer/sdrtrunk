package io.github.dsheirer.message;

import io.github.dsheirer.sample.Listener;

public interface IMessageProvider
{
	public void setMessageListener( Listener<Message> listener );
	public void removeMessageListener();
}
