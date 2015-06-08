package decode.p25.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
import javax.sound.sampled.Control.Type;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import audio.AudioFormats;

public class IMBETargetDataLine implements TargetDataLine
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( IMBETargetDataLine.class );
	
	public static final int BUFFER_SIZE = 18;
	
	public byte[] mFrame;
	
	/**
	 * IMBE target dataline provides a java audio system compatible interface to 
	 * ingest and buffer raw IMBE audio frames and expose a standard audio
	 * system interface for follow-on audio processors to consume the output audio 
	 * stream provided by this interface.
	 */
	public IMBETargetDataLine()
	{
	}

	@Override
	public void open( AudioFormat format, int bufferSize )
			throws LineUnavailableException
	{
		if( format != AudioFormats.IMBE_AUDIO_FORMAT )
		{
			throw new LineUnavailableException( "Unsupported format" );
 		}
	}
	
	@Override
	public void open() throws LineUnavailableException
	{
		open( AudioFormats.IMBE_AUDIO_FORMAT, BUFFER_SIZE );
	}

	@Override
	public void close()
	{
	}

	@Override
	public boolean isOpen()
	{
		return true;
	}

	@Override
	public void open( AudioFormat format ) throws LineUnavailableException
	{
		open( format, BUFFER_SIZE );
	}

	@Override
	public int read( byte[] buffer, int offset, int length )
	{
		if( mFrame != null )
		{
			System.arraycopy( mFrame, 0, buffer, 0, mFrame.length );
			
			return mFrame.length;
		}
		
		return 0;
	}
	
//	private int getBytesAvailable()
//	{
//		return 1920;
//		return mFrame.length;
//	}

	/**
	 * Primary inject point for submitting IMBE frame data into the audio
	 * system via this data line
	 */
	public void receive( byte[] data )
	{
		mFrame = data;
	}

	/**
	 * Not implemented
	 */
	@Override
	public void drain()
	{
	}

	/**
	 * Flushes (deletes) any buffered audio frames.  If a frame is currently 
	 * being processed it will continue and a stop event will be broadcast 
	 * after the current frame has been dispatched.
	 */
	@Override
	public void flush()
	{
		mFrame = null;
	}

	/**
	 * If the line is open, sets the running state to true and attempts to 
	 * retrieve a frame from the buffer.  If successful, a line start event is
	 * broadcast.  If no frames are currently buffered, then a line start event
	 * will be broadcast once the first frame is received.
	 * 
	 * If the line is closed, or if the running state is already set to true,
	 * then the start() is ignored.
	 */
	@Override
	public void start()
	{
	}

	/**
	 * Stops this data line, sets running and active states to false and purges
	 * any currently buffered data frames.  Note: the line will remain open 
	 * until close() is invoked and incoming frame data will continue to buffer.
	 */
	@Override
	public void stop()
	{
	}

	/**
	 * Indicates if this line us currently opened, but does not reflect if the
	 * line is currently streaming data.  Use the isActive() method to determine
	 * streaming status.
	 */
	@Override
	public boolean isRunning()
	{
		return true;
	}

	/**
	 * Indicates if this line is currently streaming data
	 */
	@Override
	public boolean isActive()
	{
		return true;
	}

	/**
	 * IMBE AudioFormat produced by this target data line.
	 */
	@Override
	public AudioFormat getFormat()
	{
		return AudioFormats.IMBE_AUDIO_FORMAT;
	}

	/**
	 * Size of the internal buffer in bytes.  This method will not return the 
	 * true size of the buffer if this data line was constructed with a buffer
	 * size other than the default (250 frames) buffer size.
	 */
	@Override
	public int getBufferSize()
	{
		return 18;
	}

	/**
	 * Number of bytes of imbe frame data available.  Value will be the number
	 * of buffered frames times 18 bytes.
	 */
	@Override
	public int available()
	{
		return 18;
	}

	/**
	 * Current number of frames provided by this data line since it was opened.
	 * Counter rolls over once it exceeds the max integer value.
	 */
	@Override
	public int getFramePosition()
	{
		return 0;
	}

	/**
	 * Current number of frames provided by this data line since it was opened
	 */
	@Override
	public long getLongFramePosition()
	{
		return 0;
	}

	/**
	 * Number of microseconds worth of data that has been sourced by this
	 * target data line.  Returns the frame counter times 20,000 microseconds.
	 */
	@Override
	public long getMicrosecondPosition()
	{
		return 0;
	}

	/**
	 * Not implemented
	 */
	@Override
	public float getLevel()
	{
		return AudioSystem.NOT_SPECIFIED;
	}

	@Override
	public javax.sound.sampled.Line.Info getLineInfo()
	{
		return new DataLine.Info( IMBETargetDataLine.class, 
								  AudioFormats.IMBE_AUDIO_FORMAT );
	}

	/**
	 * No controls are implemented for this data line
	 */
	@Override
	public Control[] getControls()
	{
		return null;
	}

	/**
	 * No controls are implemented for this data line
	 */
	@Override
	public boolean isControlSupported( Type control )
	{
		return false;
	}

	/**
	 * No controls are implemented for this data line
	 */
	@Override
	public Control getControl( Type control )
	{
		return null;
	}

	@Override
	public void addLineListener( LineListener listener )
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void removeLineListener( LineListener listener )
	{
		// TODO Auto-generated method stub
	}
}
