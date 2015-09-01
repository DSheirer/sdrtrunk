package module.decode.event;

import sample.Listener;

public interface ICallEventProvider
{
	public void setCallEventListener( Listener<CallEvent> listener );
	public void removeCallEventListener();
}
