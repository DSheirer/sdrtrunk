package audio.output;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.FloatControl;
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
import controller.NamingThreadFactory;

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
	
	public MonoAudioOutput( Mixer mixer )
	{
		super();

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
				
				mCanProcessAudio = true;
				
				mGainControl = (FloatControl)mOutput.getControl( 
						FloatControl.Type.MASTER_GAIN );
				
				mMuteControl = (BooleanControl)mOutput.getControl( 
						BooleanControl.Type.MUTE );
				
				mExecutorService = Executors.newSingleThreadScheduledExecutor( 
						new NamingThreadFactory( "audio (mono) output" ) );

				/* Run the queue processor task every 40 milliseconds */
				mExecutorService.scheduleAtFixedRate( new QueueProcessor(), 
						0, 40, TimeUnit.MILLISECONDS );
			}
		} 
        catch ( LineUnavailableException e )
		{
        	mLog.error( "Couldn't obtain source data line for 48kHz PCM mono "
        			+ "audio output" );
		}
	}

	@Override
	public String getChannelName()
	{
		return MixerChannel.MONO.getLabel();
	}

	public void dispose()
	{
		super.dispose();
		
		if( mOutput != null )
		{
			mOutput.close();
		}
	}
	
	public class QueueProcessor implements Runnable
	{
		private AtomicBoolean mProcessing = new AtomicBoolean();
		
		public QueueProcessor()
		{
		}

		@Override
		public void run()
		{
			/* The mProcessing flag ensures that only one instance of the
			 * processor can run at any given time */
			if( mProcessing.compareAndSet( false, true ) )
			{
				List<AudioPacket> packets = new ArrayList<AudioPacket>();
				
				mBuffer.drainTo( packets );
				
				if( packets.size() > 0 )
				{
					broadcast( mAudioContinuationEvent );
					
					for( AudioPacket packet: packets )
					{
						if( mCanProcessAudio )
						{
							if( isSquelched() )
							{
								checkStop();
							}
							else if( packet.getType() == Type.AUDIO )
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
								
								mOutput.write( buffer.array(), 0, 
										buffer.array().length );
								
								checkStart();
								
								broadcast( packet.getAudioMetadata() );
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
		
		private void checkStart()
		{
			if( mCanProcessAudio && !isSquelched() && 
				!mOutput.isRunning() && mOutput.available() <= START_THRESHOLD )
			{
				mOutput.start();
				
				mAudioEventBroadcaster.broadcast( mAudioStartEvent );
			}
		}
		
		private void checkStop()
		{
			if( mCanProcessAudio && mOutput.isRunning() &&
				( mOutput.available() >= STOP_THRESHOLD || isSquelched() ) )
			{
				mOutput.drain();
				mOutput.stop();

				mAudioEventBroadcaster.broadcast( mAudioStopEvent );
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
}
