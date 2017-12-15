package ua.in.smartjava.source;

import ua.in.smartjava.sample.Provider;
import ua.in.smartjava.sample.SampleType;
import ua.in.smartjava.sample.complex.ComplexBuffer;

public abstract class ComplexSource extends Source implements Provider<ComplexBuffer>
{
	public ComplexSource()
	{
		super( SampleType.COMPLEX );
	}
}
