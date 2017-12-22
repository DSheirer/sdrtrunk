package io.github.dsheirer.dsp.filter;

import io.github.dsheirer.sample.Listener;

import java.util.BitSet;

public class LTRPulseShapingFilter implements Listener<Boolean>
{
	private int[][] mIndexes = { { 0, 1, 2, 3, 4 },
								 { 1, 2, 3, 4, 0 },
								 { 2, 3, 4, 0, 1 },
								 { 3, 4, 0, 1, 2 },
								 { 4, 0, 1, 2, 3 } };
	
	private BitSet mBuffer;
	private int mBufferPointer = 0;
	private Listener<Boolean> mListener;

	/**
	 * Specialized pulse shaping filter that detects a weak 0 or 1 pulse that
	 * is characterized by two consecutive values, true or false, within a 
	 * sequence of five boolean values where the remainder are the opposite 
	 * value, and elongates that weak pulse to be three consecutive values, 
	 * which is enough to cause the slicer to render a correct value.
	 */
	public LTRPulseShapingFilter()
	{
		mBuffer = new BitSet();
		mBuffer.clear();
	}
	
	@Override
    public void receive( Boolean current )
    {
		boolean previous = mBuffer.get( mBufferPointer );
		
		if( current )
		{
			mBuffer.set( mBufferPointer );
		}
		else
		{
			mBuffer.clear( mBufferPointer );
		}
		
		mBufferPointer++;
		
		if( mBufferPointer >= 5 )
		{
			mBufferPointer = 0;
		}
		
		/* Check for weak positive pulse*/
		if( mBuffer.cardinality() == 2 )
		{
			if( mBuffer.get( mIndexes[ mBufferPointer ][ 1 ] ) && 
				mBuffer.get( mIndexes[ mBufferPointer ][ 2 ] ) )
			{
				mBuffer.set( mIndexes[ mBufferPointer][ 3 ] );
			}
		}
		/* Or, a weak negative pulse */
		else if( mBuffer.cardinality() == 3 )
		{
			if( !mBuffer.get( mIndexes[ mBufferPointer][ 1 ] ) && 
				!mBuffer.get( mIndexes[ mBufferPointer][ 2 ] ) )
			{
				mBuffer.clear( mIndexes[ mBufferPointer][ 3 ] );
			}
		}
		
		if( mListener != null )
		{
			mListener.receive( previous );
		}
    }
	
	public String getIndexSet( int pointer )
	{
		StringBuilder sb = new StringBuilder();
		sb.append( "[" );
		
		for( int x = 0; x < 5; x++ )
		{
			sb.append( mIndexes[ pointer ][ x ] );
			sb.append( "," );
		}

		sb.append( "[" );
		
		return sb.toString();
	}
	
	public void setListener( Listener<Boolean> listener )
	{
		mListener = listener;
	}
	
	public void removeListener( Listener<Boolean> listener )
	{
		mListener = null;
	}
}
