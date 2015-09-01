package module.decode.state;

import sample.Listener;

public interface IDecoderStateEventListener
{
	public Listener<DecoderStateEvent> getDecoderStateListener();
}
