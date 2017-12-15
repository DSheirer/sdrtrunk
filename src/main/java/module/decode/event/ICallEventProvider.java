package module.decode.event;

import sample.Listener;

public interface ICallEventProvider
{
	public void addCallEventListener( Listener<CallEvent> listener );
	public void removeCallEventListener( Listener<CallEvent> listener );
}
