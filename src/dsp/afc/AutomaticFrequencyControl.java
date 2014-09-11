package dsp.afc;

import sample.Listener;
import source.tuner.FrequencyChangeListener;
import buffer.FloatAveragingBuffer;

public class AutomaticFrequencyControl implements Listener<Float>, 
												  FrequencyChangeListener
{
	private static float LARGE_ERROR = 10000.0f;
	private static float MEDIUM_ERROR = 5000.0f;
	private static float SMALL_ERROR = 1000.0f;
	private static float FINE_ERROR = 500.0f;
	private static int SAMPLE_THRESHOLD = 15000;
	private Mode mMode = Mode.FAST;
	
	private FloatAveragingBuffer mBuffer = 
			new FloatAveragingBuffer( SAMPLE_THRESHOLD );

	private int mSampleCounter = 0;
	private int mSkipCounter = 0;
	private int mSkipThreshold = 4;
	private int mErrorCorrection = 0;
	private long mCurrentFrequency;
	private int mCurrentSampleRate;
	private FrequencyErrorListener mListener;
	
	public enum Mode
	{
		NORMAL,FAST;
	}
	
	public AutomaticFrequencyControl( FrequencyErrorListener listener )
	{
		mListener = listener;
	}
	
	public void dispose()
	{
		mListener = null;
		mBuffer = null;
	}
	
	public int getErrorCorrection()
	{
		return mErrorCorrection;
	}
	
	public void reset()
	{
		mErrorCorrection = 0;
		mMode = Mode.FAST;
		dispatch();
	}
	
	private void dispatch()
	{
		if( mListener != null )
		{
			mListener.setError( mErrorCorrection );
		}
	}

	@Override
    public void receive( Float sample )
    {
		if( mMode == Mode.FAST )
		{
			float average = mBuffer.get( sample.floatValue() );
			
			mSampleCounter++;
			
			if( mSampleCounter >= SAMPLE_THRESHOLD )
			{
				mSampleCounter = 0;
				
				update( average );
			}
		}
		else
		{
			mSkipCounter++;
			
			if( mSkipCounter >= mSkipThreshold )
			{
				mSkipCounter = 0;

				float average = mBuffer.get( sample.floatValue() );
				
				mSampleCounter++;
				
				if( mSampleCounter >= SAMPLE_THRESHOLD )
				{
					mSampleCounter = 0;
					
					update( average );
				}
			}
		}
    }
	
	private void update( float average )
	{
		int correction = 0;
		
		if( average > LARGE_ERROR )
		{
			correction = -1000;
			mMode = Mode.FAST;
		}
		else if( average > MEDIUM_ERROR )
		{
			correction = -500;
			mMode = Mode.FAST;
		}
		else if( average > SMALL_ERROR )
		{
			correction = -100;
			mMode = Mode.NORMAL;
		}
		else if( average > FINE_ERROR )
		{
			correction = -50;
			mMode = Mode.NORMAL;
		}
		else if( average < -LARGE_ERROR )
		{
			correction = 1000;
			mMode = Mode.FAST;
		}
		else if( average < -MEDIUM_ERROR )
		{
			correction = 500;
			mMode = Mode.FAST;
		}
		else if( average < -SMALL_ERROR )
		{
			correction = 100;
			mMode = Mode.NORMAL;
		}
		else if( average < -FINE_ERROR )
		{
			correction = 50;
			mMode = Mode.NORMAL;
		}

		if( correction != 0 )
		{
			mErrorCorrection -= correction;
			dispatch();
		}
		else
		{
			mMode = Mode.NORMAL;
		}
	}

	@Override
    public void frequencyChanged( long frequency, int bandwidth )
    {
		if( mCurrentFrequency != frequency || mCurrentSampleRate != bandwidth )
		{
			mCurrentFrequency = frequency;
			mCurrentSampleRate = bandwidth;
			
			reset();
		}
    }
}
