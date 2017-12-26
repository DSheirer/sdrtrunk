package io.github.dsheirer.audio.squelch;

import io.github.dsheirer.sample.Listener;

public interface ISquelchStateListener
{
	public Listener<SquelchState> getSquelchStateListener();
}
