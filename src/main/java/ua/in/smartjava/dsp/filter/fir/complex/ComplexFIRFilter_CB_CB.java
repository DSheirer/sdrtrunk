package ua.in.smartjava.dsp.filter.fir.complex;

import ua.in.smartjava.sample.Listener;
import ua.in.smartjava.sample.complex.ComplexBuffer;

public class ComplexFIRFilter_CB_CB extends ComplexFIRFilter 
				implements Listener<ComplexBuffer>
{
	private Listener<ComplexBuffer> mListener;
	
	/**
	 * Complex FIR Filter for processing complex buffers.  Wraps two real FIR
	 * filters for processing each of the inphase and quadrature samples.
	 * 
	 * @param coefficients - odd length symmetric ua.in.smartjava.filter taps
	 * @param gain - gain value to apply to each of the filtered samples
	 */
	public ComplexFIRFilter_CB_CB( float[] coefficients, float gain )
	{
		super( coefficients, gain );
	}
	
	public void dispose()
	{
		mListener = null;
	}

	@Override
	public void receive( ComplexBuffer buffer )
	{
		if( mListener != null )
		{
			float[] samples = buffer.getSamples();
			
			for( int x = 0; x < samples.length; x += 2 )
			{
				samples[ x ] = filterInphase( samples[ x ] );
				samples[ x + 1 ] = filterQuadrature( samples[ x + 1 ] );
			}
			
			mListener.receive( buffer );
		}
	}
	
	public void setListener( Listener<ComplexBuffer> listener )
	{
		mListener = listener;
	}
	
	public void removeListener()
	{
		mListener = null;
	}
}
