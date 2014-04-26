package source;

import java.util.List;

import sample.Provider;
import sample.complex.ComplexSample;

public abstract class ComplexSource extends Source 
				implements Provider<List<ComplexSample>>
{
	public ComplexSource( String name )
	{
		super( name, SampleType.COMPLEX );
	}
}
