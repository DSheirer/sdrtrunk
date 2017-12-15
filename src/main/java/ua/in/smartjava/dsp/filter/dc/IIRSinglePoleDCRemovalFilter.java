package ua.in.smartjava.dsp.filter.dc;

import ua.in.smartjava.sample.real.RealSampleListener;

public class IIRSinglePoleDCRemovalFilter implements RealSampleListener
{
	private float mAlpha;
	private float mPreviousInput = 0.0f;
	private float mPreviousOutput = 0.0f;

	private RealSampleListener mListener;
	
	/**
	 * IIR single-pole DC removal ua.in.smartjava.filter, as described by J M de Freitas in
	 * 29Jan2007 paper at: 
	 * 
	 * http://www.mathworks.com/matlabcentral/fileexchange/downloads/72134/download
	 * 
	 * @param alpha 0.0 - 1.0 float - the closer alpha is to unity, the closer
	 * the cutoff frequency is to DC.
	 */
	public IIRSinglePoleDCRemovalFilter( float alpha )
	{
		mAlpha = alpha;
	}
	@Override
    public void receive( float currentInput )
    {
		float currentOutput = ( currentInput - mPreviousInput ) + 
							  ( mAlpha * mPreviousOutput );
		
		if( mListener != null )
		{
			mListener.receive( currentOutput );
		}
		
		mPreviousInput = currentInput;
		mPreviousOutput = currentOutput;
    }

	public void setListener( RealSampleListener listener )
	{
		mListener = listener;
	}
}
