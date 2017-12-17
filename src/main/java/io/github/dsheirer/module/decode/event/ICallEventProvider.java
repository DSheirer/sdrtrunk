package io.github.dsheirer.module.decode.event;

import io.github.dsheirer.sample.Listener;

public interface ICallEventProvider
{
	public void addCallEventListener( Listener<CallEvent> listener );
	public void removeCallEventListener( Listener<CallEvent> listener );
}
