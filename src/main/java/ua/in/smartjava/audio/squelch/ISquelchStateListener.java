package ua.in.smartjava.audio.squelch;

import ua.in.smartjava.sample.Listener;

public interface ISquelchStateListener
{
	public Listener<SquelchState> getSquelchStateListener();
}
