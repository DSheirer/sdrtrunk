package module.decode.p25;

import dsp.symbol.Dibit;
import sample.Broadcaster;
import sample.Listener;
import sample.real.RealSampleListener;

/**
 * C4FM slicer to convert the output stream of the C4FMSymbolFilter into a 
 * stream of C4FM symbols.  
 * 
 * Supports registering listener(s) to receive normal and/or inverted symbol
 * output streams.
 */
public class C4FMSlicer implements RealSampleListener
{
	private static final float THRESHOLD = 2.0f;

	private Broadcaster<Dibit> mBroadcaster = new Broadcaster<Dibit>();
	
	public void dispose()
	{
		mBroadcaster.dispose();
		mBroadcaster = null;
	}
	
	/**
	 * Primary method for receiving output from the C4FMSymbolFilter.  Slices
	 * (converts) the filtered sample value into a C4FMSymbol decision. 
	 */
	@Override
    public void receive( float sample )
    {
		if( sample > THRESHOLD )
		{
			dispatch( Dibit.D01_PLUS_3 );
		}
		else if( sample > 0 )
		{
			dispatch( Dibit.D00_PLUS_1 );
		}
		else if( sample > -THRESHOLD )
		{
			dispatch( Dibit.D10_MINUS_1 );
		}
		else
		{
			dispatch( Dibit.D11_MINUS_3 );
		}
    }

	/**
	 * Dispatches the symbol decision to any registered listeners
	 */
	private void dispatch( Dibit symbol )
	{
		mBroadcaster.receive( symbol );
	}

	/**
	 * Registers the listener to receive the normal (non-inverted) C4FM symbol
	 * stream.
	 */
    public void addListener( Listener<Dibit> listener )
    {
		mBroadcaster.addListener( listener );
    }

	/**
	 * Removes the listener
	 */
    public void removeListener( Listener<Dibit> listener )
    {
    	mBroadcaster.removeListener( listener );
    }
}
