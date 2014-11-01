package dsp.afc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.real.RealSampleListener;
import source.tuner.FrequencyChangeEvent;
import source.tuner.FrequencyChangeEvent.Attribute;
import source.tuner.FrequencyChangeListener;
import buffer.FloatAveragingBuffer;

public class AutomaticFrequencyControl implements RealSampleListener, 
												  FrequencyChangeListener
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( AutomaticFrequencyControl.class );
	
	private static float LARGE_ERROR = 0.300f;
	private static float MEDIUM_ERROR = 0.150f;
	private static float SMALL_ERROR = 0.030f;
	private static float FINE_ERROR = 0.015f;

	private static int LARGE_FREQUENCY_CORRECTION = 1500; //Hertz
	private static int MEDIUM_FREQUENCY_CORRECTION = 500;
	private static int SMALL_FREQUENCY_CORRECTION = 100;
	private static int FINE_FREQUENCY_CORRECTION = 50;
	
	private static int SAMPLE_THRESHOLD = 15000;
	private Mode mMode = Mode.FAST;
	
	private FloatAveragingBuffer mBuffer = 
			new FloatAveragingBuffer( SAMPLE_THRESHOLD );

	private int mSampleCounter = 0;
	private int mSkipCounter = 0;
	private int mSkipThreshold = 4;
	private int mErrorCorrection = 0;
	private long mCurrentFrequency;
	private int mMaximumCorrection;
	private FrequencyChangeListener mListener;
	
	public enum Mode
	{
		NORMAL,FAST;
	}
	
	public AutomaticFrequencyControl( FrequencyChangeListener listener,
									  int maximumCorrection )
	{
		mListener = listener;
		mMaximumCorrection = maximumCorrection;
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
			mListener.frequencyChanged( 
				new FrequencyChangeEvent( Attribute.FREQUENCY_ERROR, 
										  mErrorCorrection ) );
		}
	}

	@Override
    public void receive( float sample )
    {
		if( mMode == Mode.FAST )
		{
			float average = mBuffer.get( sample );
			
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

				float average = mBuffer.get( sample );
				
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
			correction = -LARGE_FREQUENCY_CORRECTION;
			mMode = Mode.FAST;
		}
		else if( average > MEDIUM_ERROR )
		{
			correction = -MEDIUM_FREQUENCY_CORRECTION;
			mMode = Mode.FAST;
		}
		else if( average > SMALL_ERROR )
		{
			correction = -SMALL_FREQUENCY_CORRECTION;
			mMode = Mode.NORMAL;
		}
		else if( average > FINE_ERROR )
		{
			correction = -FINE_FREQUENCY_CORRECTION;
			mMode = Mode.NORMAL;
		}
		else if( average < -LARGE_ERROR )
		{
			correction = LARGE_FREQUENCY_CORRECTION;
			mMode = Mode.FAST;
		}
		else if( average < -MEDIUM_ERROR )
		{
			correction = MEDIUM_FREQUENCY_CORRECTION;
			mMode = Mode.FAST;
		}
		else if( average < -SMALL_ERROR )
		{
			correction = SMALL_FREQUENCY_CORRECTION;
			mMode = Mode.NORMAL;
		}
		else if( average < -FINE_ERROR )
		{
			correction = FINE_FREQUENCY_CORRECTION;
			mMode = Mode.NORMAL;
		}

		if( correction != 0 )
		{
			mErrorCorrection -= correction;
			
			if( Math.abs( mErrorCorrection ) > mMaximumCorrection )
			{
				mErrorCorrection = ( mErrorCorrection < 0 ) ? 
										-mMaximumCorrection : 
										 mMaximumCorrection;
			}
			
			dispatch();
		}
		else
		{
			mMode = Mode.NORMAL;
		}
	}

	@Override
    public void frequencyChanged( FrequencyChangeEvent event )
    {
		switch( event.getAttribute() )
		{
			case FREQUENCY:
				int frequency = (int)event.getValue();
				
				if( mCurrentFrequency != frequency )
				{
					mCurrentFrequency = frequency;
					
					reset();
				}
			case SAMPLE_RATE_ERROR:
				break;
			default:
				break;
		}
    }
}
