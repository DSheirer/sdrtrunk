package source;

import sample.Provider;
import sample.simplex.SimplexBuffer;


public abstract class SimplexSource extends Source implements Provider<SimplexBuffer>
{
	public SimplexSource( String name )
	{
		super( name, SampleType.FLOAT );
	}
}
