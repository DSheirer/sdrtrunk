package decode.p25.audio;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.FloatControl.Type;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import message.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import audio.AudioType;
import audio.IAudioOutput;
import audio.IAudioTypeListener;
import audio.SquelchListener;
import controller.ResourceManager;
import controller.ThreadPoolManager;
import controller.ThreadPoolManager.ThreadType;
import decode.p25.message.ldu.LDUMessage;

/**
 * IMBE Audio Output converter.  Processes 18-byte IMBE audio frames and
 * produces PCM 48k MONO 16-bit sample output.  
 * 
 * Note: this class depends on an external imbe format conversion provider that
 * is provided by an external library like JMBE, and that library must be on the
 * classpath and discoverable at runtime.
 */
public class P25AudioOutput implements IAudioOutput, IAudioTypeListener,
		SquelchListener, Listener<Message>
{
	private final static Logger mLog = LoggerFactory.getLogger( P25AudioOutput.class );

	/* 1 IMBE frame = 160 samples/320 bytes @ 8kHz or 1920 bytes at 48kHz */
	private static final int PROCESSED_AUDIO_FRAME_SIZE = 320;
	private IMBETargetDataLine mIMBETargetDataLine = new IMBETargetDataLine();
	private SourceDataLine mOutput;
	private AudioInputStream mIMBEToPCMConverter;
//	private AudioInputStream mUpsampler;
	private ArrayBlockingQueue<byte[]> mIMBEFrameQueue = 
			new ArrayBlockingQueue<byte[]>( 100 );
	private ArrayBlockingQueue<byte[]> mProcessedAudioQueue = 
			new ArrayBlockingQueue<byte[]>( 100 );
	
	private AtomicBoolean mConverting = new AtomicBoolean();
	private AtomicBoolean mDispatching = new AtomicBoolean();
	private AtomicBoolean mEnabled = new AtomicBoolean();
	private boolean mCanProcessAudio = false;
	
	private ThreadPoolManager mThreadPoolManager;
	
	public P25AudioOutput( ResourceManager resourceManager )
	{
		loadConverter();
		
		if( mCanProcessAudio )
		{
			mThreadPoolManager = resourceManager.getThreadPoolManager();

			/* Schedule the imbe frame processor to run slightly faster than 6
			 * times a second.  Each LDU message contains 9 frames and the 
			 * frame rate is 50, so we need to run at least 6 times a second */ 
			mThreadPoolManager.scheduleFixedRate( ThreadType.SOURCE_SAMPLE_PROCESSING, 
					new IMBEFrameConverter(), 150, TimeUnit.MILLISECONDS );
		}
	}

	/**
	 * Primary inject point for p25 imbe audio frame messages.  Each LDU audio 
	 * message contains 9 imbe voice frames.
	 */
	public void receive( Message message )
	{
		if( mCanProcessAudio )
		{
			if( message instanceof LDUMessage )
			{
				for( byte[] frame: ((LDUMessage)message).getIMBEFrames() )
				{
					if( !mIMBEFrameQueue.offer( frame ) )
					{
						mLog.debug( "IMBE frame queue full - throwing away imbe audio frame" );
					}
				}
				
			}
		}
	}

	/**
	 * Receives processed audio buffers from the imbe frame converter and queues
	 * them for dispatch.  If the dispatcher is not currently running, this 
	 * method will start it.
	 */
	private void receive( byte[] audio )
	{
		mProcessedAudioQueue.offer( audio );
		
		if( !mDispatching.get() )
		{
			mThreadPoolManager.scheduleOnce( new AudioDispatcher(), 0, TimeUnit.MILLISECONDS );
		}
	}

	/**
	 * Loads audio frame processing chain.  Constructs an imbe targetdataline
	 * to receive the raw imbe frames.  Adds an IMBE to 8k PCM format conversion
	 * stream wrapper.  Finally, adds an upsampling (8k to 48k) stream wrapper.
	 */
	private void loadConverter()
	{
		try
		{
			mIMBEToPCMConverter = AudioSystem
				.getAudioInputStream( IMBEAudioFormat.PCM_SIGNED_8KHZ_16BITS, 
						new AudioInputStream( mIMBETargetDataLine ) );
			
			if( mIMBEToPCMConverter != null )
			{
				mLog.debug( "IMBE audio converter library loaded successfully" );
//				mUpsampler = AudioSystem
//					.getAudioInputStream( IMBEAudioFormat.PCM_SIGNED_48KHZ_16BITS, 
//								mIMBEToPCMConverter );
//
//				if( mUpsampler != null )
//				{
//					mLog.debug( "Resampler loaded successfully" );
					
					try
			        {
				        mOutput = AudioSystem.getSourceDataLine( IMBEAudioFormat.PCM_SIGNED_8KHZ_16BITS );

				        if( mOutput != null )
			        	{
							mLog.debug( "Audio output line loaded successfully" );
							
				        	/* Open the audio line with room for two buffers */ 
				        	mOutput.open( IMBEAudioFormat.PCM_SIGNED_8KHZ_16BITS, 
				        			PROCESSED_AUDIO_FRAME_SIZE * 2 );
				        	
				        	Control[] controls = mOutput.getControls();

				        	FloatControl gain = (FloatControl)mOutput
				        			.getControl( FloatControl.Type.MASTER_GAIN );

				        	if( gain != null )
				        	{
//				        		gain.setValue( 4.0f );
				        	}
				        	
							mCanProcessAudio = true;
			        	}
				        else
				        {
							mLog.debug( "Couldn't create output line" );
				        }
			        }
			        catch ( LineUnavailableException e )
			        {
			        	mLog.error( "AudioOutput - couldn't open audio speakers "
			        			+ "for playback", e );
			        }
					
//				}
//				else
//				{
//					mIMBEToPCMConverter = null;
//					
//					mLog.debug( "couldn't load Resampler" );
//				}
			}
			else
			{
				mLog.debug( "could not load IMBE audio converter library" );
			}
		}
		catch( IllegalArgumentException iae )
		{
			mLog.equals( "couldn't find/load IMBE to PCM audio converter" );
		}
	}
	
	@Override
	public void setSquelch( SquelchState state )
	{
		/* Ignored */
	}

	@Override
	public void setAudioPlaybackEnabled( boolean enabled )
	{
		mEnabled.set( enabled );
	}

	@Override
	public void dispose()
	{
		mCanProcessAudio = false;
		
		
		mIMBEFrameQueue.clear();
		mProcessedAudioQueue.clear();

		mConverting.set( false );
		mDispatching.set( false );
		
		mOutput.close();
		mOutput = null;
		
		mIMBETargetDataLine.close();
		mIMBETargetDataLine = null;
		
		
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAudioType( AudioType type )
	{
		//Not implemented
	}

	private class IMBEFrameConverter implements Runnable
	{
		@Override
		public void run()
		{
			if( mConverting.compareAndSet( false, true ) )
			{
				boolean processing = true;
				
				while( processing )
				{
					byte[] frame = mIMBEFrameQueue.poll();
					
					if( frame != null )
					{
						if( frame.length == 18 )
						{
							/* Insert an 18-byte imbe frame into the stream */
							mIMBETargetDataLine.receive( frame );
							
							/* Read 1920 bytes of converted upsamped audio from output */
							byte[] audio = new byte[ PROCESSED_AUDIO_FRAME_SIZE ];
							
							try
							{
//								mLog.debug( "reading converted audio bytes - available: " + mUpsampler.available() );
//								int read = mUpsampler.read( audio );
								int read = mIMBEToPCMConverter.read( audio );
								
								if( read > 0 )
								{
									receive( audio );
								}
								else
								{
									mLog.debug( "Couldn't read audio data from "
										+ "conversion stream after inserting imbe "
										+ "frame" );
								}
							} 
							catch ( IOException e )
							{
								mLog.error( "Error reading processed audio data "
									+ "from imbe conversion stream", e );
							}
						}
						else
						{
							mLog.equals( "We got an imbe frame that's not 18 bytes - length:" + ( frame == null ? "null" : frame.length ) );
						}
					}
					else
					{
						processing = false;
					}
				}
				
				mConverting.set( false );
			}
		}
	}
	
	private class AudioDispatcher implements Runnable
	{
		private int mEmptyFrameCount = 0;
		
		@Override
		public void run()
		{
			if( mDispatching.compareAndSet( false, true ) )
			{
				mOutput.drain();

				/* Write 2 frames of audio and then start the line */
				writeAudioFrame();
				writeAudioFrame();

				mOutput.start();
				
				while( mEmptyFrameCount < 3 )
				{
					writeAudioFrame();
				}

				mOutput.stop();
				
				mDispatching.set( false );
			}
		}
		
		private void writeAudioFrame()
		{
			byte[] audio = mProcessedAudioQueue.poll();
			
			if( audio == null )
			{
				mEmptyFrameCount++;
			}

			/* Send empty audio if we're not currently enabled to playback */
			if( audio == null || !mEnabled.get() )
			{
				audio = new byte[ PROCESSED_AUDIO_FRAME_SIZE ];
			}
			
			if( mOutput != null )
			{
				mOutput.write( audio, 0, audio.length );
			}
		}
	}
}

