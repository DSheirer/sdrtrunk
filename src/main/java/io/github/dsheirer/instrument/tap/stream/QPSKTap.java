package io.github.dsheirer.instrument.tap.stream;

import io.github.dsheirer.instrument.tap.TapType;

public class QPSKTap extends ComplexTap
{
	public QPSKTap( String name, int delay, float sampleRateRatio )
	{
		super( TapType.STREAM_QPSK, name, delay, sampleRateRatio );
	}
}
