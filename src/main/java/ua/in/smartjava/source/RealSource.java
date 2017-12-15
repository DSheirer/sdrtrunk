package ua.in.smartjava.source;

import ua.in.smartjava.sample.Provider;
import ua.in.smartjava.sample.SampleType;
import ua.in.smartjava.sample.real.RealBuffer;


public abstract class RealSource extends Source implements Provider<RealBuffer>
{
	public RealSource()
	{
		super( SampleType.REAL );
	}
}
