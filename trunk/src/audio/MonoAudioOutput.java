package audio;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import audio.AudioPacket.Type;

/**
 * Audio output with automatic flow control based on the availability of 
 * audio data packets.  
 */
public class MonoAudioOutput implements Listener<AudioPacket>
{
	private final static Logger mLog = LoggerFactory.getLogger( MonoAudioOutput.class );

	/* Output line buffer size is set at one second of audio at 48 kHz and
	 * two bytes per sample */
	private final static int BUFFER_SIZE = 48000;
	
	/* The queue processor will run every 20 milliseconds checking for inbound
	 * audio and automatically starting or stopping audio playback.  We set the 
	 * data line available space threshold to start when we're 20 ms away from 
	 * being full, and the stop threshold when 20 ms away from being empty */
	private final static int SAMPLE_SIZE_40_MS = 48000 / 25;
	private final static int START_THRESHOLD = SAMPLE_SIZE_40_MS;
	private final static int STOP_THRESHOLD = BUFFER_SIZE - SAMPLE_SIZE_40_MS;
	
	private LinkedTransferQueue<AudioPacket> mBuffer = new LinkedTransferQueue<>();

	private ScheduledExecutorService mExecutorService;
	
	private SourceDataLine mOutput;
	
	private boolean mCanProcessAudio;
	
	private AtomicBoolean mMuted = new AtomicBoolean( true );
	private AtomicBoolean mSquelched = new AtomicBoolean( false );

	public MonoAudioOutput()
	{
        try
		{
			mOutput = AudioSystem.getSourceDataLine( 
						AudioFormats.PCM_SIGNED_48KHZ_16BITS );

			if( mOutput != null )
			{
				mOutput.open( AudioFormats.PCM_SIGNED_48KHZ_16BITS, BUFFER_SIZE );
				
				mCanProcessAudio = true;
				
				mExecutorService = Executors.newSingleThreadScheduledExecutor();

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

	public void setMuted( boolean muted )
	{
		mMuted.set( muted );
	}

	public void setSquelched( boolean squelched )
	{
		mSquelched.set( squelched );
	}
	
	
	public void dispose()
	{
		mCanProcessAudio = false;

		mBuffer.clear();
		
		if( mExecutorService != null )
		{
			mExecutorService.shutdown();
		}
		
		if( mOutput != null )
		{
			mOutput.close();
		}
	}

	@Override
	public void receive( AudioPacket packet )
	{
		if( mCanProcessAudio )
		{
			mBuffer.add( packet );
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
					for( AudioPacket packet: packets )
					{
						if( mCanProcessAudio )
						{
							if( mMuted.get() || mSquelched.get() )
							{
								checkStop();
							}
							else if( packet.getType() == Type.AUDIO )
							{
								mOutput.write( packet.getAudioData(), 0, 
										packet.getAudioData().length );
								
								checkStart();
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
			if( mCanProcessAudio && !mMuted.get() && !mSquelched.get() && 
				!mOutput.isRunning() && mOutput.available() <= START_THRESHOLD )
			{
				mOutput.start();
			}
		}
		
		private void checkStop()
		{
			if( mCanProcessAudio && mOutput.isRunning() &&
				( mOutput.available() >= STOP_THRESHOLD || mMuted.get() || mSquelched.get() ) )
			{
				mOutput.drain();
				mOutput.stop();
			}
		}
	}
}
