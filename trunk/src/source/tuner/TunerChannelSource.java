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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import sample.complex.ComplexBuffer;
import sample.complex.ComplexSample;
import sample.complex.ComplexSampleAssembler;
import source.ComplexSource;
import source.SourceException;
import source.tuner.frequency.FrequencyChangeEvent;
import source.tuner.frequency.FrequencyChangeListener;
import source.tuner.frequency.FrequencyChangeEvent.Attribute;
import util.Oscillator;
import controller.ThreadPoolManager;
import controller.ThreadPoolManager.ThreadType;
import dsp.filter.ComplexPrimeCICDecimate;
import dsp.filter.FilterFactory;
import dsp.filter.Window.WindowType;

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
	private Oscillator mSineWaveGenerator;
	private ComplexPrimeCICDecimate mDecimationFilter;

	private ComplexSampleAssembler mBufferAssembler = 
								new ComplexSampleAssembler( 3000 );
	private long mTunerFrequency = 0;
	private int mTunerFrequencyError = 0;
	private int mTunerSampleRate;
	private FrequencyChangeListener mFrequencyChangeListener;
	private ThreadPoolManager mThreadPoolManager;
	private ScheduledFuture<?> mTaskHandle;
	
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
		
		mSineWaveGenerator = new Oscillator( frequencyOffset, mTuner.getSampleRate() );

		/* Fire a sample rate change event to get the decimation chain setup */
		frequencyChanged( new FrequencyChangeEvent( 
					Attribute.SAMPLE_RATE, mTuner.getSampleRate() ) );
	    
		/* Schedule the decimation task to run 50 times a second */
	    mTaskHandle = mThreadPoolManager.scheduleFixedRate( ThreadType.DECIMATION, 
	    		new DecimationProcessor(), 20, TimeUnit.MILLISECONDS );

	    /* Finally, register to receive samples from the tuner */
		mTuner.addListener( (Listener<ComplexBuffer>)this );
    }
	
    @Override
    public void dispose()
    {
    	mFrequencyChangeListener = null;
    	
		//Tell the tuner to release our resources
		mTuner.removeListener( (FrequencyChangeListener)this );
		mTuner.releaseChannel( this );

		if( mTaskHandle != null )
		{
			mThreadPoolManager.cancel( mTaskHandle );
		}
		
		mBuffer.clear();
		mBuffer = null;

//		mDecimationFilter.dispose();
		mDecimationFilter = null;
		
		mBufferAssembler.dispose();
		
		mTuner = null;
		
		mTunerChannel = null;
		
		mTunerChannel = null;
		mTuner = null;
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
    public void receive( ComplexBuffer sampleArray )
    {
		mBuffer.add( sampleArray );
    }

	@Override
    public void setListener( Listener<List<ComplexSample>> listener )
    {
		mBufferAssembler.setListener( listener );
    }
	
	@Override
    public void removeListener( Listener<List<ComplexSample>> listener )
    {
		mBufferAssembler.removeListener( listener );
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
					mSineWaveGenerator.setSampleRate( sampleRate );

					/* Get new decimation filter */
					mDecimationFilter = FilterFactory
							.getDecimationFilter( sampleRate, 
												  CHANNEL_RATE, 
												  1,   //Order
												  CHANNEL_PASS_FREQUENCY, 
												  60, //dB attenuation
												  WindowType.HAMMING );
					
					/* re-add the original output listener */
					mDecimationFilter.setListener( mBufferAssembler );

					mTunerSampleRate = sampleRate;
				}
				break;
			case FREQUENCY_ERROR:
				mTunerFrequencyError = event.getValue().intValue();
				
				long frequencyErrorOffset = mTunerFrequency - 
									   mTunerChannel.getFrequency() -
									   mTunerFrequencyError;
				
				mSineWaveGenerator.setFrequency( frequencyErrorOffset );
				break;
			case FREQUENCY:
				long frequency = event.getValue().longValue();
				
				/* If the frequency is updated, the AFC will also get reset, so
				 * we don't include the frequency error value here */
				long frequencyOffset = frequency - mTunerChannel.getFrequency(); 

				mSineWaveGenerator.setFrequency( frequencyOffset );
				
				mTunerFrequency = frequency;
				break;
			default:
				break;
		}
    }

    public int getSampleRate() throws SourceException
    {
	    return mTuner.getSampleRate();
    }

    public long getFrequency() throws SourceException
    {
	    return mTuner.getFrequency();
    }
	
	public class DecimationProcessor implements Runnable 
	{
		@Override
        public void run()
        {
			/* General exception handler so that any errors won't kill the
			 * decimation thread and cause the input buffers to fill up and
			 * run the program out of memory */
			try
			{
				List<ComplexBuffer> sampleBuffers = 
						new ArrayList<ComplexBuffer>();
	
				if( mBuffer != null )
				{
					/* Limit to 4, so that after a garbage collect we don't
					 * induce wild swings as it tries to take tons of samples
					 * and process them, causing delays and buffer refill */
					mBuffer.drainTo( sampleBuffers, 4 );
			
					for( ComplexBuffer buffer: sampleBuffers )
					{
						float[] samples = buffer.getSamples();
						float[] translated = new float[ samples.length ];

						/* Perform frequency translation */
						for( int x = 0; x < samples.length; x += 2 )
						{
							ComplexSample multiplier = 
									mSineWaveGenerator.nextComplex();
							
							translated[ x ] = ( samples[ x ] * multiplier.inphase() ) - 
									( samples[ x + 1 ] * multiplier.quadrature() );
					
							translated[ x + 1 ] = ( samples[ x + 1 ] * multiplier.inphase() ) + 
									( samples[ x ] * multiplier.quadrature() );
						}
						
						mDecimationFilter.receive( translated );
					}
					
					sampleBuffers.clear();
				}
			}
			catch( Exception e )
			{
				mLog.error( "encountered an error during decimation process", e );
			}
        }
	}

    public void addListener( FrequencyChangeListener listener )
    {
		mFrequencyChangeListener = listener;
    }
}
