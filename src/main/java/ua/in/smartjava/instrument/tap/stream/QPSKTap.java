package ua.in.smartjava.instrument.tap.stream;

import ua.in.smartjava.instrument.tap.TapType;

public class QPSKTap extends ComplexTap
{
	public QPSKTap( String name, int delay, float sampleRateRatio )
	{
		super( TapType.STREAM_QPSK, name, delay, sampleRateRatio );
	}
}
