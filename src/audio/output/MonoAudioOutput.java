package audio.output;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Control;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import source.mixer.MixerChannel;
import audio.AudioEvent;
import audio.AudioFormats;
import audio.AudioPacket;
import audio.AudioPacket.Type;
import controller.ThreadPoolManager;
import controller.ThreadPoolManager.ThreadType;

/**
 * Mono Audio output with automatic flow control based on the availability of 
 * audio data packets.  
 */
public class MonoAudioOutput extends AudioOutput
{
	private final static Logger mLog = LoggerFactory.getLogger( MonoAudioOutput.class );

	/* Output line buffer size is set at one half second of audio at 48 kHz and
	 * two bytes per sample */
	private final static int BUFFER_SIZE = 48000;
	
	/* The queue processor will run every 20 milliseconds checking for inbound
	 * audio and automatically starting or stopping audio playback.  We set the 
	 * data line available space threshold to start when we're 20 ms away from 
	 * being full, and the stop threshold when 20 ms away from being empty */
	private final static int SAMPLE_SIZE_40_MS = BUFFER_SIZE / 25;
	private final static int START_THRESHOLD = SAMPLE_SIZE_40_MS;
	private final static int STOP_THRESHOLD = BUFFER_SIZE - SAMPLE_SIZE_40_MS;

	private SourceDataLine mOutput;
	private FloatControl mGainControl;
	private BooleanControl mMuteControl;
	private AudioEvent mAudioStartEvent;
	private AudioEvent mAudioStopEvent;
	private AudioEvent mAudioContinuationEvent;
	private ScheduledFuture<?> mProcessorTask;
	
	public MonoAudioOutput( ThreadPoolManager threadPoolManager, Mixer mixer )
	{
		super( threadPoolManager );

		mAudioStartEvent = new AudioEvent( AudioEvent.Type.AUDIO_STARTED, 
				getChannelName() );
		mAudioStopEvent = new AudioEvent( AudioEvent.Type.AUDIO_STOPPED, 
				getChannelName() );
		mAudioContinuationEvent = new AudioEvent( AudioEvent.Type.AUDIO_CONTINUATION, 
				getChannelName() );

		try
		{
			mOutput = (SourceDataLine)mixer.getLine( 
					AudioFormats.MONO_SOURCE_DATALINE_INFO );
			
			if( mOutput != null )
			{
				mOutput.open( AudioFormats.PCM_SIGNED_48KHZ_16BITS_MONO, BUFFER_SIZE );
				mOutput.addLineListener( this );
				mCanProcessAudio = true;
			}
		} 
        catch ( LineUnavailableException e )
		{
        	mLog.error( "Couldn't obtain source data line for 48kHz PCM mono "
        			+ "audio output" );
		}
		
		if( mOutput != null )
		{
			try
			{
				Control gain = mOutput.getControl( FloatControl.Type.MASTER_GAIN );
				mGainControl = (FloatControl)gain;
			}
			catch( IllegalArgumentException iae )
			{
				mLog.warn( "Couldn't obtain MASTER GAIN control for mono line [" + 
					mixer.getMixerInfo().getName() + "]" );
			}
			
			try
			{
				Control mute = mOutput.getControl( BooleanControl.Type.MUTE );
				mMuteControl = (BooleanControl)mute;
			}
			catch( IllegalArgumentException iae )
			{
				mLog.warn( "Couldn't obtain MUTE control for mono line [" + 
					mixer.getMixerInfo().getName() + "]" );
			}
			
			/* Run the queue processor task every 40 milliseconds */
			mProcessorTask = mThreadPoolManager.scheduleFixedRate( 
				ThreadType.AUDIO_PROCESSING, new QueueProcessor(), 
				40, TimeUnit.MILLISECONDS );
		}
	}

	@Override
	public String getChannelName()
	{
		return MixerChannel.MONO.getLabel();
	}

	public void dispose()
	{
		if( mProcessorTask != null && mThreadPoolManager != null )
		{
			mThreadPoolManager.cancel( mProcessorTask );
		}
		
		mProcessorTask = null;
		
		super.dispose();
		
		if( mOutput != null )
		{
			mOutput.close();
		}
		
		mOutput = null;
		mGainControl = null;
		mMuteControl = null;
	}
	
	public class QueueProcessor implements Runnable
	{
		private AtomicBoolean mProcessing = new AtomicBoolean();
		
		@Override
		public void run()
		{
			try
			{
				/* The mProcessing flag ensures that only one instance of the
				 * processor can run at any given time */
				if( mProcessing.compareAndSet( false, true ) )
				{
					List<AudioPacket> packets = new ArrayList<AudioPacket>();
					
					mBuffer.drainTo( packets );
					
					if( packets.size() > 0 )
					{
						for( AudioPacket packet: packets )
						{
							broadcast( mAudioContinuationEvent );
							
							if( mCanProcessAudio )
							{
								if( packet.getType() == Type.AUDIO )
								{
									float[] samples = packet.getAudioBuffer().getSamples();
									
									/* Little-endian byte buffer */
									ByteBuffer buffer = 
										ByteBuffer.allocate( samples.length * 2 )
												.order( ByteOrder.LITTLE_ENDIAN );
									
									ShortBuffer shortBuffer = buffer.asShortBuffer();

									for( float sample: samples )
									{
										shortBuffer.put( (short)( sample * Short.MAX_VALUE ) );
									}
									
									//This is a blocking write
									mOutput.write( buffer.array(), 0, buffer.array().length );

									checkStart();

									broadcast( packet.getAudioMetadata() );

									mLastActivity = System.currentTimeMillis();
								}
							}
						}
					}
					else
					{
						checkStop();
					}
					
					mProcessing.set( false );
				}
			}
			catch( Exception e )
			{
				mLog.error( "Error while processing audio buffers", e );
			}
		}
		
		private void checkStart()
		{
			if( mCanProcessAudio && 
				!mOutput.isRunning() && mOutput.available() <= START_THRESHOLD )
			{
				mOutput.start();
			}
		}
		
		private void checkStop()
		{
			if( mCanProcessAudio && mOutput.isRunning() &&
				mOutput.available() >= STOP_THRESHOLD )
			{
				mOutput.drain();
				mOutput.stop();
			}
		}
	}

	@Override
	public void setMuted( boolean muted )
	{
		if( mMuteControl != null )
		{
			mMuteControl.setValue( muted );
			
			broadcast( new AudioEvent( muted ? AudioEvent.Type.AUDIO_MUTED : 
				AudioEvent.Type.AUDIO_UNMUTED, getChannelName() ) );
		}
	}

	@Override
	public boolean isMuted()
	{
		if( mMuteControl != null )
		{
			return mMuteControl.getValue();
		}
		
		return false;
	}

	@Override
	public FloatControl getGainControl()
	{
		return mGainControl;
	}

	@Override
	public boolean hasGainControl()
	{
		return mGainControl != null;
	}

	@Override
	public void update( LineEvent event )
	{
		LineEvent.Type type = event.getType();

		if( type == LineEvent.Type.START )
		{
			mAudioEventBroadcaster.broadcast( mAudioStartEvent );
		}
		else if( type == LineEvent.Type.STOP )
		{
			mAudioEventBroadcaster.broadcast( mAudioStopEvent );
		}
	}
}