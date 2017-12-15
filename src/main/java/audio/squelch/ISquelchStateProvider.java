package audio.squelch;

import sample.Listener;

public interface ISquelchStateProvider
{
	public void setSquelchStateListener( Listener<SquelchState> listener );
	public void removeSquelchStateListener();
}
