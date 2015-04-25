package dsp.psk;

import sample.Broadcaster;
import sample.Listener;
import sample.complex.ComplexSample;
import dsp.symbol.Dibit;

public class QPSKPolarSlicer implements Listener<ComplexSample>
{
	private Broadcaster<Dibit> mBroadcaster = new Broadcaster<Dibit>();
	
	/**
	 * Slices a ComplexSample representing a phase shifted symbol according to
	 * the following constellation pattern:
	 * 
	 *    \ 00 /
	 *     \  /
	 *   11 \/ 10
	 *      /\
	 *     /  \
	 *    / 11 \
	 */
	public QPSKPolarSlicer()
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
		if( sample.inPhaseAbsolute() > sample.quadratureAbsolute() )
		{
			if( sample.inphase() > 0 )
			{
				return Dibit.D10_MINUS_1;
			}
			else
			{
				return Dibit.D01_PLUS_3;
			}
		}
		else
		{
			if( sample.quadrature() > 0 )
			{
				return Dibit.D00_PLUS_1;
			}
			else
			{
				return Dibit.D11_MINUS_3;
			}
		}
	}
	
}
