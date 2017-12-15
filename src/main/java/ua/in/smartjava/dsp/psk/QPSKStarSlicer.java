package ua.in.smartjava.dsp.psk;

import ua.in.smartjava.sample.Broadcaster;
import ua.in.smartjava.sample.Listener;
import ua.in.smartjava.sample.complex.Complex;
import ua.in.smartjava.dsp.symbol.Dibit;

public class QPSKStarSlicer implements Listener<Complex>
{
	private Broadcaster<Dibit> mBroadcaster = new Broadcaster<Dibit>();

	/**
	 * Slices a ComplexSample representing a phase shifted symbol according to
	 * the following constellation pattern:
	 * 
	 *    00 | 01
	 *    ---|---
	 *    10 | 11
	 */
	public QPSKStarSlicer()
	{
	}
	
	public void dispose()
	{
		mBroadcaster.dispose();
		mBroadcaster = null;
	}

	public void addListener( Listener<Dibit> listener )
	{
		mBroadcaster.addListener( listener );
	}

	public void removeListener( Listener<Dibit> listener )
	{
		mBroadcaster.removeListener( listener );
	}

	@Override
	public void receive( Complex sample )
	{
		mBroadcaster.broadcast( decide( sample ) );
	}
	
	public static Dibit decide( Complex sample )
	{
		if( sample.inphase() > 0 )
		{
			return sample.quadrature() > 0 ? Dibit.D00_PLUS_1 : Dibit.D10_MINUS_1;
		}
		else
		{
			return sample.quadrature() > 0 ? Dibit.D01_PLUS_3 : Dibit.D11_MINUS_3;
		}
	}
	
}
