package dsp.psk;

import sample.Broadcaster;
import sample.Listener;
import sample.complex.ComplexSample;
import dsp.symbol.Dibit;

public class QPSKStarSlicer implements Listener<ComplexSample>
{
	private Broadcaster<Dibit> mBroadcaster = new Broadcaster<Dibit>();

	/**
	 * Slices a ComplexSample representing a phase shifted symbol according to
	 * the following constellation pattern:
	 * 
	 *    11 | 01
	 *    ---|---
	 *    10 | 00
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
	public void receive( ComplexSample sample )
	{
		mBroadcaster.broadcast( decide( sample ) );
	}
	
	public static Dibit decide( ComplexSample sample )
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
