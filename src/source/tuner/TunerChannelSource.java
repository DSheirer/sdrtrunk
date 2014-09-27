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

import sample.Listener;
import sample.complex.ComplexSample;
import sample.complex.ComplexSampleAssembler;
import source.ComplexSource;
import source.SourceException;
import source.tuner.FrequencyChangeEvent.Attribute;
import util.Oscillator;
import controller.ThreadPoolManager;
import controller.ThreadPoolManager.ThreadType;
import dsp.filter.ComplexFilter;
import dsp.filter.FilterFactory;
import dsp.filter.Window.WindowType;

public class TunerChannelSource extends ComplexSource
							 implements FrequencyChangeListener,
							 			Listener<Float[]>
{
	private static int sCHANNEL_RATE = 48000;
	private static int sCHANNEL_PASS_FREQUENCY = 12500;
	
	private LinkedTransferQueue<Float[]> mBuffer =
							new LinkedTransferQueue<Float[]>();
	private Tuner mTuner;
	private TunerChannel mTunerChannel;
	private Oscillator mSineWaveGenerator;
	private ComplexFilter[] mDecimationFilters;

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
	    
		/* Schedule the decimation task to run 20 times a second */
	    mTaskHandle = mThreadPoolManager.schedule( ThreadType.DECIMATION, 
	    		new DecimationProcessor(), 50, TimeUnit.MILLISECONDS );

	    /* Finally, register to receive samples from the tuner */
		mTuner.addListener( (Listener<Float[]>)this );
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
		
		for( ComplexFilter filter: mDecimationFilters )
		{
			filter.dispose();
		}
		
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
    public void receive( Float[] samples )
    {
		mBuffer.add( samples );
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
		mDecimationFilters[ mDecimationFilters.length - 1 ]
				.setListener( null );
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
				int sampleRate = (int)event.getValue();
				
				if( mTunerSampleRate != sampleRate )
				{
					/* Set the oscillator to the new frequency */
					mSineWaveGenerator.setSampleRate( sampleRate );
					
					/* Get new decimation filters */
					mDecimationFilters = FilterFactory
							.getDecimationFilters( sampleRate, 
												   sCHANNEL_RATE, 
												   sCHANNEL_PASS_FREQUENCY, 
												   48, //dB attenuation
												   WindowType.HAMMING );
					
					/* wire the filters together */
					if( mDecimationFilters.length > 1 )
					{
						for( int x = 1; x < mDecimationFilters.length; x++ )
						{
							mDecimationFilters[ x - 1 ]
									.setListener( mDecimationFilters[ x ] );
						}
					}
					
					/* re-add the original output listener */
					mDecimationFilters[ mDecimationFilters.length - 1 ]
										.setListener( mBufferAssembler );

					mTunerSampleRate = sampleRate;
				}
				break;
			case FREQUENCY_ERROR:
				mTunerFrequencyError = (int)event.getValue();
				
				long frequencyErrorOffset = mTunerFrequency - 
									   mTunerChannel.getFrequency() -
									   mTunerFrequencyError;
				
				mSineWaveGenerator.setFrequency( frequencyErrorOffset );
				break;
			case FREQUENCY:
				int frequency = (int)event.getValue();
				
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
			List<Float[]> samplesList = new ArrayList<Float[]>();
			
			if( mBuffer != null )
			{
				/* Limit to 10, so that after a garbage collect we don't
				 * induce wild swings as it tries to take tons of samples
				 * and process them, causing delays and buffer refill */
				mBuffer.drainTo( samplesList, 10 );

				for( Float[] samples: samplesList )
				{
					for( int x = 0; x < samples.length; x += 2 )
					{
						Float left = samples[ x ];
						Float right = samples[ x + 1 ];
						
						if( left != null && right != null )
						{
			            	/* perform frequency translation */
		        			ComplexSample translated = ComplexSample.multiply( 
		        					mSineWaveGenerator.nextComplex(), left, right ); 
			            	
			            	/* decimate the sample rate */
		        			if( mDecimationFilters[ 0 ] != null )
		        			{
				            	mDecimationFilters[ 0 ].receive( translated );
		        			}
						}
					}
				}
				
				samplesList.clear();
			}
        }
	}

    public void addListener( FrequencyChangeListener listener )
    {
		mFrequencyChangeListener = listener;
    }
}
