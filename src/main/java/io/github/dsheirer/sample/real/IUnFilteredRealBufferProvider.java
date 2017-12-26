package io.github.dsheirer.sample.real;

import io.github.dsheirer.sample.Listener;

public interface IUnFilteredRealBufferProvider
{
	public void setUnFilteredRealBufferListener( Listener<RealBuffer> listener );
	public void removeUnFilteredRealBufferListener();
}
