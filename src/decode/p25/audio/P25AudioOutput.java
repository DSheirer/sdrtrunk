package decode.p25.audio;

import java.io.IOException;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
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
	private static final int PROCESSED_8KHZ_AUDIO_FRAME_SIZE = 320;
	private static final int PROCESSED_48KHZ_AUDIO_FRAME_SIZE = 1920;
	private int mFrameSize = 1920;
	
	private IMBETargetDataLine mIMBETargetDataLine = new IMBETargetDataLine();
	private SourceDataLine mOutput;
	private AudioInputStream mIMBEToPCMConverter;
	private LinkedTransferQueue<byte[]> mIMBEFrameQueue = 
			new LinkedTransferQueue<byte[]>();
	private LinkedTransferQueue<byte[]> mProcessedAudioQueue = 
			new LinkedTransferQueue<byte[]>();
	
	private AtomicBoolean mConverting = new AtomicBoolean();
	private AtomicBoolean mDispatching = new AtomicBoolean();
	private AtomicBoolean mEnabled = new AtomicBoolean();
	private boolean mCanProcessAudio = false;
	private boolean mEncryptedAudio = false;
	
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
					new IMBEFrameConverter(), 40, TimeUnit.MILLISECONDS );
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
				LDUMessage ldu = (LDUMessage)message;
				
				/* Check valid ldu frames for encryption.  Set state to encrypted
				 * so that no encrypted frames are sent to the converter */
				if( ldu.isValid() )
				{
					mEncryptedAudio = ldu.isEncrypted();
				}

				if( !mEncryptedAudio )
				{
					for( byte[] frame: ((LDUMessage)message).getIMBEFrames() )
					{
						if( !mIMBEFrameQueue.offer( frame ) )
						{
							mLog.debug( "IMBE frame queue full - throwing away "
									+ "imbe audio frame" );
						}
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
	        mOutput = AudioSystem.getSourceDataLine( 
	        		IMBEAudioFormat.PCM_SIGNED_48KHZ_16BITS );

	        if( mOutput != null )
        	{
	        	mFrameSize = PROCESSED_48KHZ_AUDIO_FRAME_SIZE;
	        	
	        	/* Open the audio line with room for two buffers */ 
	        	mOutput.open( IMBEAudioFormat.PCM_SIGNED_48KHZ_16BITS, 
	        				  mFrameSize * 2 );

				mCanProcessAudio = true;
        	}
	        else
	        {
				mLog.debug( "Couldn't obtain a 48kHz source data line "
						+ "- attempting 8kHz rate" );
				
		        mOutput = AudioSystem.getSourceDataLine( 
		        		IMBEAudioFormat.PCM_SIGNED_8KHZ_16BITS );

		        if( mOutput != null )
	        	{
		        	mFrameSize = PROCESSED_8KHZ_AUDIO_FRAME_SIZE;
		        	
		        	/* Open the audio line with room for two buffers */ 
		        	mOutput.open( IMBEAudioFormat.PCM_SIGNED_8KHZ_16BITS, 
		        				  mFrameSize * 2 );
		        	
					mCanProcessAudio = true;
	        	}
		        else
		        {
					mLog.error( "Couldn't obtain a 48 kHz or 8 kHz "
						+ "source data line - no decoded IMBE audio "
						+ "will be available" );
					
					logSourceLines();
		        }
	        }
        }
        catch ( LineUnavailableException lue )
        {
        	mLog.error( "AudioOutput - couldn't open audio output line "
        			+ "for playback", lue );
        }
		catch( IllegalArgumentException iae )
		{
			mLog.error( "Error fetching or opening audio output for "
					+ "48kHz or 8kHz 16 bit audio", iae );
		}
		catch( Exception e )
		{
			mLog.error( "Error with audio output", e );
		}

		if( mCanProcessAudio )
		{
			try
			{
				if( mOutput.getFormat().getSampleRate() == IMBEAudioFormat.PCM_48KHZ_RATE )
				{
					mIMBEToPCMConverter = AudioSystem
							.getAudioInputStream( IMBEAudioFormat.PCM_SIGNED_48KHZ_16BITS, 
									new AudioInputStream( mIMBETargetDataLine ) );
				}
				else
				{
					mIMBEToPCMConverter = AudioSystem
							.getAudioInputStream( IMBEAudioFormat.PCM_SIGNED_8KHZ_16BITS, 
									new AudioInputStream( mIMBETargetDataLine ) );
				}
				
				if( mIMBEToPCMConverter != null )
				{
					mLog.info( "IMBE audio converter library loaded successfully" );
				}
				else
				{
					mLog.info( "could not load IMBE audio converter library" );
				}
			}
			catch( IllegalArgumentException iae )
			{
				mLog.error( "could NOT find/load IMBE audio converter library", iae );
			}
		}
	}
	
	private void logSourceLines()
	{
		Mixer.Info[] infos = AudioSystem.getMixerInfo();
		
		for( Mixer.Info info: AudioSystem.getMixerInfo() )
		{
			Mixer mixer = AudioSystem.getMixer( info );
			
			Line.Info[] lineInfos = mixer.getSourceLineInfo();
			
			for( Line.Info lineInfo: lineInfos )
			{
				mLog.debug( "Source Line: " + lineInfo.toString() );
			}
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
							long start = System.currentTimeMillis();
							
							/* Insert an 18-byte imbe frame into the stream */
							mIMBETargetDataLine.receive( frame );
							
							/* Read one frame of converted audio from output */
							byte[] audio = new byte[ mFrameSize ];
							
							try
							{
								int read = mIMBEToPCMConverter.read( audio );
								
								if( read > 0 )
								{
									mLog.debug( "Conversion Time: " + ( System.currentTimeMillis() - start ) );
									
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
//				mOutput.drain();

				/* Write 2 frames of audio and then start the line */
				writeAudioFrame();
				writeAudioFrame();

				mOutput.start();
				
				while( mEmptyFrameCount < 4 )
				{
					writeAudioFrame();
				}
				
				mLog.debug( "Stopping ..." );

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
				mLog.debug( " ... inserting empty" );
			}
			else
			{
				mEmptyFrameCount = 0;
			}

			/* Send empty audio if we're not currently enabled to playback */
			if( audio == null || !mEnabled.get() )
			{
				audio = new byte[ mFrameSize ];
			}
			
			if( mOutput != null )
			{
				mOutput.write( audio, 0, audio.length );
			}
		}
	}
}

