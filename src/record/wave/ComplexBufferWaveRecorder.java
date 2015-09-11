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
package record.wave;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;

import module.Module;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import sample.complex.ComplexBuffer;
import sample.complex.IComplexBufferListener;
import util.TimeStamp;
import controller.ThreadPoolManager;
import controller.ThreadPoolManager.ThreadType;

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
	
	private ThreadPoolManager mThreadPoolManager;
	private BufferProcessor mBufferProcessor;
	private ScheduledFuture<?> mProcessorHandle;
	private LinkedBlockingQueue<ComplexBuffer> mBuffers = new LinkedBlockingQueue<>( 500 );
	
	public ComplexBufferWaveRecorder( ThreadPoolManager threadPoolManager, 
			int sampleRate, String filePrefix )
	{
		mThreadPoolManager = threadPoolManager;
		
		mAudioFormat = 	new AudioFormat( sampleRate,  //SampleRate
										 16,     //Sample Size
										 2,      //Channels
										 true,   //Signed
										 false ); //Little Endian

		mFilePrefix = filePrefix;
	}

	/**
	 * Converts the float samples in a complex buffer to a little endian 16-bit
	 * buffer
	 */
	public static ByteBuffer convert( ComplexBuffer buffer )
	{
		float[] samples = buffer.getSamples();
		
		ByteBuffer converted = ByteBuffer.allocate( samples.length * 2 );
		converted.order( ByteOrder.LITTLE_ENDIAN );

		for( float sample: samples )
		{
			converted.putShort( (short)( sample * Short.MAX_VALUE ) );
		}
		
		return converted;
	}

	public Path getFile()
	{
		return mFile;
	}
	
	public void start()
	{
		if( !running() )
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
				mProcessorHandle = mThreadPoolManager.scheduleFixedRate( 
					ThreadType.BASEBAND_RECORDING, mBufferProcessor, 500, 
					TimeUnit.MILLISECONDS );
			}
			catch( IOException io )
			{
				mLog.error( "Error starting complex baseband recorder", io );
			}
		}
	}
	
	public boolean running()
	{
		return mProcessorHandle != null;
	}
	
	public void stop()
	{
		if( running() )
		{
			mThreadPoolManager.cancel( mProcessorHandle );
			
			mProcessorHandle = null;
			
			if( mWriter != null )
			{
				try
				{
					mWriter.close();
				}
				catch( IOException io )
				{
					mLog.error( "Error stopping complex wave recorder [" + 
								getFile() + "]", io );
				}
			}

		}
		
		mBuffers.clear();
		mWriter = null;
		mFile = null;
	}
	
	@Override
    public void receive( ComplexBuffer buffer )
    {
		if( running() )
		{
			boolean success = mBuffers.offer( buffer );
			
			if( !success )
			{
				mLog.error( "ComplexWaveRecorder: buffer overflow or error - throwing "
						+ "away a buffer of sample data" );
				stop();
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
		
		mThreadPoolManager = null;
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
				ComplexBuffer buffer = mBuffers.poll();
				
				while( buffer != null )
				{
					mWriter.write( convert( buffer ) );
					buffer = mBuffers.poll();
				}
            }
			catch ( IOException ioe )
			{
				/* Stop this module if/when we get an IO exception */
				stop();
				
				mLog.error( "IOException while trying to write to the wave "
						+ "writer", ioe );
			}
    	}
    }
}
