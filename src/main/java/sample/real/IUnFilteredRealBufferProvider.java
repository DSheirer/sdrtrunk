package sample.real;

import sample.Listener;

public interface IUnFilteredRealBufferProvider
{
	public void setUnFilteredRealBufferListener( Listener<RealBuffer> listener );
	public void removeUnFilteredRealBufferListener();
}
