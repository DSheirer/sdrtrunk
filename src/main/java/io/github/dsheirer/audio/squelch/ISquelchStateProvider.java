package io.github.dsheirer.audio.squelch;

import io.github.dsheirer.sample.Listener;

public interface ISquelchStateProvider
{
	public void setSquelchStateListener( Listener<SquelchState> listener );
	public void removeSquelchStateListener();
}
