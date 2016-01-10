/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
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
package source.tuner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Buffer;
import sample.Listener;
import sample.complex.Complex;
import sample.complex.ComplexBuffer;
import source.ComplexSource;
import source.SourceException;
import source.tuner.frequency.FrequencyChangeEvent;
import source.tuner.frequency.FrequencyChangeEvent.Event;
import source.tuner.frequency.IFrequencyChangeProcessor;
import controller.ThreadPoolManager;
import controller.ThreadPoolManager.ThreadType;
import dsp.filter.FilterFactory;
import dsp.filter.Window.WindowType;
import dsp.filter.cic.ComplexPrimeCICDecimate;
import dsp.mixer.Oscillator;

public class TunerChannelSource extends ComplexSource
		 implements IFrequencyChangeProcessor, Listener<ComplexBuffer>
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( TunerChannelSource.class );
	
	private static int CHANNEL_RATE = 48000;
	private static int CHANNEL_PASS_FREQUENCY = 12000;
	
	private LinkedTransferQueue<ComplexBuffer> mBuffer =
							new LinkedTransferQueue<ComplexBuffer>();
	private Tuner mTuner;
	private TunerChannel mTunerChannel;
	private Oscillator mMixer;
	private ComplexPrimeCICDecimate mDecimationFilter;
	private Listener<ComplexBuffer> mListener;
	private IFrequencyChangeProcessor mFrequencyChangeProcessor;
	private ThreadPoolManager mThreadPoolManager;
	private ScheduledFuture<?> mTaskHandle;

	private long mTunerFrequency = 0;
	private int mTunerSampleRate;
	private int mChannelFrequencyCorrection = 0;
	
	private DecimationProcessor mDecimationProcessor = new DecimationProcessor();
	
	private AtomicBoolean mRunning = new AtomicBoolean();
	private boolean mExpended = false;
	
	/**
	 * Provides a Digital Drop Channel (DDC) to decimate the IQ output from a 
	 * tuner down to a 48 kHz IQ channel rate.
	 * 
	 * Note: this class can only be used once (started and stopped) and a new
	 * tuner channel source must be requested from the tuner once this object
	 * has been stopped.  This is because channels are managed dynamically and
	 * center tuned frequency may have changed since this source was obtained 
	 * and thus the tuner might no longer be able to source this channel once it
	 * has been stopped.
	 * 
	 * @param threadPoolManager to use for decimation processing task
	 * @param tuner to obtain wideband IQ samples from
	 * @param tunerChannel specifying the center frequency for the DDC
	 * @throws RejectedExecutionException if the thread pool manager cannot 
	 * 		  accept the decimation processing task
	 * @throws SourceException if the tuner has an issue providing IQ samples
	 */
	public TunerChannelSource( ThreadPoolManager threadPoolManager, Tuner tuner, 
							   TunerChannel tunerChannel )
				   throws RejectedExecutionException, SourceException
    {
	    mThreadPoolManager = threadPoolManager;
	    
	    mTuner = tuner;
	    mTuner.addListener( this );
	    
	    mTunerChannel = tunerChannel;

	    mTunerFrequency = mTuner.getFrequency();
	    
	    /* Setup the frequency translator to the current source frequency */
		long frequencyOffset = mTunerFrequency - mTunerChannel.getFrequency();
		
		mMixer = new Oscillator( frequencyOffset, mTuner.getSampleRate() );

		/* Fire a sample rate change event to setup the decimation chain */
		frequencyChanged( new FrequencyChangeEvent( 
			Event.NOTIFICATION_SAMPLE_RATE_CHANGE, mTuner.getSampleRate() ) );
    }
	
	
    @Override
	public void reset()
	{
	}


	@Override
	public void start()
	{
		if( mExpended )
		{
			throw new IllegalStateException( "Attempt to re-start an expended "
				+ "tuner channel source.  TunerChannelSource objects can only "
				+ "be used once. " );
		}
		
		if( mRunning.compareAndSet( false, true ))
		{
			/* Schedule the decimation task to run every 20 ms (50 iterations/second) */
		    mTaskHandle = mThreadPoolManager.scheduleFixedRate( ThreadType.DECIMATION, 
		    		mDecimationProcessor, 20, TimeUnit.MILLISECONDS );

		    /* Finally, register to receive samples from the tuner */
			mTuner.addListener( (Listener<ComplexBuffer>)this );
		}
		else
		{
			mLog.warn( "Attempt to start() and already running tuner channel "
					+ "source was ignored" );
		}
	}


	@Override
	public void stop()
	{
		if( mRunning.compareAndSet( true, false ))
		{
			mTuner.releaseChannel( this );
			mDecimationProcessor.shutdown();
			
			if( mThreadPoolManager != null && mTaskHandle != null )
			{
				mThreadPoolManager.cancel( mTaskHandle );
				mTaskHandle = null;
			}
			
			mBuffer.clear();
			
			mExpended = true;
		}
		else
		{
			mLog.warn( "Attempt to stop() and already stopped tuner channel "
					+ "source was ignored" );
		}
	}


	@Override
    public void dispose()
    {
		if( !mRunning.get() )
		{
	    	/* Tell the tuner to release/unregister our resources */
	    	mTuner.removeFrequencyChangeProcessor( this );
			mTuner = null;
			mTunerChannel = null;
			mBuffer = null;
			mFrequencyChangeProcessor = null;
			mListener = null;
			mMixer = null;
			mDecimationFilter.dispose();
			mDecimationFilter = null;
		}
    }
    
	public Tuner getTuner()
	{
		return mTuner;
	}
	
	public TunerChannel getTunerChannel()
	{
		return mTunerChannel;
	}
	
	@Override
    public void receive( ComplexBuffer buffer )
    {
		if( mRunning.get() )
		{
			mBuffer.add( buffer );
		}
    }

    public void setFrequencyChangeListener( IFrequencyChangeProcessor processor )
    {
		mFrequencyChangeProcessor = processor;
    }

	@Override
	public void setListener( Listener<ComplexBuffer> listener )
	{
		/* Save a pointer to the listener so that if we have to change the 
		 * decimation filter, we can re-add the listener */
		mListener = listener;
		
		mDecimationFilter.setListener( listener );
	}

	@Override
	public void removeListener( Listener<ComplexBuffer> listener )
	{
		mDecimationFilter.removeListener();
	}

	/**
	 * Handler for frequency change events received from the tuner and channel
	 * frequency correction events received from the channel consumer/listener
	 */
	@Override
    public void frequencyChanged( FrequencyChangeEvent event )
    {
		// Echo the event to the registered event listener
		if( mFrequencyChangeProcessor != null )
		{
			mFrequencyChangeProcessor.frequencyChanged( event );
		}

		switch( event.getEvent() )
		{
			case NOTIFICATION_FREQUENCY_CHANGE:
				mTunerFrequency = event.getValue().longValue();
				updateMixerFrequencyOffset();
				break;
			case REQUEST_CHANNEL_FREQUENCY_CORRECTION_CHANGE:
				mChannelFrequencyCorrection = event.getValue().intValue();

				updateMixerFrequencyOffset();
				
				if( mFrequencyChangeProcessor != null )
				{
					mFrequencyChangeProcessor.frequencyChanged( 
						new FrequencyChangeEvent( 
							Event.NOTIFICATION_CHANNEL_FREQUENCY_CORRECTION_CHANGE, 
							mChannelFrequencyCorrection ) );
				}
				break;
			case NOTIFICATION_SAMPLE_RATE_CHANGE:
				int sampleRate = event.getValue().intValue();
				
				if( mTunerSampleRate != sampleRate )
				{
					mMixer.setSampleRate( sampleRate );

					/* Get new decimation filter */
					mDecimationFilter = FilterFactory
							.getDecimationFilter( sampleRate, 
												  CHANNEL_RATE, 
												  1,   //Order
												  CHANNEL_PASS_FREQUENCY, 
												  60, //dB attenuation
												  WindowType.HAMMING );
					
					/* re-add the original output listener */
					mDecimationFilter.setListener( mListener );

					mTunerSampleRate = sampleRate;
				}
				break;
			default:
				break;
		}
    }

	/**
	 * Calculates the local mixer frequency offset from the tuned frequency,
	 * channel's requested frequency, and channel frequency correction.
	 */
	private void updateMixerFrequencyOffset()
	{
		long offset = mTunerFrequency - 
				   mTunerChannel.getFrequency() -
				   mChannelFrequencyCorrection;

		mMixer.setFrequency( offset );
	}

    public int getSampleRate() throws SourceException
    {
    	return CHANNEL_RATE;
    }

    public long getFrequency() throws SourceException
    {
    	return mTunerChannel.getFrequency();
    }
	
    /**
     * Decimates an inbound buffer of I/Q samples from the source down to the
     * standard 48000 channel sample rate
     */
	public class DecimationProcessor implements Runnable 
	{
		private boolean mProcessing = true;
		private List<ComplexBuffer> mSampleBuffers = new ArrayList<ComplexBuffer>();

		public void shutdown()
		{
			mProcessing = false;
		}
		
		@Override
        public void run()
        {
			/* General exception handler so that any errors won't kill the
			 * decimation thread and cause the input buffers to fill up and
			 * run the program out of memory */
			try
			{
				if( mProcessing )
				{
					if( mBuffer != null )
					{
						mBuffer.drainTo( mSampleBuffers, 4 );
				
						for( Buffer buffer: mSampleBuffers )
						{
							/* Check to see if we've been shutdown */
							if( !mProcessing )
							{
								mBuffer.clear();
								return;
							}
							else
							{
								float[] samples = buffer.getSamples();

								/* We make a copy of the buffer so that we don't affect
								 * anyone else that is using the same buffer, like other
								 * channels or the spectral display */
								float[] translated = new float[ samples.length ];
								
								/* Perform frequency translation */
								for( int x = 0; x < samples.length; x += 2 )
								{
									mMixer.rotate();
									
									translated[ x ] = Complex.multiplyInphase( 
										samples[ x ], samples[ x + 1 ], mMixer.inphase(), mMixer.quadrature() );

									translated[ x + 1 ] = Complex.multiplyQuadrature( 
											samples[ x ], samples[ x + 1 ], mMixer.inphase(), mMixer.quadrature() );
								}
								
								if( mProcessing && mDecimationFilter != null )
								{
									mDecimationFilter.receive( new ComplexBuffer( translated ) );
								}
							}
						}
						
						mSampleBuffers.clear();
					}
				}
			}
			catch( Exception e )
			{
				/* Only log the stack trace if we're still processing */
				if( mProcessing )
				{
					mLog.error( "Error encountered during decimation process", e );
				}
			}

			/* Check to see if we've been shutdown */
			if( !mProcessing )
			{
				mBuffer.clear();
				mSampleBuffers.clear();
			}
        }
	}
}
