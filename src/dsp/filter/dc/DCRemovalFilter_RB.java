package dsp.filter.dc;

import sample.Listener;
import sample.real.RealBuffer;

public abstract class DCRemovalFilter_RB implements Listener<RealBuffer>
{
	protected Listener<RealBuffer> mListener;
	
	public DCRemovalFilter_RB()
	{
		
	}
	
	public void setListener( Listener<RealBuffer> listener )
	{
		mListener = listener;
	}
	
	public void removeListener()
	{
		mListener = null;
	}
	
	public void dispose()
	{
		mListener = null;
	}
	
	public abstract void reset();
}
