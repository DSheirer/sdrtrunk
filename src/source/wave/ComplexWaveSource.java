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
package source.wave;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.ConversionUtils;
import sample.Listener;
import sample.complex.ComplexBuffer;
import source.ComplexSource;
import source.IControllableFileSource;
import source.IFrameLocationListener;

public class ComplexWaveSource extends ComplexSource implements IControllableFileSource
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( ComplexWaveSource.class );

	private IFrameLocationListener mFrameLocationListener;
	private int mBytesPerFrame;
	private int mFrameCounter = 0;
    private long mFrequency = 0;
    private Listener<ComplexBuffer> mListener;
    private AudioInputStream mInputStream;
    private File mFile;
    
    public ComplexWaveSource( File file ) throws IOException 
    {
    	mFile = file;
    }
    
	@Override
	public void reset()
	{
		stop();
		start();
	}

	@Override
	public void start()
	{
		try
		{
			open();
		} 
		catch ( IOException | UnsupportedAudioFileException e )
		{
			mLog.error( "Error starting complex wave source" );
		}
	}

	@Override
	public void stop()
	{
		try
		{
			close();
		} 
		catch ( IOException e )
		{
			mLog.error( "Error stopping complex wave source" );
		}
	}

	@Override
	public long getFrameCount() throws IOException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
    public int getSampleRate()
    {
		if( mInputStream != null )
		{
			return (int)mInputStream.getFormat().getSampleRate();
		}
		
		return 0;
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
     * Closes the source file
     */
    public void close() throws IOException
    {
    	if( mInputStream != null )
    	{
        	mInputStream.close();
    	}
    	else
    	{
    		throw new IOException( "Can't close wave source - was not opened" );
    	}
    	
    	mInputStream = null;
    }

    /**
     * Opens the source file for reading
     */
    public void open() throws IOException, UnsupportedAudioFileException
    {
    	if( mInputStream == null )
    	{
        	mInputStream = AudioSystem.getAudioInputStream( mFile );
    	}
    	else
    	{
    		throw new IOException( "Can't open wave source - is already opened" );
    	}


        AudioFormat format = mInputStream.getFormat();
        
        mBytesPerFrame = format.getFrameSize();

        if( format.getChannels() != 2 || format.getSampleSizeInBits() != 16 )
        {
        	throw new IOException( "Unsupported Wave Format - EXPECTED: 2 " +
        		"channels 16-bit samples FOUND: " + 
    			mInputStream.getFormat().getChannels() + " channels " + 
        		mInputStream.getFormat().getSampleSizeInBits() + "-bit samples" );
        }

        /* Broadcast that we're at frame location 0 */
        broadcast( 0 );
    }

    /**
     * Reads the number of frames and sends a buffer to the listener
     */
	@Override
	public void next( int frames ) throws IOException
	{
		next( frames, true );
	}

	/**
	 * Reads the number of frames and optionally sends the buffer to the listener
	 */
    public void next( int frames, boolean broadcast ) throws IOException
    {
        if( mInputStream != null )
        {
        	byte[] buffer = new byte[ mBytesPerFrame * frames ];
        	
        	/* Fill the buffer with samples from the file */
        	int samplesRead = mInputStream.read( buffer );

        	mFrameCounter += samplesRead;
        	
        	broadcast( mFrameCounter );
        	
        	if( broadcast && mListener != null )
        	{
            	if( samplesRead < buffer.length )
            	{
            		buffer = Arrays.copyOf( buffer, samplesRead );
            	}

            	float[] samples = ConversionUtils
            			.convertFromSigned16BitSamples( buffer );
            	
            	mListener.receive( new ComplexBuffer( samples ) );
        	}
        }
    }
    
    /**
     * Registers the listener to receive sample buffers as they are read from 
     * the wave file
     */
	@Override
	public void setListener( Listener<ComplexBuffer> listener )
	{
        mListener = listener;
	}

	/**
	 * Unregisters the listener from receiving sample buffers
	 */
    public void removeListener( Listener<ComplexBuffer> listener )
    {
    	mListener = null;
    }

	@Override
    public void dispose()
    {
		mListener = null;
    }

	@Override
	public File getFile()
	{
		return mFile;
	}

	private void broadcast( int byteLocation )
	{
		int frameLocation = (int)( byteLocation / mBytesPerFrame );
		
		if( mFrameLocationListener != null )
		{
			mFrameLocationListener.frameLocationUpdated( frameLocation );
		}
	}
	
	@Override
	public void setListener( IFrameLocationListener listener )
	{
		mFrameLocationListener = listener;
	}

	@Override
	public void removeListener( IFrameLocationListener listener )
	{
		mFrameLocationListener = null;
	}
}
