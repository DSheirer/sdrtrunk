package source;

import sample.Provider;
import sample.SampleType;

public abstract class FloatSource extends Source implements Provider<Float>
{
	public FloatSource( String name )
	{
		super( name, SampleType.REAL );
	}
}
