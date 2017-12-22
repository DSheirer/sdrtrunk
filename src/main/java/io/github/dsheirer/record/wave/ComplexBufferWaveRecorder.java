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
package io.github.dsheirer.record.wave;

import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.Buffer;
import io.github.dsheirer.sample.ConversionUtils;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexBuffer;
import io.github.dsheirer.sample.complex.IComplexBufferListener;
import io.github.dsheirer.util.TimeStamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WAVE audio recorder module for recording complex (I&Q) samples to a wave file
 */
public class ComplexBufferWaveRecorder extends Module
				implements IComplexBufferListener, Listener<ComplexBuffer>
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( ComplexBufferWaveRecorder.class );

    private WaveWriter mWriter;
    private String mFilePrefix;
    private Path mFile;
	private AudioFormat mAudioFormat;
	
	private BufferProcessor mBufferProcessor;
	private ScheduledFuture<?> mProcessorHandle;
	private LinkedBlockingQueue<ComplexBuffer> mBuffers = new LinkedBlockingQueue<>( 500 );
	
	private AtomicBoolean mRunning = new AtomicBoolean();
	
	public ComplexBufferWaveRecorder( int sampleRate, String filePrefix )
	{
		mAudioFormat = 	new AudioFormat( sampleRate,  //SampleRate
										 16,     //Sample Size
										 2,      //Channels
										 true,   //Signed
										 false ); //Little Endian

		mFilePrefix = filePrefix;
	}

	public Path getFile()
	{
		return mFile;
	}
	
	public void start( ScheduledExecutorService executor )
	{
		if( mRunning.compareAndSet( false, true ) )
		{
			if( mBufferProcessor == null )
			{
				mBufferProcessor = new BufferProcessor();
			}

			try
			{
				StringBuilder sb = new StringBuilder();
				sb.append( mFilePrefix );
				sb.append( "_" );
				sb.append( TimeStamp.getTimeStamp( "_" ) );
				sb.append( ".wav" );
				
				mFile = Paths.get( sb.toString() );

				mWriter = new WaveWriter( mAudioFormat, mFile );

				/* Schedule the processor to run every 500 milliseconds */
				mProcessorHandle = executor.scheduleAtFixedRate( 
						mBufferProcessor, 0, 500, TimeUnit.MILLISECONDS );
			}
			catch( IOException io )
			{
				mLog.error( "Error starting complex baseband recorder", io );
			}
		}
	}
	
	public void stop()
	{
		if( mRunning.compareAndSet( true, false ) )
		{
			receive( new PoisonPill() );
		}
	}
	
	@Override
    public void receive( ComplexBuffer buffer )
    {
		if( mRunning.get() )
		{
			boolean success = mBuffers.offer( buffer );
			
			if( !success )
			{
				mLog.error( "recorder buffer overflow - purging [" +
						mFile.toFile().getAbsolutePath() + "]" );
				
				mBuffers.clear();
			}
		}
    }
	
	@Override
	public Listener<ComplexBuffer> getComplexBufferListener()
	{
		return this;
	}

	@Override
	public void dispose()
	{
		stop();
	}

	@Override
	public void reset()
	{
	}
	
	public class BufferProcessor implements Runnable
    {
    	public void run()
    	{
			try
            {
				Buffer buffer = mBuffers.poll();
				
				while( buffer != null )
				{
					if( buffer instanceof PoisonPill )
					{
						buffer = null;
						
						mBuffers.clear();
						
						if( mWriter != null )
						{
							try
							{
								mWriter.close();
								mWriter = null;
							}
							catch( IOException io )
							{
								mLog.error( "Error stopping complex wave recorder [" + 
											getFile() + "]", io );
							}
						}
						
						mFile = null;

						if( mProcessorHandle != null )
						{
							mProcessorHandle.cancel( true );
						}
						
						mProcessorHandle = null;
					}
					else
					{
						mWriter.write( ConversionUtils.convertToSigned16BitSamples( buffer ) );
						buffer = mBuffers.poll();
					}
				}
            }
			catch ( IOException ioe )
			{
				/* Stop this module if/when we get an IO exception */
				mBuffers.clear();
				stop();
				
				mLog.error( "IOException while trying to write to the wave "
						+ "writer", ioe );
			}
    	}
    }

	/**
	 * This is used as a sentinel value to signal the buffer processor to end
	 */
	public class PoisonPill extends ComplexBuffer
	{
		public PoisonPill()
		{
			super( new float[ 1 ] );
		}
	}
}
