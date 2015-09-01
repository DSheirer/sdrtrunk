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

import sample.Listener;
import sample.complex.Complex;
import sample.complex.ComplexBuffer;
import source.ComplexSource;
import source.SourceException;
import source.tuner.frequency.FrequencyChangeEvent;
import source.tuner.frequency.FrequencyChangeEvent.Attribute;
import source.tuner.frequency.FrequencyChangeListener;
import util.Oscillator;
import controller.ThreadPoolManager;
import controller.ThreadPoolManager.ThreadType;
import dsp.filter.FilterFactory;
import dsp.filter.Window.WindowType;
import dsp.filter.cic.ComplexPrimeCICDecimate;

public class TunerChannelSource extends ComplexSource
							 implements FrequencyChangeListener,
							 			Listener<ComplexBuffer>
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
	private FrequencyChangeListener mFrequencyChangeListener;
	private ThreadPoolManager mThreadPoolManager;
	private ScheduledFuture<?> mTaskHandle;

	private long mTunerFrequency = 0;
	private int mTunerFrequencyError = 0;
	private int mTunerSampleRate;
	
	private DecimationProcessor mDecimationProcessor = new DecimationProcessor();
	
	public TunerChannelSource( ThreadPoolManager threadPoolManager,
							   Tuner tuner, 
							   TunerChannel tunerChannel )
				   throws RejectedExecutionException, SourceException
    {
	    super( "Tuner Channel Source" );

	    mThreadPoolManager = threadPoolManager;
	    
	    mTuner = tuner;
	    mTuner.addListener( (FrequencyChangeListener)this );
	    
	    mTunerChannel = tunerChannel;

	    mTunerFrequency = mTuner.getFrequency();
	    
	    /* Setup the frequency translator to the current source frequency */
		long frequencyOffset = mTunerFrequency - mTunerChannel.getFrequency();
		
		mMixer = new Oscillator( frequencyOffset, mTuner.getSampleRate() );

		/* Fire a sample rate change event to get the decimation chain setup */
		frequencyChanged( new FrequencyChangeEvent( 
					Attribute.SAMPLE_RATE, mTuner.getSampleRate() ) );
	    
		/* Schedule the decimation task to run 50 times a second */
	    mTaskHandle = mThreadPoolManager.scheduleFixedRate( ThreadType.DECIMATION, 
	    		mDecimationProcessor, 20, TimeUnit.MILLISECONDS );

	    /* Finally, register to receive samples from the tuner */
		mTuner.addListener( (Listener<ComplexBuffer>)this );
    }
	
    @Override
    public void dispose()
    {
    	/* Tell the tuner to release/unregister our resources */
		mTuner.removeListener( (FrequencyChangeListener)this );
		mTuner.releaseChannel( this );
		mTuner = null;
		mTunerChannel = null;

		/* Flag the decimation processor to shutdown */
		if( mDecimationProcessor != null )
    	{
    		mDecimationProcessor.dispose();
    	}
		
    }
    
    /**
     * Cleanup method to allow the decimation processor to perform final cleanup
     * once it gracefully shuts itself down
     */
    private void cleanup()
    {
		if( mTaskHandle != null )
		{
			mThreadPoolManager.cancel( mTaskHandle );
			mThreadPoolManager = null;
			mTaskHandle = null;
		}


		mBuffer.clear();
		mBuffer = null;

		mFrequencyChangeListener = null;
		mListener = null;

		mMixer = null;
		
		mDecimationFilter.dispose();
		mDecimationFilter = null;
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
		mBuffer.add( buffer );
    }

    public void setFrequencyChangeListener( FrequencyChangeListener listener )
    {
		mFrequencyChangeListener = listener;
    }

	@Override
	public void setListener( Listener<ComplexBuffer> listener )
	{
		/* Get a pointer to the listener so that if we have to change the 
		 * decimation filter, we can re-add the listener */
		mListener = listener;
		
		mDecimationFilter.setListener( listener );
	}

	@Override
	public void removeListener( Listener<ComplexBuffer> listener )
	{
		mDecimationFilter.removeListener();
	}
	
	@Override
    public void frequencyChanged( FrequencyChangeEvent event )
    {
		/* Send change to Automatic Frequency Control if it exists */
		if( mFrequencyChangeListener != null )
		{
			mFrequencyChangeListener.frequencyChanged( event );
		}

		switch( event.getAttribute() )
		{
			case SAMPLE_RATE_ERROR:
				break;
			case SAMPLE_RATE:
				int sampleRate = event.getValue().intValue();
				
				if( mTunerSampleRate != sampleRate )
				{
					/* Set the oscillator to the new frequency */
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
			case FREQUENCY_ERROR:
				mTunerFrequencyError = event.getValue().intValue();
				
				long frequencyErrorOffset = mTunerFrequency - 
									   mTunerChannel.getFrequency() -
									   mTunerFrequencyError;

				mMixer.setFrequency( frequencyErrorOffset );
				break;
			case FREQUENCY:
				long frequency = event.getValue().longValue();
				
				/* If the frequency is updated, the AFC will also get reset, so
				 * we don't include the frequency error value here */
				mTunerFrequencyError = 0;
				
				long frequencyOffset = frequency - mTunerChannel.getFrequency(); 

				mMixer.setFrequency( frequencyOffset );
				
				mTunerFrequency = frequency;
				break;
			default:
				break;
		}
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
		private AtomicBoolean mDispose = new AtomicBoolean( false );

		public void dispose()
		{
			mDispose.set( true );
		}
		
		@Override
        public void run()
        {
			/* General exception handler so that any errors won't kill the
			 * decimation thread and cause the input buffers to fill up and
			 * run the program out of memory */
			try
			{
				/* Check to see if we've been shutdown */
				if( mDispose.get() )
				{
					cleanup();
					return;
				}
				else
				{
					List<ComplexBuffer> sampleBuffers = 
							new ArrayList<ComplexBuffer>();
		
					if( mBuffer != null )
					{
						mBuffer.drainTo( sampleBuffers, 4 );
				
						for( ComplexBuffer buffer: sampleBuffers )
						{
							/* Check to see if we've been shutdown */
							if( mDispose.get() )
							{
								cleanup();
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
								
								if( mDecimationFilter != null )
								{
									mDecimationFilter.receive( new ComplexBuffer( translated ) );
								}
							}
						}
						
						sampleBuffers.clear();
					}
				}
			}
			catch( Exception e )
			{
				mLog.error( "encountered an error during decimation process", e );
			}
        }
	}
}
