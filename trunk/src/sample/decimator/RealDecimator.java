package sample.decimator;

import sample.complex.ComplexSampleListener;


public class RealDecimator implements ComplexSampleListener
{
	private ComplexSampleListener mListener;
	private int mCounter = 0;
	private int mDecimationRate;

	/**
	 * Constructs a new Decimator object with the specified decimation rate.
	 */
	public RealDecimator( int rate )
	{
		mDecimationRate = rate;
	}
	
	public void dispose()
	{
		mListener = null;
	}

	/**
	 * Sets a new decimation rate.  Rate can be changed after this object
	 * is constructed.
	 */
	public synchronized void setDecimationRate( int rate )
	{
		mDecimationRate = rate;
	}

	/**
	 * Receives samples allowing only 1 of every (rate) sample to go on 
	 * to the registered listener
	 */
	@Override
    public void receive( float i, float q )
    {
		mCounter++;
		
		if( mCounter >= mDecimationRate )
		{
			mListener.receive( i, q );

			mCounter = 0;
		}
    }
	
	/**
	 * Sets the decimated output listener
	 */
	public void setListener( ComplexSampleListener listener )
	{
		mListener = listener;
	}
}
