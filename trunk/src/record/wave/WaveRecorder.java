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
package record.wave;

import gui.SDRTrunk;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.sampled.AudioFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import record.Recorder;
import record.RecorderType;
import util.waveaudio.WaveWriter;

/**
 * Threaded WAVE audio recorder.
 */
public class WaveRecorder extends Recorder
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( WaveRecorder.class );

	private AudioFormat mAudioFormat;
    private boolean mRunning = false;
    private boolean mPaused = false;
    private WaveWriter mWriter;
    private String mFileName;
    private BufferProcessor mBufferProcessor;

    //Sample buffer queue ... constrained to 512 sample buffers (arbitrary size)
	LinkedBlockingQueue<ByteBuffer> mBufferQueue = 
								new LinkedBlockingQueue<ByteBuffer>( 512 );

	public WaveRecorder( AudioFormat format, String filename )
	{
		super( RecorderType.AUDIO );
		
		mAudioFormat = format;
		mFileName = filename;
	}
	
	public void start() throws IOException
	{
		if( !mRunning )
		{
			mWriter = new WaveWriter( mFileName + ".wav", mAudioFormat );
			mWriter.start();
			
			mBufferProcessor = new BufferProcessor();
			mBufferProcessor.start();
			
			mRunning = true;
		}
	}
	
	public void stop() throws IOException
	{
		if( mRunning )
		{
			mBufferProcessor = null;
			
			mWriter.stop();
			mWriter = null;
			
			mRunning = false;
		}
	}
	
	public void pause()
	{
		mPaused = true;
	}
	
	public void resume()
	{
		mPaused = false;
	}

	/**
	 * Assumes that the byte[] represents samples corresponding to the 
	 * audio format object defined at construction
	 */
    public void receive( ByteBuffer buffer )
    {
    	if( mRunning && !mPaused )
    	{
    		boolean success = mBufferQueue.offer( buffer );

    		if( !success )
    		{
    			mLog.error( "Wave recorder buffer overflow - throwing away " + 
    					buffer.capacity() + " samples" );
    		}
    	}
    }
    
    public class BufferProcessor extends Thread
    {
    	public void run()
    	{
			while( true )
			{
				try
                {
	                if( mWriter != null )
	                {
	                	mWriter.write( mBufferQueue.take() );
	                }
                }
				catch ( IOException ioe )
				{
					mLog.error( "IOException while trying to write to the "
							+ "wave writer", ioe );
				}
                catch ( InterruptedException e )
                {
                	mLog.error( "Oops ... error while processing the buffer"
                			+ " queue for the wave recorder", e );
                }
			}
    	}
    }
}
