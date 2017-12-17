package io.github.dsheirer.source;

import io.github.dsheirer.sample.Provider;
import io.github.dsheirer.sample.SampleType;
import io.github.dsheirer.sample.real.RealBuffer;


public abstract class RealSource extends Source implements Provider<RealBuffer>
{
	public RealSource()
	{
		super( SampleType.REAL );
	}
}
