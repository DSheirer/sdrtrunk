package channel.state;

import sample.Listener;

public interface IDecoderStateEventListener
{
	public Listener<DecoderStateEvent> getDecoderStateListener();
}
