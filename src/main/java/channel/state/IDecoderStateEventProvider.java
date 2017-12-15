package channel.state;

import sample.Listener;

public interface IDecoderStateEventProvider
{
	public void setDecoderStateListener( Listener<DecoderStateEvent> listener );
	public void removeDecoderStateListener();
}
