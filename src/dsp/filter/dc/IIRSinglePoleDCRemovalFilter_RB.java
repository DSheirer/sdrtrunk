package dsp.filter.dc;

import sample.real.RealBuffer;

public class IIRSinglePoleDCRemovalFilter_RB extends DCRemovalFilter_RB
{
	private float mAlpha;
	private float mPreviousInput = 0.0f;
	private float mPreviousOutput = 0.0f;

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
		float[] samples = buffer.getSamples();
		
		for( int x = 0; x < samples.length; x++ )
		{
			samples[ x ] = filter( samples[ x ] );
		}
		
		if( mListener != null )
		{
			mListener.receive( buffer );
		}
	}

    public float filter( float sample )
    {
		float currentOutput = ( sample - mPreviousInput ) + 
							  ( mAlpha * mPreviousOutput );
		
		mPreviousInput = sample;
		mPreviousOutput = currentOutput;
		
		return currentOutput;
    }

	@Override
	public void reset()
	{
		mPreviousInput = 0.0f;
		mPreviousOutput = 0.0f;
	}
}
