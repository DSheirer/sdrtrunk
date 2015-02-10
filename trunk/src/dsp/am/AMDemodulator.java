package dsp.am;

import sample.Listener;
import sample.complex.ComplexSample;
import sample.real.RealSampleListener;
import sample.real.RealSampleProvider;

public class AMDemodulator implements Listener<ComplexSample>, RealSampleProvider
{
    private RealSampleListener mListener;

    public AMDemodulator()
    {
    }
    
    @Override
    public void receive( ComplexSample sample )
    {
        float demodulated = (float)Math.sqrt( ( sample.real() * 
				sample.real() ) +
			  ( sample.imaginary() * 
			    sample.imaginary() ) ); 

        if( mListener != null )
        {
        	mListener.receive( demodulated * 500.0f );
        }
    }

    /**
     * Adds a listener to receive demodulated samples
     */
    public void setListener( RealSampleListener listener )
    {
    	mListener = listener;
    }

    /**
     * Removes a listener from receiving demodulated samples
     */
    public void removeListener( RealSampleListener listener )
    {
    	mListener = null;
    }
}
