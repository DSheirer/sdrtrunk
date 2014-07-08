/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
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

import log.Log;
import sample.Listener;
import source.Source;
import source.Source.SampleType;
import source.tuner.FrequencyChangeListener;
import dsp.filter.Window;
import dsp.filter.Window.WindowType;
import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D;

/**
 * Processes both complex samples or float samples and dispatches a float array
 * of DFT results, using configurable fft size and output dispatch timelines.  
 */
public class DFTProcessor implements Listener<Float[]>,
									 DFTResultsProvider,
									 FrequencyChangeListener
{
	private CopyOnWriteArrayList<DFTResultsListener> mListeners =
			new CopyOnWriteArrayList<DFTResultsListener>();

	private ArrayBlockingQueue<Float[]> mQueue = 
							new ArrayBlockingQueue<Float[]>( 200 );
							
	private ScheduledExecutorService mScheduler = 
							Executors.newScheduledThreadPool(1);	
	
	private FFTWidth mFFTWidth = FFTWidth.FFT04096;
	private FFTWidth mNewFFTWidth = FFTWidth.FFT04096;
	
	private double[] mWindow;
	private WindowType mWindowType = Window.WindowType.HANNING;

	private FloatFFT_1D mFFT = new FloatFFT_1D( mFFTWidth.getWidth() );
	
	private int mFrameRate;
	private int mSampleRate;
	private float mOverlapPerFrame;
	private float mOverlapResidual;
	private float[] mPreviousFrame = new float[ 8192 ];
	
	private Float[] mCurrentBuffer;
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
		try
        {
	        clearScheduler();
        }
        catch ( InterruptedException e )
        {
        	Log.error( "DFTProcessor - exception shutting down processor - " + 
        				e.getLocalizedMessage() );
        }

		mScheduler.shutdown();
		
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
										mFFTWidth.getWidth() * 2 );		
		}
		else
		{
			mWindow = Window.getWindow( mWindowType,
										mFFTWidth.getWidth() );		
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
	
	public Source.SampleType getSampleType()
	{
		return mSampleType;
	}

	/**
	 * Queues an FFT size change request.  The scheduled executor will apply 
	 * the change when it runs.
	 */
	public void setFFTSize( FFTWidth width )
	{
		mNewFFTWidth = width;
	}
	
	public FFTWidth getFFTWidth()
	{
		return mFFTWidth;
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
		else
		{
			mFrameRate = framesPerSecond;

			calculateOverlap();

			/**
			 * Shutdown the scheduler and clear out any remaining tasks
			 */
	        try
	        {
	    		clearScheduler();
	        }
	        catch ( InterruptedException e )
	        {
		        Log.error( "DFTProcessor - exception while awaiting shutdown of "
		        		+ "calculation scheduler for reset" );
	        }

			/**
	         * Reset the scheduler
	         */
			mScheduler = Executors.newScheduledThreadPool(1);

			/**
			 * Schedule the DFT to run calculations at a fixed rate
			 */
			int initialDelay = 0;
			int period = (int)( 1000 / mFrameRate );
			TimeUnit unit = TimeUnit.MILLISECONDS;

			mScheduler.scheduleAtFixedRate( new DFTCalculationTask(), 
												initialDelay, period, unit );
			
		}
	}
	
	/**
	 * Shuts down the scheduler and blocks up to 100 milliseconds until all
	 * tasks have completed.
	 */
	private void clearScheduler() throws InterruptedException
	{
		mScheduler.shutdown();

        mScheduler.awaitTermination( 100, TimeUnit.MILLISECONDS );
	}

	public int getCalculationsPerSecond()
	{
		return mFrameRate;
	}
	
	/**
	 * Places the sample into a transfer queue for future processing. 
	 */
	@Override
    public void receive( Float[] samples )
    {
		if( !mQueue.offer( samples ) )
		{
			Log.error( "DFTProcessor - queue is full, purging queue, samples[" + samples + "]" );
			mQueue.clear();
			mQueue.offer( samples );
		}
    }
	
	private void getNextBuffer()
	{
		mCurrentBuffer = null;

		long start = System.currentTimeMillis();
		
		try
        {
            mCurrentBuffer = mQueue.take();
        }
        catch ( InterruptedException e )
        {
        	mCurrentBuffer = null;
        }

		mCurrentBufferPointer = 0;
	}

	private float[] getSamples()
	{
		int remaining;
		
		if( mSampleType == SampleType.COMPLEX )
		{
			remaining = mFFTWidth.getWidth() * 2;
		}
		else
		{
			remaining = mFFTWidth.getWidth();
		}

		float[] currentFrame = new float[ mFFTWidth.getWidth() * 2 ];
		int currentFramePointer = 0;
		
		/* Get the integer component of the overlap, leaving the fractional 
		 * residual for the next frame */
		float requiredOverlap = mOverlapPerFrame + mOverlapResidual;
		int overlap = (int)requiredOverlap;
		mOverlapResidual = requiredOverlap - overlap;

		/* If positive overlap, we have to re-use samples from previous frame */
		if( overlap > 0 )
		{
			int previousFramePointer;
			int length;
			
			if( mSampleType == SampleType.COMPLEX )
			{
				length = overlap * 2;
				previousFramePointer = mPreviousFrame.length - length;
			}
			else
			{
				length = overlap;
				previousFramePointer = (int)(mPreviousFrame.length / 2) - length;
			}

			System.arraycopy( mPreviousFrame, previousFramePointer, 
							  currentFrame, currentFramePointer, length );
			
			remaining -= length;
			currentFramePointer += length;			
		}
		
		/* If negative overlap, we have to throw away samples */
		else
		{
			if( mSampleType == SampleType.COMPLEX )
			{
				purge( -( overlap * 2 ) );
			}
			else
			{
				purge( -overlap );
			}
		}
		
		while( mRunning.get() && remaining > 0 )
		{
			if( mCurrentBuffer == null || 
					mCurrentBufferPointer == mCurrentBuffer.length )
			{
				getNextBuffer();
			}

			int samplesAvailable = mCurrentBuffer.length - mCurrentBufferPointer;

			if( samplesAvailable >= remaining )
			{
				while( remaining > 0 )
				{
					currentFrame[ currentFramePointer ] = 
								(float)mCurrentBuffer[ mCurrentBufferPointer ];
					
					currentFramePointer++;
					mCurrentBufferPointer++;
					remaining --;
				}

				mCurrentBufferPointer += remaining;
				remaining = 0;
			}
			else
			{
				while( samplesAvailable > 0 )
				{
					currentFrame[ currentFramePointer ] = 
							(float)mCurrentBuffer[ mCurrentBufferPointer ];
				
					currentFramePointer++;
					mCurrentBufferPointer++;
					samplesAvailable--;
					remaining--;
				}
			}
		}

		/* If frames are overlapping, store current frame to use next time */
		if( mOverlapPerFrame > 0 )
		{
			mPreviousFrame = null;
			mPreviousFrame = Arrays.copyOf( currentFrame, currentFrame.length );
		}

		return currentFrame;
	}
	
	private void calculate()
	{
		float[] samples = getSamples();
		
		Window.apply( mWindow, samples );

		if( mSampleType == SampleType.FLOAT )
		{
			mFFT.realForwardFull( samples );
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
					mCurrentBufferPointer == mCurrentBuffer.length )
			{
				getNextBuffer();
			}
			
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
	}
	
	/**
	 * Takes a calculated DFT results set, reformats the data, and sends it 
	 * out to all registered listeners.
	 */
	private void dispatch( float[] results )
	{
		float dc = Math.abs( results[ 0 ] );

		/* Ensure we have power in the DC power bin (bin 0), otherwise don't
		 * dispatch the results */
		if( dc != 0 )
		{
			float[] processed = new float[ results.length / 2 ];

			int half = results.length / 2;
			int quarter = results.length / 4;
			
			for( int x = 2; x < half; x += 2 )
			{
				processed[ quarter + ( x / 2 ) ] = 
						 (float)( Math.log10( 
						 (double)( Math.abs( results[ x ] ) / 
					     dc ) ) );
			}
			
			for( int x = 0; x < quarter - 1; x++ )
			{
				processed[ x ] = 
						 (float)( Math.log10( 
						 (double)( Math.abs( results[ half + ( ( x + 1 ) * 2 ) ] ) / 
					     dc ) ) );
			}

			//Real bin n/2 -1
			processed[ quarter - 1 ] = (float)( Math.log10( 
							(double)( Math.abs( results[ 1 ] ) / dc ) ) ); 

			Iterator<DFTResultsListener> it = mListeners.iterator();

			while( it.hasNext() )
			{
				it.next().receive( processed );
			}
		}
		
		results = null;
	}

	@Override
    public void addListener( DFTResultsListener listener )
    {
		mListeners.add( listener );
    }

	@Override
    public void removeListener( DFTResultsListener listener )
    {
		mListeners.remove( listener );
    }
	
	private class DFTCalculationTask implements Runnable
	{
		@Override
        public void run()
        {
			/* Only run if we're not currently running */
			if( mRunning.compareAndSet( false, true ) )
			{
				checkFFTSize();

				calculate();
				
				mRunning.set( false );
			}
			else
			{
				Log.error( "DFTProcessor - calculation already in progress"
						+ " - skipping this iteration" );
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
		if( mNewFFTWidth.getWidth() != mFFTWidth.getWidth() )
		{
			mFFTWidth = mNewFFTWidth;
			
			calculateOverlap();

			mWindow = Window.getWindow( WindowType.HANNING, 
					( mFFTWidth.getWidth() * 2 ) );
			
			mPreviousFrame = new float[ mFFTWidth.getWidth() * 2 ];

			mFFT = new FloatFFT_1D( mFFTWidth.getWidth() );
		}
	}
	
	public void clearBuffer()
	{
		mQueue.clear();
	}
	
	@Override
    public void frequencyChanged( long frequency, int bandwidth )
    {
		mSampleRate = bandwidth;
		
		calculateOverlap();
    }
	
	/**
	 * Positive overlap means we have to re-use samples from the previous
	 * frame
	 * 
	 * Negative overlap means we have to throw away samples
	 */
	private void calculateOverlap()
	{
		mOverlapPerFrame = 0.0f;
		mOverlapResidual = 0.0f;
		
		int requiredSampleRate = mFFTWidth.getWidth() * mFrameRate;
		
		int overlap = requiredSampleRate - mSampleRate;

		mOverlapPerFrame = (float)overlap / (float)mFrameRate;
	}
}
