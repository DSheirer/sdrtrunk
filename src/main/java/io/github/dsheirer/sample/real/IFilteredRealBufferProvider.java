package io.github.dsheirer.sample.real;

import io.github.dsheirer.sample.Listener;

public interface IFilteredRealBufferProvider
{
	public void setFilteredRealBufferListener( Listener<RealBuffer> listener );
	public void removeFilteredRealBufferListener();
}
