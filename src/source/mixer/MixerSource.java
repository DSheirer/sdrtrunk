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
package source.mixer;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import log.Log;
import sample.Listener;
import source.FloatArraySource;
import source.SourceException;

public class MixerSource extends FloatArraySource
{
	private long mFrequency = 0;
	private int mBufferSize = 16384;
	
	private BufferReader mBufferReader = new BufferReader();
	private TargetDataLine mTargetDataLine;
	private AudioFormat mAudioFormat;
	private String mDisplayName;
	private int mBytesPerFrame = 0;
	private SampleAdapter mSampleAdapter;
	
	CopyOnWriteArrayList<Listener<Float[]>> mSampleListeners = 
							new CopyOnWriteArrayList<Listener<Float[]>>();
    
	/**
	 * Complex Mixer Source - constructs a reader on the mixer/sound card target 
	 * data line using the specified audio format (sample size, sample rate ) 
	 * and broadcasts float[] sample data to all registered listeners.  Reads 
	 * sample buffers sized to 10% of the sample rate specified in audio format.
	 * 
	 * @param targetDataLine - mixer or sound card to be used
	 * 
	 * @param format - audio format
	 * 
	 * @param name - token name to use for this source
	 * 
	 * @param sampleAdapter - adapter to convert byte array data read from the
	 * mixer into float array data.  Can optionally invert the channel data if
	 * the left/right stereo channels are inverted.
	 */
    public MixerSource( TargetDataLine targetDataLine, 
    						   AudioFormat format,
    						   String name,
    						   SampleAdapter sampleAdapter )
    {
    	super( name );
    	
    	mTargetDataLine = targetDataLine;
        mAudioFormat = format;
        mSampleAdapter = sampleAdapter;
    }
    
    public void setListener( Listener<Float[]> listener )
    {
		mSampleListeners.add( listener );
		
		/* If this is the first listener, start the reader thread */
		if( !mBufferReader.isRunning() )
		{
			Thread thread = new Thread( mBufferReader );
			thread.setName( getName() + " Sample Reader" );
			thread.start();
		}
    }

    public void removeListener( Listener<Float[]> listener )
    {
		mSampleListeners.remove( listener );

		/* If this is the laster listener, stop the reader thread */
		if( mSampleListeners.isEmpty() )
		{
			mBufferReader.stop();
		}
    }

    public void broadcast( Float[] samples )
    {
		Iterator<Listener<Float[]>> it = mSampleListeners.iterator();
		
		while( it.hasNext() )
		{
			Listener<Float[]> next = it.next();
			
			/* if this is the last (or only) listener, send him the original 
			 * buffer, otherwise send him a copy of the buffer */
			if( it.hasNext() )
			{
				next.receive( Arrays.copyOf( samples, samples.length ) );
			}
			else
			{
				next.receive( samples );
			}
		}
    }
    
	@Override
	public String toString()
	{
		return mDisplayName;
	}
	
    public int getSampleRate()
    {
		if( mTargetDataLine != null )
		{
		    return (int)mTargetDataLine.getFormat().getSampleRate();
		}
		else
		{
			return 0;
		}
    }

    /**
     * Returns the frequency of this source.  Default is 0.
     */
    public long getFrequency() throws SourceException
    {
	    return mFrequency;
    }

    /**
     * Specify the frequency that will be returned from this source.  This may
     * be useful if you are streaming an external audio source in through the
     * sound card and you want to specify a frequency for that source
     */
    public void setFrequency( long frequency )
    {
    	mFrequency = frequency;
    }

	/**
	 * Reader thread.  Performs blocking read against the mixer target data 
	 * line, converts the samples to an array of floats using the specified 
	 * adapter.  Dispatches float arrays to all registered listeners.
	 */
	public class BufferReader implements Runnable
	{
		private AtomicBoolean mRunning = new AtomicBoolean();
		
		@Override
        public void run()
        {
			if( mRunning.compareAndSet( false, true ) )
			{
        		if( mTargetDataLine == null )
        		{
        			mRunning.set( false );

        			Log.error( "ComplexMixerSource - mixer target data line"
                			+ " is null" );
        		}
        		else
        		{
        			mBytesPerFrame = mAudioFormat.getSampleSizeInBits() / 8;

    	    		if( mBytesPerFrame == AudioSystem.NOT_SPECIFIED )
    	            {
    	                mBytesPerFrame = 2;
    	            }
    	    		
    	    		/* Set buffer size to 1/10 second of samples */
    	    		mBufferSize = (int)( mAudioFormat.getSampleRate() 
    	    				* 0.05 ) * mBytesPerFrame;

            		/* We'll reuse the same buffer for each read */
            		byte[] buffer = new byte[ mBufferSize ];
            		
            		try
                    {
	                    mTargetDataLine.open( mAudioFormat );

	                    mTargetDataLine.start();
                    }
                    catch ( LineUnavailableException e )
                    {
                    	Log.error( "ComplexMixerSource - mixer target data line"
                    			+ "not available to read data from - " + 
                    			e.getLocalizedMessage() );
                    	
                    	mRunning.set( false );
                    }
            		
            		while( mRunning.get() )
    				{
        				try
                        {
            				/* Blocking read - waits until the buffer fills */
        					mTargetDataLine.read( buffer, 0, buffer.length );

	                        /* Convert samples to float array */
	            			Float[] convertedSamples = 
	            					mSampleAdapter.convert( buffer );
	            			
	            			/* Dispatch samples to registered listeners */
	            			broadcast( convertedSamples );
                        }
                        catch ( Exception e )
                        {
                        	Log.error( "ComplexMixerSource - error while reading"
                        			+ "from the mixer target data line - " +
                        			e.getLocalizedMessage() );
                        	
                        	mRunning.set( false );
                        }
    				}
        		}
        		
        		/* Close the data line if it is still open */
        		if( mTargetDataLine != null && mTargetDataLine.isOpen() )
        		{
        			mTargetDataLine.close();
        		}
			}
        }

		/**
		 * Stops the reader thread
		 */
		public void stop()
		{
			if( mRunning.compareAndSet( true, false ) )
			{
				mTargetDataLine.stop();
				mTargetDataLine.close();
			}
		}

		/**
		 * Indicates if the reader thread is running
		 */
		public boolean isRunning()
		{
			return mRunning.get();
		}
	}

	@Override
    public void dispose()
    {
		if( mBufferReader != null )
		{
			mBufferReader.stop();
			mSampleListeners.clear();			
		}
    }
}
