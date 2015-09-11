package audio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.FloatControl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import audio.metadata.AudioMetadata;
import audio.output.AudioOutput;
import audio.output.MonoAudioOutput;
import controller.ThreadPoolManager;
import controller.ThreadPoolManager.ThreadType;

public class AudioManager implements Listener<AudioPacket>, IAudioController
{
	private static final Logger mLog = LoggerFactory.getLogger( AudioManager.class );

	public static final int AUDIO_TIMEOUT = 2000; //2 seconds
	public static final int NO_SOURCE = 0;

	private LinkedTransferQueue<AudioPacket> mAudioPacketQueue = new LinkedTransferQueue<>();
	private Map<Integer,AudioChannelAssignment> mChannelAssignments = new HashMap<>();
	private int mLowestPrioritySource;

	private List<AudioOutput> mAvailableAudioOutputs = new ArrayList<>();
	private Map<String,AudioOutput> mChannelMap = new HashMap<>();
	private List<String> mChannelNames = new ArrayList<>();
	private AudioOutput mAudioOutput;
	
	private Listener<AudioEvent> mConfigurationChangeListener;
	
	private ThreadPoolManager mThreadPoolManager;
	private ScheduledFuture<?> mProcessingTask;

	/**
	 * Handles all audio produced by the decoding channels and routes audio 
	 * packets to any combination of outputs based on any alias audio routing
	 * options specified by the user.
	 */
	public AudioManager( ThreadPoolManager manager )
	{
		mThreadPoolManager = manager;

		mAudioOutput = new MonoAudioOutput();
		mAvailableAudioOutputs.add( mAudioOutput );
		mChannelNames.add( mAudioOutput.getChannelName() );
		mChannelMap.put( mAudioOutput.getChannelName(), mAudioOutput );
		
		mProcessingTask = mThreadPoolManager.scheduleFixedRate( 
			ThreadType.AUDIO_PROCESSING, new AudioPacketProcessor(), 15, 
				TimeUnit.MILLISECONDS );
	}
	
	public void dispose()
	{
		if( mThreadPoolManager != null && mProcessingTask != null )
		{
			mThreadPoolManager.cancel( mProcessingTask );
		}
		
		mAudioPacketQueue.clear();
		
		mThreadPoolManager = null;
		mProcessingTask = null;
		
		mAudioOutput.dispose();
	}

	@Override
	public synchronized void receive( AudioPacket packet )
	{
		mAudioPacketQueue.add( packet );
	}
	
