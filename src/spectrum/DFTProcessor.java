/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package spectrum;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jtransforms.fft.FloatFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Buffer;
import sample.Listener;
import sample.SampleType;
import sample.complex.ComplexBuffer;
import source.tuner.frequency.FrequencyChangeEvent;
import source.tuner.frequency.IFrequencyChangeProcessor;
import spectrum.converter.DFTResultsConverter;
import controller.NamingThreadFactory;
import dsp.filter.Window;
import dsp.filter.Window.WindowType;

/**
 * Processes both complex samples or float samples and dispatches a float array
 * of DFT results, using configurable fft size and output dispatch timelines.  
 */
public class DFTProcessor implements Listener<ComplexBuffer>, 
									 IFrequencyChangeProcessor,
									 IDFTWidthChangeProcessor
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( DFTProcessor.class );

	private CopyOnWriteArrayList<DFTResultsConverter> mListeners =
			new CopyOnWriteArrayList<DFTResultsConverter>();

	private ArrayBlockingQueue<ComplexBuffer> mQueue = 
			new ArrayBlockingQueue<ComplexBuffer>( 200 );
							
	private ScheduledExecutorService mScheduler = Executors
			.newScheduledThreadPool( 1, new NamingThreadFactory( "spectrum dft" ) );	
	
	private DFTSize mDFTSize = DFTSize.FFT04096;
	private DFTSize mNewDFTSize = DFTSize.FFT04096;
	
	private double[] mWindow;
	
	/* The Cosine and Hanning windows seem to offer the best spectral display
	 * with minimal bin leakage/smearing */
	private WindowType mWindowType = Window.WindowType.HANNING;

	private FloatFFT_1D mFFT = new FloatFFT_1D( mDFTSize.getSize() );
	
	private int mFrameRate;
	private int mSampleRate;
	private int mFFTFloatsPerFrame;
	private float mNewFloatsPerFrame;
	private float mNewFloatResidual;
	private float[] mPreviousFrame = new float[ 8192 ];
	
	private float[] mCurrentBuffer;
	private int mCurrentBufferPointer = 0;
	
	private SampleType mSampleType;

	private AtomicBoolean mRunning = new AtomicBoolean();
	
	public DFTProcessor( SampleType sampleType )
	{
		setSampleType( sampleType );
		setFrameRate( 20 );
	}
	
	public void dispose()
	{
		stop();
		
		mListeners.clear();
		mQueue.clear();
		mWindow = null;
		mCurrentBuffer = null;
	}
	
	public WindowType getWindowType()
	{
		return mWindowType;
	}
	
	public void setWindowType( WindowType windowType )
	{
		mWindowType = windowType;
		
		if( mSampleType == SampleType.COMPLEX )
		{
			mWindow = Window.getWindow( mWindowType, 
										mDFTSize.getSize() * 2 );		
		}
		else
		{
			mWindow = Window.getWindow( mWindowType,
										mDFTSize.getSize() );		
		}
	}
	
	/**
	 * Sets the processor mode to Float or Complex, depending on the sample
	 * types that will be delivered for processing
	 */
	public void setSampleType( SampleType type )
	{
		mSampleType = type;
		setWindowType( mWindowType );
	}
	
	public SampleType getSampleType()
	{
		return mSampleType;
	}

	/**
	 * Queues an FFT size change request.  The scheduled executor will apply 
	 * the change when it runs.
	 */
	public void setDFTSize( DFTSize size )
	{
		mNewDFTSize = size;
	}
	
	public DFTSize getDFTSize()
	{
		return mDFTSize;
	}
	
	public int getFrameRate()
	{
		return mFrameRate;
	}
	
	public void setFrameRate( int framesPerSecond )
	{
		//TODO: make sure frame rate & sample rate sample requirement doesn't
		//expect overlap greater than the previous frame length
		
		if( framesPerSecond < 1 || framesPerSecond > 1000 )
		{
			throw new IllegalArgumentException( "DFTProcessor cannot run "
			+ "more than 1000 times per second -- requested setting:" 
					+ framesPerSecond );
		}

		mFrameRate = framesPerSecond;

		calculateConsumptionRate();

		restart();
	}
	
	public void start()
	{
		/**
         * Reset the scheduler
         */
		mScheduler = Executors.newScheduledThreadPool( 1, new NamingThreadFactory( "spectrum dft" ) );

		/**
		 * Schedule the DFT to run calculations at a fixed rate
		 */
		int initialDelay = 0;
		int period = (int)( 1000 / mFrameRate );
		TimeUnit unit = TimeUnit.MILLISECONDS;

		mScheduler.scheduleAtFixedRate( new DFTCalculationTask(), 
											initialDelay, period, unit );
	}
	
	public void stop()
	{
		/**
		 * Shutdown the scheduler and clear out any remaining tasks
		 */
        try
        {
    		mScheduler.shutdown();

            mScheduler.awaitTermination( 100, TimeUnit.MILLISECONDS );
        }
        catch ( InterruptedException e )
        {
        	/* Do nothing ... we're shutting down */
//	        mLog.error( "DFTProcessor - exception while awaiting shutdown of "
//	        		+ "calculation scheduler for reset", e );
        }
	}
	
	public void restart()
	{
		stop();
		start();
	}
	
	public int getCalculationsPerSecond()
	{
		return mFrameRate;
	}
	
	/**
	 * Places the sample into a transfer queue for future processing. 
	 */
	@Override
    public void receive( ComplexBuffer sampleBuffer )
    {
		if( !mQueue.offer( sampleBuffer ) )
		{
			mLog.error( "DFTProcessor - [" + mSampleType.toString()
						+ "] queue is full, purging queue, "
						+ "samples[" + sampleBuffer + "]" );

			mQueue.clear();
			mQueue.offer( sampleBuffer );
		}
    }
	
	private void getNextBuffer()
	{
		mCurrentBuffer = null;

		try
        {
			Buffer buffer = mQueue.take();
            mCurrentBuffer = buffer.getSamples();
        }
        catch ( InterruptedException e )
        {
        	mCurrentBuffer = null;
        }

		mCurrentBufferPointer = 0;
	}

	private float[] getSamples()
	{
		int remaining = (int)mFFTFloatsPerFrame;

		float[] currentFrame = new float[ remaining ];

		int currentFramePointer = 0;

		float integralFloatsToConsume = mNewFloatsPerFrame + mNewFloatResidual;
		
		int newFloatsToConsumeThisFrame = (int)integralFloatsToConsume;
		
		mNewFloatResidual = integralFloatsToConsume - newFloatsToConsumeThisFrame;
		
		/* If the number of required floats for the fft is greater than the
		 * consumption rate per frame, we have to reach into the previous
		 * frame to makeup the difference. */
		if( newFloatsToConsumeThisFrame < remaining )
		{
			int previousFloatsRequired = remaining - newFloatsToConsumeThisFrame;
			
			System.arraycopy( mPreviousFrame, 
							  mPreviousFrame.length - previousFloatsRequired, 
							  currentFrame, 
							  currentFramePointer, 
							  previousFloatsRequired );
			
			remaining -= previousFloatsRequired;
			currentFramePointer += previousFloatsRequired;
		}

		/* Fill the rest of the buffer with new samples */
		while( mRunning.get() && remaining > 0 )
		{
			if( mCurrentBuffer == null || 
					mCurrentBufferPointer >= mCurrentBuffer.length )
			{
				getNextBuffer();
			}

			/* If we don't have new samples to use, send the current frame with
			 * the remaining values as zero */
			if( mCurrentBuffer == null )
			{
				remaining = 0;
			}
			else
			{
				int samplesAvailable = mCurrentBuffer.length - mCurrentBufferPointer;

				while( remaining > 0 && samplesAvailable > 0 )
				{
					currentFrame[ currentFramePointer++ ] = 
							(float)mCurrentBuffer[ mCurrentBufferPointer++ ];
					
					samplesAvailable--;
					remaining--;
					newFloatsToConsumeThisFrame--;
				}
			}
		}
		
		/* If the incoming float rate is greater than the fft consumption rate,
		 * then we have to purge some floats, otherwise, store the previous
		 * frame, because we have overlapping frames */
		if( newFloatsToConsumeThisFrame > 0 )
		{
			purge( newFloatsToConsumeThisFrame );
		}
		else
		{
			mPreviousFrame = Arrays.copyOf( currentFrame, currentFrame.length );
		}

		return currentFrame;
	}
	
	private void calculate()
	{
		float[] samples = getSamples();
		
		Window.apply( mWindow, samples );

		if( mSampleType == SampleType.REAL )
		{
			mFFT.realForward( samples );
		}
		else
		{
			mFFT.complexForward( samples );
		}
		
		dispatch( samples );
	}

	
	private void purge( int samplesToPurge )
	{
		if( samplesToPurge <= 0 )
		{
			throw new IllegalArgumentException( "DFTProcessor - cannot purge "
					+ "negative sample amount" );
		}

		while( mRunning.get() && samplesToPurge > 0 )
		{
			if( mCurrentBuffer == null || 
					mCurrentBufferPointer >= mCurrentBuffer.length )
			{
				getNextBuffer();
			}

			if( mCurrentBuffer != null )
			{
				int samplesAvailable = mCurrentBuffer.length - mCurrentBufferPointer;
				
				if( samplesAvailable >= samplesToPurge )
				{
					mCurrentBufferPointer += samplesToPurge;
					samplesToPurge = 0;
				}
				else
				{
					samplesToPurge -= samplesAvailable;
					mCurrentBufferPointer = mCurrentBuffer.length;
				}
			}
			else
			{
				samplesToPurge = 0;
			}
		}
	}
	
	/**
	 * Takes a calculated DFT results set, reformats the data, and sends it 
	 * out to all registered listeners.
	 */
	private void dispatch( float[] results )
	{
		Iterator<DFTResultsConverter> it = mListeners.iterator();

		while( it.hasNext() )
		{
			it.next().receive( results );
		}
	}

    public void addConverter( DFTResultsConverter listener )
    {
		mListeners.add( listener );
    }

    public void removeConverter( DFTResultsConverter listener )
    {
		mListeners.remove( listener );
    }
	
	private class DFTCalculationTask implements Runnable
	{
		@Override
        public void run()
        {
			try
			{
				/* Only run if we're not currently running */
				if( mRunning.compareAndSet( false, true ) )
				{
					checkFFTSize();

					calculate();
					
					mRunning.set( false );
				}
			}
			catch( Exception e )
			{
				mLog.error( "error during dft processor calculation task", e );
			}
        }
	}
	
	/**
	 * Checks for a queued FFT width change request and applies it.  This 
	 * method will only be accessed by the scheduled executor that gains 
	 * access to run a calculate method, thus providing thread safety.
	 */
	private void checkFFTSize()
	{
		if( mNewDFTSize.getSize() != mDFTSize.getSize() )
		{
			mDFTSize = mNewDFTSize;
			
			calculateConsumptionRate();

			setWindowType( mWindowType );

			if( mSampleType == SampleType.COMPLEX )
			{
				mPreviousFrame = new float[ mDFTSize.getSize() * 2 ];
			}
			else
			{
				mPreviousFrame = new float[ mDFTSize.getSize() ];
			}

			mFFT = new FloatFFT_1D( mDFTSize.getSize() );
		}
	}
	
	public void clearBuffer()
	{
		mQueue.clear();
	}
	
	@Override
    public void frequencyChanged( FrequencyChangeEvent event )
    {
		switch( event.getEvent() )
		{
			case NOTIFICATION_SAMPLE_RATE_CHANGE:
				mSampleRate = event.getValue().intValue();
				calculateConsumptionRate();
				break;
			default:
				break;
		}
    }
	
	/**
	 * 
	 */
	private void calculateConsumptionRate()
	{
		mNewFloatResidual = 0.0f;
		
		mNewFloatsPerFrame = ( (float)mSampleRate / (float)mFrameRate ) *
				( mSampleType == SampleType.COMPLEX ? 2.0f : 1.0f );
		
		mFFTFloatsPerFrame = ( mSampleType == SampleType.COMPLEX ? 
					mDFTSize.getSize() * 2 : 
					mDFTSize.getSize() );
	}
}
