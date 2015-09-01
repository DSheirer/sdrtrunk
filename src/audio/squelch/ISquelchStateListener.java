package audio.squelch;

import sample.Listener;

public interface ISquelchStateListener
{
	public Listener<SquelchState> getSquelchStateListener();
}
