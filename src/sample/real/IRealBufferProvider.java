package sample.real;

import sample.Listener;

public interface IRealBufferProvider
{
	public void setRealBufferListener( Listener<RealBuffer> listener );
	public void removeRealBufferListener();
}