	public class AudioPacketProcessor implements Runnable
	{
		@Override
		public void run()
		{
			removeCompletedChannelAssignments();
			
			if( mAudioPacketQueue != null )
			{
				List<AudioPacket> packets = new ArrayList<AudioPacket>();

				mAudioPacketQueue.drainTo( packets );
				
				for( AudioPacket packet: packets )
				{
					int source = packet.getAudioMetadata().getSource();

					if( packet.getType() == AudioPacket.Type.END )
					{
						
					}
					else
					{
						/* Existing channel assignment */
						if( mChannelAssignments.containsKey( source ) )
						{
							mChannelAssignments.get( source ).getAudioOutput()
										.receive( packet );
						}
						/* Start a new channel assignment */
						else if( !mAvailableAudioOutputs.isEmpty() )
						{
							AudioOutput channel = mAvailableAudioOutputs.remove( 0 );
							
							mChannelAssignments.put( packet.getAudioMetadata().getSource(), 
								new AudioChannelAssignment( channel, packet.getAudioMetadata().getPriority() ) );
							
							updateLowestPriorityAssignment();
							
							channel.receive( packet );
						}
						/* Override the lowest priority assignment */
						else
						{
							if( mLowestPrioritySource > -1 )
							{
								int priority = packet.getAudioMetadata().getPriority();

								if( packet.getAudioMetadata().isSelected() || 
									priority < mChannelAssignments
										.get( mLowestPrioritySource ).getPriority() )
								{
									AudioChannelAssignment assignment = 
										mChannelAssignments.remove( mLowestPrioritySource );
									
									if( assignment != null )
									{
										assignment.setPriority( priority );

										int overrideSource = packet.getAudioMetadata().getSource();
										
										mChannelAssignments.put( overrideSource, assignment );
										
										mLowestPrioritySource = overrideSource;
										
										assignment.getAudioOutput().receive( packet );
									}
									else
									{
										mLowestPrioritySource = -1;
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Checks each audio channel assignment and removes any completed channels.
	 */
	private void removeCompletedChannelAssignments()
	{
		if( !mChannelAssignments.isEmpty() )
		{
			Iterator<Entry<Integer,AudioChannelAssignment>> it = 
					mChannelAssignments.entrySet().iterator();

			Entry<Integer,AudioChannelAssignment> entry;

			boolean removed = false;
			
			while( it.hasNext() )
			{
				entry = it.next();
				
				if( entry.getValue().isComplete() )
				{
					mAvailableAudioOutputs.add( entry.getValue().getAudioOutput() );
					entry.getValue().dispose();
					it.remove();
					removed = true;
				}
			}
			
			if( removed )
			{
				updateLowestPriorityAssignment();
			}
		}
	}

	/**
	 * Identifies the lowest priority channel assignment ... the one that can
	 * be overridden with a higher priority call
	 */
	private void updateLowestPriorityAssignment()
	{
		int lowestPriority = -1;
		
		mLowestPrioritySource = -1;
		
		for( int source: mChannelAssignments.keySet() )
		{
			AudioChannelAssignment assignment = mChannelAssignments.get( source );
			
			if( assignment.getPriority() > lowestPriority )
			{
				lowestPriority = assignment.getPriority();
				
				mLowestPrioritySource = source;
			}
		}
	}
	
	public class AudioChannelAssignment implements Listener<AudioEvent>
	{
		private static final long CALL_END_DELAY = 2000; //2 seconds
		
		private AudioOutput mAudioOutput;
		private int mPriority;
		private long mAudioStoppedTime = Long.MAX_VALUE;
		
		public AudioChannelAssignment( AudioOutput audioOutput, int priority )
		{
			mAudioOutput = audioOutput;
			mPriority = priority;

			mAudioOutput.addAudioEventListener( this );
		}
		
		public boolean isComplete()
		{
			return mAudioStoppedTime != Long.MAX_VALUE &&
				   mAudioStoppedTime + CALL_END_DELAY < System.currentTimeMillis();
		}
		
		public void dispose()
		{
			mAudioOutput.removeAudioEventListener( this );
			mAudioOutput = null;
		}
		
		public AudioOutput getAudioOutput()
		{
			return mAudioOutput;
		}

		public int getPriority()
		{
			return mPriority;
		}
		
		public void setPriority( int priority )
		{
			mPriority = priority;
		}

		@Override
		public void receive( AudioEvent event )
		{
			switch( event.getType() )
			{
				case AUDIO_STARTED:
					mAudioStoppedTime = Long.MAX_VALUE;
					break;
				case AUDIO_STOPPED:
					mAudioStoppedTime = System.currentTimeMillis();
					break;
				default:
					break;
			}
		}
	}

	@Override
	public List<String> getAudioChannels()
	{
		return mChannelNames;
	}

	@Override
	public BooleanControl getMuteControl( String channel )
			throws AudioException
	{
		if( mChannelMap.containsKey( channel ) )
		{
			return mChannelMap.get( channel ).getMuteControl();
		}

		throw new AudioException( "Channel [" + channel + "] is not a valid channel" );
	}

	@Override
	public boolean hasMuteControl( String channel )
	{
		return mChannelMap.containsKey( channel ) &&
			   mChannelMap.get( channel ) != null &&
			   mChannelMap.get( channel ).getMuteControl() != null;
	}

	@Override
	public FloatControl getGainControl( String channel ) throws AudioException
	{
		if( mChannelMap.containsKey( channel ) )
		{
			return mChannelMap.get( channel ).getGainControl();
		}

		throw new AudioException( "Channel [" + channel + "] is not a valid channel" );
	}

	@Override
	public boolean hasGainControl( String channel )
	{
		return mChannelMap.containsKey( channel ) &&
			   mChannelMap.get( channel ) != null &&
			   mChannelMap.get( channel ).getGainControl() != null;
	}

	/**
	 * Sets the listener to receive audio events, specifically, audio
	 * configuration change events
	 */
	@Override
	public void setConfigurationChangeListener( Listener<AudioEvent> listener )
	{
		mConfigurationChangeListener = listener;
	}

	@Override
	public void addAudioEventListener( String channel, Listener<AudioEvent> listener ) 
			throws AudioException
	{
		if( mChannelMap.containsKey( channel ) )
		{
			mChannelMap.get( channel ).addAudioEventListener( listener );
		}
		else
		{
			throw new AudioException( "Channel [" + channel + "] is not a valid "
					+ "channel.  Audio event listener was not registered" );
		}
	}

	@Override
	public void removeAudioEventListener( String channel, Listener<AudioEvent> listener )
	{
		if( mChannelMap.containsKey( channel ) )
		{
			mChannelMap.get( channel ).removeAudioEventListener( listener );
		}
	}

	@Override
	public void setAudioMetadataListener( String channel,
			Listener<AudioMetadata> listener )
	{
		if( mChannelMap.containsKey( channel ) )
		{
			mChannelMap.get( channel ).setAudioMetadataListener( listener );
		}
	}

	@Override
	public void removeAudioMetadataListener( String channel )
	{
		if( mChannelMap.containsKey( channel ) )
		{
			mChannelMap.get( channel ).removeAudioMetadataListener();
		}
	}
}
