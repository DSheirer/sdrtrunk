package ua.in.smartjava.audio.squelch;

import ua.in.smartjava.sample.Listener;

public interface ISquelchStateProvider
{
	public void setSquelchStateListener( Listener<SquelchState> listener );
	public void removeSquelchStateListener();
}
