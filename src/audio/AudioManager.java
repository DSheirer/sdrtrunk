package audio;

/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
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

import java.util.ArrayList;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Broadcaster;
import sample.Listener;
import source.mixer.MixerChannel;
import source.mixer.MixerChannelConfiguration;
import audio.AudioEvent.Type;
import audio.output.AudioOutput;
import audio.output.MonoAudioOutput;
import audio.output.StereoAudioOutput;
import controller.ThreadPoolManager;
import controller.ThreadPoolManager.ThreadType;

public class AudioManager implements Listener<AudioPacket>, IAudioController
{
	private static final Logger mLog = LoggerFactory.getLogger( AudioManager.class );

	public static final int AUDIO_TIMEOUT = 2000; //2 seconds
	public static final int NO_SOURCE = 0;
	
	public static final AudioEvent CONFIGURATION_CHANGE_STARTED = 
			new AudioEvent( Type.AUDIO_CONFIGURATION_CHANGE_STARTED, null );
	
	public static final AudioEvent CONFIGURATION_CHANGE_COMPLETE = 
			new AudioEvent( Type.AUDIO_CONFIGURATION_CHANGE_COMPLETE, null );

	private LinkedTransferQueue<AudioPacket> mAudioPacketQueue = new LinkedTransferQueue<>();
	private Map<Integer,AudioChannelAssignment> mChannelAssignments = new HashMap<>();
	private int mLowestPrioritySource;

	private List<AudioOutput> mAvailableAudioOutputs = new ArrayList<>();
	private Map<String,AudioOutput> mAudioOutputMap = new HashMap<>();
	private List<String> mChannelNames = new ArrayList<>();
	
	private Broadcaster<AudioEvent> mControllerBroadcaster = new Broadcaster<>();
	
	private ThreadPoolManager mThreadPoolManager;
	private ScheduledFuture<?> mProcessingTask;
	
	private MixerChannelConfiguration mMixerChannel;

	/**
	 * Processes all audio produced by the decoding channels and routes audio 
	 * packets to any combination of outputs based on any alias audio routing
	 * options specified by the user.
	 */
	public AudioManager( ThreadPoolManager manager )
	{
		mThreadPoolManager = manager;

		//TODO: save audio system preferences to the settings manager
		
		/* Use the system default mixer and mono channel as default startup */
		Mixer mixer = AudioSystem.getMixer( null );
		
		if( mixer != null )
		{
			mMixerChannel = new MixerChannelConfiguration( mixer, MixerChannel.MONO );
			
			try
			{
				setMixerChannelConfiguration( mMixerChannel );
			} 
			catch ( AudioException e )
			{
				mLog.error( "Couldn't create an audio output device", e );
			}
		}
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
					/* Don't process any packet's marked as do not monitor */
					if( !packet.getAudioMetadata().isDoNotMonitor() )
					{
						int source = packet.getAudioMetadata().getSource();

						if( packet.getType() != AudioPacket.Type.END )
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
	
	public void endAllAudioAssignments()
	{
		for( AudioOutput output: mAudioOutputMap.values() )
		{
			output.receive( new AudioPacket( AudioPacket.Type.END, null ) );
		}
	}
	
	public class AudioChannelAssignment implements Listener<AudioEvent>
	{
		private static final long CALL_END_DELAY = 2000; //2 seconds
		
		private AudioOutput mAudioOutput;
		private int mPriority;
		private long mAudioStoppedTime = System.currentTimeMillis();
		
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
				case AUDIO_STOPPED:
					mAudioStoppedTime = System.currentTimeMillis();
					break;
				default:
					break;
			}
		}
	}
	
	@Override
	public void setMixerChannelConfiguration( MixerChannelConfiguration entry ) throws AudioException
	{
		if( entry != null &&
			( entry.getMixerChannel() == MixerChannel.MONO || 
			  entry.getMixerChannel() == MixerChannel.STEREO ) )
		{
			mControllerBroadcaster.broadcast( CONFIGURATION_CHANGE_STARTED );
			
			if( mThreadPoolManager != null && mProcessingTask != null )
			{
				mThreadPoolManager.cancel( mProcessingTask );
			}

			endAllAudioAssignments();
			
			for( Entry<Integer,AudioChannelAssignment> e: mChannelAssignments.entrySet() )
			{
				e.getValue().getAudioOutput().dispose();
			}
			mChannelAssignments.clear();
			
			for( AudioOutput output: mAvailableAudioOutputs )
			{
				output.dispose();
			}
			mAvailableAudioOutputs.clear();

			mChannelNames.clear();
			mAudioOutputMap.clear();

			switch( entry.getMixerChannel() )
			{
				case MONO:
					AudioOutput mono = new MonoAudioOutput( entry.getMixer() );
					mAvailableAudioOutputs.add( mono );
					mChannelNames.add( mono.getChannelName() );
					mAudioOutputMap.put( mono.getChannelName(), mono );
					break;
				case STEREO:
					AudioOutput left = new StereoAudioOutput( entry.getMixer(), MixerChannel.LEFT );
					mAvailableAudioOutputs.add( left );
					mChannelNames.add( left.getChannelName() );
					mAudioOutputMap.put( left.getChannelName(), left );
					
					AudioOutput right = new StereoAudioOutput( entry.getMixer(), MixerChannel.RIGHT );
					mAvailableAudioOutputs.add( right );
					mChannelNames.add( right.getChannelName() );
					mAudioOutputMap.put( right.getChannelName(), right );
					break;
				default:
					throw new AudioException( "Unsupported mixer channel "
							+ "configuration: " + entry.getMixerChannel() );
			}

			mProcessingTask = mThreadPoolManager.scheduleFixedRate( 
					ThreadType.AUDIO_PROCESSING, new AudioPacketProcessor(), 15, 
						TimeUnit.MILLISECONDS );

			mControllerBroadcaster.broadcast( CONFIGURATION_CHANGE_COMPLETE );
		}
	}

	@Override
	public MixerChannelConfiguration getMixerChannelConfiguration() throws AudioException
	{
		return mMixerChannel;
	}

	@Override
	public List<AudioOutput> getAudioOutputs()
	{
		List<AudioOutput> outputs = new ArrayList<>( mAudioOutputMap.values() );

		Collections.sort( outputs, new Comparator<AudioOutput>()
		{
			@Override
			public int compare( AudioOutput first, AudioOutput second )
			{
				return first.getChannelName().compareTo( second.getChannelName() );
			}
		}  );
		
		return outputs;
	}

	@Override
	public void addControllerListener( Listener<AudioEvent> listener )
	{
		mControllerBroadcaster.addListener( listener );
	}

	@Override
	public void removeControllerListener( Listener<AudioEvent> listener )
	{
		mControllerBroadcaster.removeListener( listener );
	}
}
