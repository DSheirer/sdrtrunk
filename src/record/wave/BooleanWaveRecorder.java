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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.sampled.AudioFormat;

import log.Log;
import record.Recorder;
import record.RecorderType;
import sample.Listener;
import util.waveaudio.WaveWriter;
import buffer.FloatSampleBufferAssembler;

/**
 * Threaded WAVE audio recorder for recording mono short samples to a wave file
 */
public class BooleanWaveRecorder extends Recorder implements Listener<Boolean>
{
	private AudioFormat mAudioFormat;
    private boolean mRunning = false;
    private boolean mPaused = false;
    private WaveWriter mWriter;
    private String mFileName;
    private BufferProcessor mBufferProcessor;

    //Sample buffer queue ... constrained to 512 sample buffers (arbitrary size)
	private LinkedBlockingQueue<ByteBuffer> mToWriteBufferQueue = 
								new LinkedBlockingQueue<ByteBuffer>( 512 );
	private FloatSampleBufferAssembler mCurrentBuffer;
	
	public BooleanWaveRecorder( int sampleRate, String filename )
	{
		super( RecorderType.AUDIO );
		
		mAudioFormat = 	new AudioFormat( sampleRate,  //SampleRate
										 16,     //Sample Size
										 1,      //Channels
										 true,   //Signed
										 false ); //Little Endian

		mCurrentBuffer = new FloatSampleBufferAssembler( sampleRate );
				
		mFileName = filename;
	}
	
	@Override
	public String getFileName()
	{
		return mFileName + ".wav";
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
    		boolean success = mToWriteBufferQueue.offer( buffer );

    		if( !success )
    		{
    			Log.error( "Wave recorder buffer overflow - throwing away " + buffer.capacity() + " samples" );
    		}
    	}
    }
    
	@Override
    public void receive( Boolean sample )
    {
		/**
		 * If the sample is true, put short value of 5000, otherwise
		 * put a short value of -5000
		 */
		mCurrentBuffer.put( sample ? 5000 : (short)-5000 );
		
		if( !mCurrentBuffer.hasRemaining() )
		{
			//Dispatch the buffer to be written to disk
			if( !mToWriteBufferQueue.offer( mCurrentBuffer.get() ) )
			{
				//We had an overflow ... log it
				Log.error( "ShortWaveRecorder: buffer overflow - throwing away 1 second of sample data" );
			}
			
			mCurrentBuffer.reset();
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
	                	mWriter.write( mToWriteBufferQueue.take() );
	                }
                }
				catch ( IOException ioe )
				{
					Log.error( "IOException while trying to write to the wave writer - " + ioe.getLocalizedMessage() );
				}
                catch ( InterruptedException e )
                {
                	Log.error( "Oops ... error while processing the buffer queue for " + 
                			" the wave recorder - " + e.getLocalizedMessage() );
                }
			}
    	}
    }

}
