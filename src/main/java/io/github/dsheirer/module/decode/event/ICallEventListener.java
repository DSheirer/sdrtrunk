package io.github.dsheirer.module.decode.event;

import io.github.dsheirer.sample.Listener;

public interface ICallEventListener
{
	public Listener<CallEvent> getCallEventListener();
}
