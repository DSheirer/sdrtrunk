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
package record;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import properties.SystemProperties;
import record.wave.ComplexBufferWaveRecorder;
import record.wave.RealBufferWaveRecorder;
import sample.Listener;
import audio.AudioPacket;
import audio.metadata.Metadata;
import audio.metadata.MetadataType;
import controller.ThreadPoolManager;

public class RecorderManager implements Listener<AudioPacket>
{
	private static final Logger mLog = LoggerFactory.getLogger( RecorderManager.class );

	public static final int AUDIO_SAMPLE_RATE = 48000;
	
	private static final long RECORDER_INACTIVITY_THRESHOLD = 10000; //1 minute in millis
	
//	private static final RealBuffer AUDIO_SEPARATOR = new AudioSeparator();
	
	private Map<String,RealBufferWaveRecorder> mRecorders = new HashMap<>();
	
	private ThreadPoolManager mThreadPoolManager;

	public RecorderManager( ThreadPoolManager threadPoolManager )
	{
		mThreadPoolManager = threadPoolManager;
	}
	
	@Override
	public void receive( AudioPacket audioPacket )
	{
		if( audioPacket.hasAudioMetadata() && 
			audioPacket.getAudioMetadata().isRecordable() )
		{
			String identifier = audioPacket.getAudioMetadata().getIdentifier();
			
			if( mRecorders.containsKey( identifier ) )
			{
				RealBufferWaveRecorder recorder = mRecorders.get( identifier );

				if( audioPacket.getType() == AudioPacket.Type.AUDIO )
				{
					recorder.receive( audioPacket.getAudioBuffer() );
				}
				else if( audioPacket.getType() == AudioPacket.Type.END )
				{
					recorder.stop();
					mRecorders.remove( identifier );
				}
			}
			else
			{
				if( audioPacket.getType() == AudioPacket.Type.AUDIO )
				{
					String filePrefix = getFilePrefix( audioPacket );
					
					RealBufferWaveRecorder recorder = 
						new RealBufferWaveRecorder( mThreadPoolManager, 
								AUDIO_SAMPLE_RATE, filePrefix );
					
					recorder.start();

					recorder.receive( audioPacket.getAudioBuffer() );
					mRecorders.put( identifier, recorder );
				}
			}
		}
	}
	
	/**
	 * Constructs a file name and path for an audio recording
	 */
	private String getFilePrefix( AudioPacket packet )
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( SystemProperties.getInstance()
				.getApplicationFolder( "recordings" ) );
		
		sb.append( File.separator );
		
		Metadata systemMetadata = packet.getAudioMetadata()
				.getMetadata( MetadataType.SYSTEM );

		sb.append( systemMetadata != null ? 
				systemMetadata.getValue() : "UNKNOWN_SYSTEM" );
		
		Metadata toMetadata = packet.getAudioMetadata()
				.getMetadata( MetadataType.TO );
		
		if( toMetadata != null )
		{
			sb.append( "_TO_" );
			sb.append( toMetadata.getValue() );
		}

		Metadata fromMetadata = packet.getAudioMetadata()
				.getMetadata( MetadataType.FROM );
		
		if( fromMetadata != null )
		{
			sb.append( "_FROM_" );
			sb.append( fromMetadata.getValue() );
		}

		return sb.toString();
	}

	/**
	 * Returns a list of recorder modules for use in a processing chain.
	 * Note: currently this only returns a baseband recorder.  
	 * 
	 * Create an instance of this RecorderManager class to listen to and centrally
	 * record all decoded audio. 
	 */
	public static ComplexBufferWaveRecorder getBasebandRecorder( 
			ThreadPoolManager threadPoolManager, String channelName )
	{
		StringBuilder sb = new StringBuilder();
        sb.append( SystemProperties.getInstance()
        					.getApplicationFolder( "recordings" ) );
        sb.append( File.separator );
        sb.append( channelName );
        sb.append(  "_baseband" );

        return new ComplexBufferWaveRecorder( threadPoolManager, 
        		AUDIO_SAMPLE_RATE, sb.toString() );
	}
	
	/**
	 * Removes any recorders where the recent activity timestamp has exceeded
	 * the retention threshold.
	 */
	public class RecorderProcessor implements Runnable
	{
		@Override
		public void run()
		{
			if( !mRecorders.isEmpty() )
			{
				List<String> keysToRemove = new ArrayList<>();

				for( Entry<String,RealBufferWaveRecorder> entry: mRecorders.entrySet() )
				{
					if( entry.getValue().getLastBufferReceived() + 
						RECORDER_INACTIVITY_THRESHOLD < System.currentTimeMillis() )
					{
						keysToRemove.add( entry.getKey() );
					}
				}
				
				for( String key: keysToRemove )
				{
					RealBufferWaveRecorder recorder = mRecorders.remove( key );
					recorder.dispose();
				}
			}
		}
	}
}
