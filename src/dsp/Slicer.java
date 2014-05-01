package dsp;

import instrument.tap.Tap;
import instrument.tap.stream.SymbolEventTap;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import sample.Listener;
import dsp.fsk.SymbolEvent;
import dsp.fsk.SymbolEvent.Shift;

/**
 * Symbol slicer with auto-aligning baud timing
 */
public class Slicer implements Listener<Boolean>
{
	public enum Output{ NORMAL, INVERTED };

	private BitSet mBitSet = new BitSet();
	private int mSymbolLength;
	private int mDecisionThreshold;
	private int mSampleCounter;
	private SymbolEventTap mSymbolEventTap;
	
	private boolean mNormalOutput = true;
	private Listener<Boolean> mListener;
	
	public Slicer( Output output, int samplesPerSymbol )
	{
		mNormalOutput = ( output == Output.NORMAL );
		
		mSymbolLength = samplesPerSymbol;
		
		/* Round up */
		mDecisionThreshold = (int)( mSymbolLength / 2 ) + ( mSymbolLength % 2 );
	}
	
	public void receive( Boolean sample )
	{
		if( mSampleCounter >= 0 )
		{
			if( sample )
			{
				mBitSet.set( mSampleCounter );
			}
			else
			{
				mBitSet.clear( mSampleCounter );
			}
		}
		
		mSampleCounter++;
		
		if( mSampleCounter >= mSymbolLength )
		{
			boolean decision = mBitSet.cardinality() >= mDecisionThreshold;
			
			send( decision );

			/* Shift timing left if the left bit in the bitset is opposite 
			 * the decision and the right bit is the same */
			if( ( mBitSet.get( 0 ) ^ decision ) && 
				( !( mBitSet.get( mSymbolLength - 1 ) ^ decision ) ) )
			{
				sendTapEvent( mBitSet, Shift.LEFT, decision );
				
				reset();
				
				mSampleCounter--;
			}
			/* Shift timing right if the left bit is the same as the 
			 * decision and the right bit is opposite */
			else if( ( !( mBitSet.get( 0 ) ^ decision ) ) && 
					 ( mBitSet.get( mSymbolLength - 1 ) ^ decision ) )
			{
				sendTapEvent( mBitSet, Shift.RIGHT, decision );
				
				/* Last bit from previous symbol to pre-fill next symbol */
				boolean previousSoftBit = mBitSet.get( mSymbolLength - 1 );
				
				reset();
				
				if( previousSoftBit )
				{
					mBitSet.set( 0 );
				}
				
				mSampleCounter++;
			}
			/* No shift */
			else
			{
				sendTapEvent( mBitSet, Shift.NONE, decision );
				
				reset();
			}
		}
	}
	
	/**
	 * Sends the bit decision to the listener
	 */
	private void send( boolean decision )
	{
		if( mListener != null )
		{
			mListener.receive( mNormalOutput ? decision : !decision );
		}
	}
	
	private void reset()
	{
		mBitSet.clear();
		mSampleCounter = 0;
	}
	
	public void setListener( Listener<Boolean> listener)
	{
		mListener = listener;
	}
	
	public void removeListener( Listener<Boolean> listener )
	{
		mListener = null;
	}

	/**
	 * Sends instrumentation tap event to all registered listeners 
	 */
	private void sendTapEvent( BitSet bitset, Shift shift, boolean decision )
	{
		if( mSymbolEventTap != null )
		{
			SymbolEvent event = 
					new SymbolEvent( bitset.get( 0, mSymbolLength ), 
									 mSymbolLength, 
									 decision, 
									 shift );
			
			mSymbolEventTap.receive( event );
		}
	}

    public void addTap( SymbolEventTap tap )
    {
    	mSymbolEventTap = tap;
    }

    public void removeTap()
    {
    	mSymbolEventTap = null;
    }
}
