package sample.real;

import sample.Listener;

public interface IFilteredRealBufferProvider
{
	public void setFilteredRealBufferListener( Listener<RealBuffer> listener );
	public void removeFilteredRealBufferListener();
}
