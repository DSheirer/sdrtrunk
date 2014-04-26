package source;

import sample.Provider;

public abstract class FloatSource extends Source implements Provider<Float>
{
	public FloatSource( String name )
	{
		super( name, SampleType.FLOAT );
	}
}
