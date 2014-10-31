package dsp.fsk;

import instrument.Instrumentable;
import instrument.tap.Tap;
import instrument.tap.stream.FloatTap;

import java.util.ArrayList;
import java.util.List;

import sample.real.RealSampleListener;

public class C4FMDecoder implements RealSampleListener, Instrumentable
{
	private final String TAP_C4FM_INPUT = "C4FM Input";
	private ArrayList<Tap> mTaps;
	private FloatTap mInputTap;
	
	public C4FMDecoder()
	{
	}

	@Override
    public List<Tap> getTaps()
    {
		if( mTaps == null )
		{
			mTaps = new ArrayList<Tap>();

			FloatTap floatTap = new FloatTap( TAP_C4FM_INPUT, 0, 1.0f );
			mTaps.add( floatTap );
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
				break;
		}
    }

	@Override
    public void removeTap( Tap tap )
    {
		switch( tap.getName() )
		{
			case TAP_C4FM_INPUT:
				mInputTap = null; 
				break;
		}
    }

	@Override
    public void receive( float t )
    {
		if( mInputTap != null )
		{
			mInputTap.receive( t );
		}
    }
}
