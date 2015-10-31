package dsp.filter.dc;

import sample.Listener;
import sample.real.RealBuffer;

public class IIRSinglePoleDCRemovalFilter_RB implements Listener<RealBuffer>
{
	private float mAlpha;
	private float mPreviousInput = 0.0f;
	private float mPreviousOutput = 0.0f;
	private Listener<RealBuffer> mListener;

	/**
	 * IIR single-pole DC removal filter, as described by J M de Freitas in
	 * 29Jan2007 paper at: 
	 * 
	 * http://www.mathworks.com/matlabcentral/fileexchange/downloads/72134/download
	 * 
	 * @param alpha 0.0 - 1.0 float - the closer alpha is to unity, the closer
	 * the cutoff frequency is to DC.
	 */
	public IIRSinglePoleDCRemovalFilter_RB( float alpha )
	{
		mAlpha = alpha;
	}
	
	@Override
	public void receive( RealBuffer buffer )
	{
		if( mListener != null )
		{
			mListener.receive( filter( buffer ) );
		}
	}
	
	public RealBuffer filter( RealBuffer buffer )
	{
		float[] samples = buffer.getSamples();
		
		for( int x = 0; x < samples.length; x++ )
		{
			samples[ x ] = filter( samples[ x ] );
		}

		return buffer;
	}

    public float filter( float sample )
    {
		float currentOutput = ( sample - mPreviousInput ) + 
							  ( mAlpha * mPreviousOutput );
		
		mPreviousInput = sample;
		mPreviousOutput = currentOutput;
		
		return currentOutput;
    }

	public void reset()
	{
		mPreviousInput = 0.0f;
		mPreviousOutput = 0.0f;
	}
	
	public void setListener( Listener<RealBuffer> listener )
	{
		mListener = listener;
	}
}
