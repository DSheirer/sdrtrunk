package io.github.dsheirer.message;

import io.github.dsheirer.sample.Listener;

public interface IMessageListener
{
	public Listener<Message> getMessageListener();
}
