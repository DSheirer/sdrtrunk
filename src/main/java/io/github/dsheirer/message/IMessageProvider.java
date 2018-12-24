package io.github.dsheirer.message;

import io.github.dsheirer.sample.Listener;

public interface IMessageProvider
{
    void setMessageListener(Listener<IMessage> listener);
    void removeMessageListener();
}
