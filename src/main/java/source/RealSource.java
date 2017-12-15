package source;

import sample.Provider;
import sample.SampleType;
import sample.real.RealBuffer;


public abstract class RealSource extends Source implements Provider<RealBuffer>
{
	public RealSource()
	{
		super( SampleType.REAL );
	}
}
