package dsp.gain;

import sample.Listener;
import sample.complex.ComplexSample;

public class ComplexGain implements Listener<ComplexSample>
{
	private float mGain;
	private Listener<ComplexSample> mListener;

	public ComplexGain( float gain )
	{
		mGain = gain;
	}
	
	@Override
	public void receive( ComplexSample sample )
	{
		if( mListener != null )
		{
			sample.multiply( mGain );
			
			mListener.receive( sample );
		}
	}

	public void setListener( Listener<ComplexSample> listener )
	{
		mListener = listener;
	}
}
