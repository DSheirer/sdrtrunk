package dsp.gain;

import sample.Listener;
import sample.complex.Complex;

public class ComplexGain implements Listener<Complex>
{
	private float mGain;
	private Listener<Complex> mListener;

	public ComplexGain( float gain )
	{
		mGain = gain;
	}
	
	@Override
	public void receive( Complex sample )
	{
		if( mListener != null )
		{
			sample.multiply( mGain );
			
			mListener.receive( sample );
		}
	}

	public void setListener( Listener<Complex> listener )
	{
		mListener = listener;
	}
}
