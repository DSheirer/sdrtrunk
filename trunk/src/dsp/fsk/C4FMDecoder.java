package dsp.fsk;

import instrument.Instrumentable;
import instrument.tap.Tap;
import instrument.tap.stream.FloatTap;

import java.util.ArrayList;
import java.util.List;

import sample.real.RealSampleListener;

public class C4FMDecoder implements RealSampleListener, Instrumentable
{
	private static final String TAP_C4FM_INPUT = "C4FM Decoder Input";
	private static final String TAP_SYMBOL_FILTER_OUTPUT = "C4FM Symbol Filter Output";

	private ArrayList<Tap> mTaps;
	private FloatTap mInputTap;
	private FloatTap mSymbolFilterOutputTap;
	
	private C4FMSymbolFilter mSymbolFilter = new C4FMSymbolFilter();
	
	private RealSampleListener mInput;
	
	public C4FMDecoder()
	{
		mInput = mSymbolFilter;
	}

	@Override
    public void receive( float sample )
    {
		mInput.receive( sample );
    }

	@Override
    public List<Tap> getTaps()
    {
		if( mTaps == null )
		{
			mTaps = new ArrayList<Tap>();

			FloatTap floatTap = new FloatTap( TAP_C4FM_INPUT, 0, 1.0f );
			mTaps.add( floatTap );

			FloatTap symbolTap = new FloatTap( TAP_SYMBOL_FILTER_OUTPUT, 10, 0.1f );
			mTaps.add(  symbolTap );
		}
		
	    return mTaps;
    }

	@Override
    public void addTap( Tap tap )
    {
		switch( tap.getName() )
		{
			case TAP_C4FM_INPUT:
				mInputTap = (FloatTap)tap; 
				mInput = mInputTap;
				mInputTap.setListener( mSymbolFilter );
				break;
			case TAP_SYMBOL_FILTER_OUTPUT:
				mSymbolFilterOutputTap = (FloatTap)tap;
				mSymbolFilter.setListener( mSymbolFilterOutputTap );
				break;
		}
    }

	@Override
    public void removeTap( Tap tap )
    {
		switch( tap.getName() )
		{
			case TAP_C4FM_INPUT:
				mInput = mSymbolFilter;
				mInputTap = null;
				break;
			case TAP_SYMBOL_FILTER_OUTPUT:
				mSymbolFilter.removeListener( mSymbolFilterOutputTap );
				mSymbolFilterOutputTap = null;
				break;
		}
    }
}
