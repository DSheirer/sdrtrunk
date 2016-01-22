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
import javax.sound.sampled.Control;
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
 * Mono Audio output in stereo format with automatic flow control based on the 
 * availability of audio data packets.  
 */
public class StereoAudioOutput extends AudioOutput
{
	private final static Logger mLog = LoggerFactory.getLogger( StereoAudioOutput.class );

	/* Output line buffer size is set at one half second of audio at 48 kHz and
	 * two bytes per sample for two channels */
	private final static int BUFFER_SIZE = 96000;
	
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
	
	private MixerChannel mMixerChannel;
	
	public StereoAudioOutput( Mixer mixer, MixerChannel channel )
	{
		super();
		
		assert( channel == MixerChannel.LEFT || channel == MixerChannel.RIGHT );
		
		mMixerChannel = channel;

		mAudioStartEvent = new AudioEvent( AudioEvent.Type.AUDIO_STARTED, 
				getChannelName() );
		mAudioStopEvent = new AudioEvent( AudioEvent.Type.AUDIO_STOPPED, 
				getChannelName() );
		mAudioContinuationEvent = new AudioEvent( AudioEvent.Type.AUDIO_CONTINUATION, 
				getChannelName() );

		try
		{
			mOutput = (SourceDataLine)mixer.getLine( 
					AudioFormats.STEREO_SOURCE_DATALINE_INFO );
			
			if( mOutput != null )
			{
				mOutput.open( AudioFormats.PCM_SIGNED_48KHZ_16BITS_STEREO, BUFFER_SIZE );
				mCanProcessAudio = true;
			}
		} 
        catch ( LineUnavailableException e )
		{
        	mLog.error( "Couldn't obtain source data line for 48kHz PCM stereo "
        			+ "audio output - mixer [" + mixer.getMixerInfo().getName() + 
        			"] channel [" + mMixerChannel.name() + "]" );
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
				mLog.warn( "Couldn't obtain MASTER GAIN control for stereo line [" + 
					mixer.getMixerInfo().getName() + " | " + channel.name() + "]" );
			}
			
			try
			{
				Control mute = mOutput.getControl( BooleanControl.Type.MUTE );
				mMuteControl = (BooleanControl)mute;
			}
			catch( IllegalArgumentException iae )
			{
				mLog.warn( "Couldn't obtain MUTE control for stereo line [" + 
					mixer.getMixerInfo().getName() + " | " + channel.name() + "]" );
			}
			
			mExecutorService = Executors.newSingleThreadScheduledExecutor( 
					new NamingThreadFactory( "audio (stereo) output" ) );

			/* Run the queue processor task every 40 milliseconds */
			mExecutorService.scheduleAtFixedRate( new QueueProcessor(), 
					0, 40, TimeUnit.MILLISECONDS );
		}
	}

	@Override
	public String getChannelName()
	{
		return mMixerChannel.getLabel();
	}

	public void dispose()
	{
		super.dispose();
		
		if( mOutput != null )
		{
			mOutput.close();
		}
	}
	
	private ByteBuffer convert( float[] samples )
	{
		/* Little-endian byte buffer */
		ByteBuffer buffer = ByteBuffer.allocate( samples.length * 4 )
				.order( ByteOrder.LITTLE_ENDIAN );
		
		ShortBuffer shortBuffer = buffer.asShortBuffer();

		if( mMixerChannel == MixerChannel.LEFT )
		{
			for( float sample: samples )
			{
				shortBuffer.put( (short)( sample * Short.MAX_VALUE ) );
				shortBuffer.put( (short)0 );
			}
		}
		else
		{
			for( float sample: samples )
			{
				shortBuffer.put( (short)0 );
				shortBuffer.put( (short)( sample * Short.MAX_VALUE ) );
			}
		}
		
		return buffer;
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
									ByteBuffer buffer = convert( 
											packet.getAudioBuffer().getSamples() );
									
									if( mOutput.available() >= buffer.array().length )
									{
										mOutput.write( buffer.array(), 0, 
												buffer.array().length );

										checkStart();
									}
									else
									{
										int wrote = 0;

										int toWrite = buffer.array().length;
										
										while( toWrite > 0 )
										{
											int available = mOutput.available();

											if( available < toWrite )
											{
												if( available > 0 )
												{
													wrote += mOutput.write( buffer.array(), 
															wrote, available );

													checkStart();
												}
											}
											else
											{
												wrote += mOutput.write( buffer.array(), wrote, 
														toWrite );

												checkStart();
											}

											toWrite = buffer.array().length - wrote;
										}
									}
									
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
