package sample.decimator;

import sample.real.RealSampleListener;


public class RealDecimator implements RealSampleListener
{
	private RealSampleListener mListener;
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
    public void receive( float sample )
    {
		mCounter++;
		
		if( mCounter >= mDecimationRate )
		{
			mListener.receive( sample );

			mCounter = 0;
		}
    }
	
	/**
	 * Sets the decimated output listener
	 */
	public void setListener( RealSampleListener listener )
	{
		mListener = listener;
	}
}
