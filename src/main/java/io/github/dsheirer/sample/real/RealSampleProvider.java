package io.github.dsheirer.sample.real;

public interface RealSampleProvider
{
	public void setListener( RealSampleListener listener );
	
	public void removeListener( RealSampleListener listener );
}
