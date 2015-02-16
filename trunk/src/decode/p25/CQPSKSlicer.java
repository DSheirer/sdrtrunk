package decode.p25;

import sample.Broadcaster;
import sample.Listener;
import sample.complex.ComplexSample;
import dsp.symbol.Dibit;

public class CQPSKSlicer implements Listener<ComplexSample>
{
	private Broadcaster<Dibit> mBroadcaster = new Broadcaster<Dibit>();
	
	public CQPSKSlicer()
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

	/**
	 * Receives a complex sample and maps the value into one of four quadrants
	 * corresponding to the CQPSK LSM pattern.
	 */
	@Override
	public void receive( ComplexSample sample )
	{
		if( sample.real() > 0 )
		{
			if( sample.imaginary() > 0 )
			{
				mBroadcaster.broadcast( Dibit.D10_MINUS_1 );
			}
			else
			{
				mBroadcaster.broadcast( Dibit.D11_MINUS_3 );
			}
		}
		else
		{
			if( sample.imaginary() > 0 )
			{
				mBroadcaster.broadcast( Dibit.D00_PLUS_1 );
			}
			else
			{
				mBroadcaster.broadcast( Dibit.D01_PLUS_3 );
			}
		}
	}
}
