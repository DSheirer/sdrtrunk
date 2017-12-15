package ua.in.smartjava.sample.real;

import ua.in.smartjava.sample.Listener;

public interface IUnFilteredRealBufferProvider
{
	public void setUnFilteredRealBufferListener( Listener<RealBuffer> listener );
	public void removeUnFilteredRealBufferListener();
}
