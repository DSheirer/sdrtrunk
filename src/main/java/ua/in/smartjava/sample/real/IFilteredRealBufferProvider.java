package ua.in.smartjava.sample.real;

import ua.in.smartjava.sample.Listener;

public interface IFilteredRealBufferProvider
{
	public void setFilteredRealBufferListener( Listener<RealBuffer> listener );
	public void removeFilteredRealBufferListener();
}
