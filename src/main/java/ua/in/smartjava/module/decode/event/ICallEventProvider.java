package ua.in.smartjava.module.decode.event;

import ua.in.smartjava.sample.Listener;

public interface ICallEventProvider
{
	public void addCallEventListener( Listener<CallEvent> listener );
	public void removeCallEventListener( Listener<CallEvent> listener );
}
