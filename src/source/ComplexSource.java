package source;

import sample.Provider;
import sample.SampleType;
import sample.complex.ComplexBuffer;

public abstract class ComplexSource extends Source implements Provider<ComplexBuffer>
{
	public ComplexSource()
	{
		super( SampleType.COMPLEX );
	}
}
