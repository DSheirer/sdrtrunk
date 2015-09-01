package module.decode.state;

import sample.Listener;

public interface IDecoderStateEventProvider
{
	public void setDecoderStateListener( Listener<DecoderStateEvent> listener );
	public void removeDecoderStateListener();
}
