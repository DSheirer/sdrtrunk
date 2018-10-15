package io.github.dsheirer.message;

import io.github.dsheirer.sample.Listener;

public interface IMessageListener
{
    Listener<IMessage> getMessageListener();
}
