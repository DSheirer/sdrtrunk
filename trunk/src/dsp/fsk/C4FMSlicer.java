package dsp.fsk;

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

	private Broadcaster<C4FMSymbol> mBroadcaster = new Broadcaster<C4FMSymbol>();
	
	/**
	 * Primary method for receiving output from the C4FMSymbolFilter.  Slices
	 * (converts) the filtered sample value into a C4FMSymbol decision. 
	 */
	@Override
    public void receive( float sample )
    {
		if( sample > THRESHOLD )
		{
			dispatch( C4FMSymbol.SYMBOL_PLUS_3 );
		}
		else if( sample > 0 )
		{
			dispatch( C4FMSymbol.SYMBOL_PLUS_1 );
		}
		else if( sample > -THRESHOLD )
		{
			dispatch( C4FMSymbol.SYMBOL_MINUS_1 );
		}
		else
		{
			dispatch( C4FMSymbol.SYMBOL_MINUS_3 );
		}
    }

	/**
	 * Dispatches the symbol decision to any registered listeners
	 */
	private void dispatch( C4FMSymbol symbol )
	{
		mBroadcaster.receive( symbol );
	}

	/**
	 * Registers the listener to receive the normal (non-inverted) C4FM symbol
	 * stream.
	 */
    public void addListener( Listener<C4FMSymbol> listener )
    {
		mBroadcaster.addListener( listener );
    }

	/**
	 * Removes the listener
	 */
    public void removeListener( Listener<C4FMSymbol> listener )
    {
    	mBroadcaster.removeListener( listener );
    }
}
