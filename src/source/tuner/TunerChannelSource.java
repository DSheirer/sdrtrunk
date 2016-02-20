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
package source.tuner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
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
import source.tuner.frequency.FrequencyChangeListener;
import dsp.filter.FilterFactory;
import dsp.filter.Window.WindowType;
import dsp.filter.cic.ComplexPrimeCICDecimate;
import dsp.mixer.Oscillator;

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
	private ScheduledFuture<?> mTaskHandle;

	private long mTunerFrequency = 0;
	private int mTunerSampleRate;
	private int mChannelFrequencyCorrection = 0;
	
	private DecimationProcessor mDecimationProcessor = new DecimationProcessor();
	
	public TunerChannelSource( Tuner tuner, TunerChannel tunerChannel )
			   throws RejectedExecutionException, SourceException
    {
	    super( "Tuner Channel Source" );

	    mTuner = tuner;
	    mTuner.addListener( (FrequencyChangeListener)this );
	    
	    mTunerChannel = tunerChannel;

	    mTunerFrequency = mTuner.getFrequency();
	    
	    /* Setup the frequency translator to the current source frequency */
		long frequencyOffset = mTunerFrequency - mTunerChannel.getFrequency();
		
		mMixer = new Oscillator( frequencyOffset, mTuner.getSampleRate() );

		/* Fire a sample rate change event to setup the decimation chain */
		frequencyChanged( new FrequencyChangeEvent( 
			Event.SAMPLE_RATE_CHANGE_NOTIFICATION, mTuner.getSampleRate() ) );
    }
	
	public void start( ScheduledExecutorService scheduledExecutorService )
	{
		/* Schedule the decimation task to run every 10 ms (100 iterations/second) */
		mTaskHandle = scheduledExecutorService.scheduleAtFixedRate( 
				    		mDecimationProcessor, 0, 10, TimeUnit.MILLISECONDS );

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
			mTaskHandle.cancel( true );
			mTaskHandle = null;
		}

		synchronized( mBuffer )
		{
			mBuffer.clear();
			mBuffer = null;
		}

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
		synchronized( mBuffer )
		{
			if( mBuffer != null )
			{
				mBuffer.add( buffer );
			}
		}
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

	/**
	 * Handler for frequency change events received from the tuner and channel
	 * frequency correction events received from the channel consumer/listener
	 */
	@Override
    public void frequencyChanged( FrequencyChangeEvent event )
    {
		// Echo the event to the registered event listener
		if( mFrequencyChangeListener != null )
		{
			mFrequencyChangeListener.frequencyChanged( event );
		}

		switch( event.getEvent() )
		{
			case FREQUENCY_CHANGE_NOTIFICATION:
				mTunerFrequency = event.getValue().longValue();
				updateMixerFrequencyOffset();
				break;
			case CHANNEL_FREQUENCY_CORRECTION_CHANGE_REQUEST:
				mChannelFrequencyCorrection = event.getValue().intValue();

				updateMixerFrequencyOffset();
				
				if( mFrequencyChangeListener != null )
				{
					mFrequencyChangeListener.frequencyChanged( 
						new FrequencyChangeEvent( 
							Event.CHANNEL_FREQUENCY_CORRECTION_CHANGE_NOTIFICATION, 
							mChannelFrequencyCorrection ) );
				}
				break;
			case SAMPLE_RATE_CHANGE_NOTIFICATION:
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
		private AtomicBoolean mDispose = new AtomicBoolean( false );
		private List<ComplexBuffer> mSampleBuffers = new ArrayList<ComplexBuffer>();

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
				if( mBuffer != null )
				{
					mBuffer.drainTo( mSampleBuffers, 8 );
			
					for( Buffer buffer: mSampleBuffers )
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
					
					mSampleBuffers.clear();
				}
			}
			catch( Exception e )
			{
				/* Only log the stack trace if we've haven't been shutdown and
				 * this is a true error */
				if( !mDispose.get() )
				{
					mLog.error( "Error encountered during decimation process", e );
				}
			}

			/* Check to see if we've been shutdown */
			if( mDispose.get() )
			{
				mSampleBuffers.clear();
				cleanup();
			}
        }
	}
}
