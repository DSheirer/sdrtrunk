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
import java.nio.ByteOrder;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.sampled.AudioFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import record.Recorder;
import record.RecorderType;
import sample.Listener;
import sample.real.RealBuffer;
import sample.real.RealSampleListener;
import util.waveaudio.WaveWriter;
import buffer.FloatSampleBufferAssembler;

/**
 * Threaded WAVE audio recorder for recording mono float samples to a wave file
 */
public class FloatWaveRecorder extends Recorder implements RealSampleListener,
		Listener<RealBuffer>
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( FloatWaveRecorder.class );

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
	
	private Listener<RealBuffer> mListener;
	
	public FloatWaveRecorder( int sampleRate, String filename )
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
	
	public void setListener( Listener<RealBuffer> listener )
	{
		mListener = listener;
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
    			mLog.error( "Wave recorder buffer overflow - throwing away " + 
    							buffer.capacity() + " samples" );
    		}
    	}
    }

	@Override
	public void receive( RealBuffer buffer )
	{
		//Dispatch the buffer to be written to disk
		if( !mToWriteBufferQueue.offer( convert( buffer ) ) )
		{
			//We had an overflow ... log it
			mLog.error( "buffer overflow - throwing away a buffer of sample data" );
		}
		
		if( mListener != null )
		{
			mListener.receive( buffer );
		}
	}

	/**
	 * Converts the float samples in a complex buffer to a little endian 16-bit
	 * buffer
	 */
	public static ByteBuffer convert( RealBuffer buffer )
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
	
	@Override
    public void receive( float sample )
    {
		mCurrentBuffer.put( sample );
		
		if( !mCurrentBuffer.hasRemaining() )
		{
			//Dispatch the buffer to be written to disk
			if( !mToWriteBufferQueue.offer( mCurrentBuffer.get() ) )
			{
				//We had an overflow ... log it
				mLog.error( "FloatWaveRecorder: buffer overflow - throwing "
						+ "away 1 second of sample data" );
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
					mLog.error( "IOException while trying to write to the wave "
							+ "writer", ioe );
				}
                catch ( InterruptedException e )
                {
                	mLog.error( "Oops ... error while processing the buffer "
                			+ "queue for the wave recorder", e );
                }
			}
    	}
    }
}
