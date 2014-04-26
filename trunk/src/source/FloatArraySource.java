package source;

import sample.Provider;
import source.Source.SampleType;

public abstract class FloatArraySource extends Source implements Provider<Float[]>
{
	public FloatArraySource( String name )
	{
		super( name, SampleType.FLOAT );
	}
}
