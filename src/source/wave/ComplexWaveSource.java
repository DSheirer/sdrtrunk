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
package source.wave;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import sample.Listener;
import sample.complex.ComplexSample;

public class ComplexWaveSource extends WaveSource
{
	private boolean mRunning = false;
    private boolean mLoop = false;

    private AudioInputStream mInputStream = null;
    private int mBytesPerFrame = 0;
    private long mFrequency = 0;
    
    private byte[] mBuffer;
    private int mBufferPointer = 0;
    private int mBufferLength;
    private ShortBuffer mBufferWrapper;
    
    private static int BUFFER_SAMPLE_SIZE = 2000;

    private ArrayList<Listener<ComplexSample>> mListeners = 
    							new ArrayList<Listener<ComplexSample>>();

    /**
     * Single channel (mono) wave file playback source with optional looping
     * for continous playback.  
     * 
     * Playback can be invoked manually with the next() and next(x) methods to 
     * get sample(s).
     * 
     * Plaback can be run automatically by invoking the start() and stop()
     * methods.
     * 
     * Registered float sample listener(s) will receive any samples produced
     * by the manual or automatic methods.
     * 
     * @param loop - true for looping
     * 
     * @throws FileNotFoundException if filename is not found
     */
    public ComplexWaveSource( File file, boolean loop ) throws IOException 
    {
    	super( file, SampleType.COMPLEX );
        mLoop = loop;
    }

	@Override
    public int getSampleRate()
    {
	    return (int)mInputStream.getFormat().getSampleRate();
    }

	/**
	 * Returns the frequency set for this file.  Normally returns zero, but
	 * the value can be set with setFrequency() method.
	 */
    public long getFrequency()
    {
	    return mFrequency;
    }

    /**
     * Changes the value returned from getFrequency() for this source.
     */
    public void setFrequency( long frequency )
    {
    	mFrequency = frequency;
    }
    
    /**
     * Closes the audio input stream
     * @throws IOException
     */
    public void close() throws IOException
    {
        if( mInputStream != null )
        {
            mInputStream.close();
        }
        
        mInputStream = null;
    }

    public void open() throws IOException
    {
        try
        {
            mInputStream = AudioSystem.getAudioInputStream( getFile() );
        }
        catch( UnsupportedAudioFileException e )
        {
        	throw new IOException( "MonoWaveSource - unsupported audio file "
        			+ "exception - ", e );
        }

        mBytesPerFrame = mInputStream.getFormat().getFrameSize();

        if( mBytesPerFrame != 2 || mInputStream.getFormat().getChannels() != 2 )
        {
        	throw new IOException( "Unsupported Wave Format - requires two "
        			+ "channels with 16-bit samples" );
        }

        mBuffer = new byte[ mBytesPerFrame * BUFFER_SAMPLE_SIZE ];
        
        readBuffer();
    }


    /**
     * Reads a sample from the wave file and broadcasts the sample to all
     * registered float listeners.
     * 
     * @return - true if data was read or false if no more data.  If loop is
     * set to true, then this method will continue to produce samples as long
     * as there are not IO exceptions.
     * 
     * @throws IOException if unable to read data from the wave file
     */
    /**
     * Reads the next buffer from the wave file.
     * @throws IOException
     */
    public void readBuffer() throws IOException
    {
        if( mInputStream != null )
        {
        	/* reset the buffer pointer and fill the buffer with zeros */
        	mBufferPointer = 0;
        	mBufferLength = 0;
        	Arrays.fill( mBuffer, (byte)0 );

        	while( mBufferLength == 0 )
        	{
        		boolean reset = false;
        		
            	/* Fill the buffer with samples from the file */
            	mBufferLength = (int)( mInputStream.read( mBuffer ) / 
            						   mBytesPerFrame );
            	
            	if( reset && mBufferLength == 0 )
            	{
            		return;
            	}

            	/* If we get a partial buffer, reset the file for the next read */
            	if( mBufferLength < mBuffer.length && mLoop )
            	{
            		close();
            		open();
            		
            		reset = true;
            	}
        	}
        	
        	if( mBufferLength > 0 )
        	{
            	mBufferWrapper = ByteBuffer.wrap( mBuffer ).order(
            					ByteOrder.LITTLE_ENDIAN ).asShortBuffer();
        	}
        	else
        	{
        		mBufferWrapper = null;
        	}
        }
    }
    
    private boolean next( boolean broadcast ) throws IOException
    {
    	boolean success = false;
    	
    	if( mBufferPointer >= mBufferLength )
    	{
    		readBuffer();
    	}
    	
    	if( mBufferLength != 0 && 
    		mBufferWrapper != null &&
    		mBufferWrapper.hasRemaining() )
    	{
    		short i = mBufferWrapper.get();
    		short q = mBufferWrapper.get();

    		mBufferPointer++;
    		incrementCurrentLocation( 1, false );

    		if( broadcast )
    		{
        		send( new ComplexSample( (float)i, (float)q ) );
    		}
    		
    		success = true;
    	}
    	
    	return success;
    }
    
    public boolean next() throws IOException
    {
    	return next( true );
    }
    
    /**
     * Reads the next (count) samples from the wave file and broadcasts them
     * to the registered listeners.
     * @param count - number of samples to broadcast
     * @return - true if successful
     * @throws IOException for any IO issues
     */
    public boolean next( int count ) throws IOException
    {
    	for( int x = 0; x < count; x++ )
    	{
    		next();
    	}
    	
    	return true;
    }

	@Override
    public void jumpTo( long index ) throws IOException
    {
		if( index < mCurrentPosition )
		{
			close();
			open();
		}
		
		while( mCurrentPosition < index )
		{
			next( false );
		}
		
		incrementCurrentLocation( 0, true );
    }

	/**
     * Broadcasts a sample to the registered listeners
     */
    private void send( ComplexSample sample )
    {
        Iterator<Listener<ComplexSample>> it = mListeners.iterator();

        while( it.hasNext() )
        {
            it.next().receive( sample );
        }
    }

    /**
     * Adds a new listener to receive samples as they are read from the wave file
     */
    public void addListener( Listener<ComplexSample> listener )
    {
        mListeners.add( listener );
    }

    public void removeListener( Listener<ComplexSample> listener )
    {
		mListeners.remove( listener );
    }

	@Override
    public void dispose()
    {
		/* Method not implemented */
    }
}
