/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
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
import java.util.HashMap;
import java.util.Map;

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
	
	private Map<String,RealBufferWaveRecorder> mRecorders = new HashMap<>();
	
	private ThreadPoolManager mThreadPoolManager;
	
	private boolean mCanStartNewRecorders = true;

	public RecorderManager( ThreadPoolManager threadPoolManager )
	{
		mThreadPoolManager = threadPoolManager;
	}
	
	public void dispose()
	{
		mThreadPoolManager = null;
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
					RealBufferWaveRecorder finished = mRecorders.remove( identifier );
					finished.stop();
				}
			}
			else if( audioPacket.getType() == AudioPacket.Type.AUDIO )
			{
				if( mCanStartNewRecorders )
				{
					String filePrefix = getFilePrefix( audioPacket );

					try
					{
						RealBufferWaveRecorder recorder = 
								new RealBufferWaveRecorder( mThreadPoolManager, 
										AUDIO_SAMPLE_RATE, filePrefix );
							
						recorder.start();

						recorder.receive( audioPacket.getAudioBuffer() );
						mRecorders.put( identifier, recorder );
					}
					catch( Exception ioe )
					{
						mCanStartNewRecorders = false;
						
						mLog.error( "Error attempting to start new audio wave recorder."
							+ "  All (future) audio recording is disabled", ioe ); 
					}
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
			
			Metadata fromMetadata = packet.getAudioMetadata()
					.getMetadata( MetadataType.FROM );
			
			if( fromMetadata != null )
			{
				sb.append( "_FROM_" );
				sb.append( fromMetadata.getValue() );
			}
		}
		else
		{
			Metadata siteMetadata = packet.getAudioMetadata()
					.getMetadata( MetadataType.SITE_ID );
			
			if( siteMetadata != null )
			{
				sb.append( "_" );
				sb.append( siteMetadata.getValue() );
			}
			
			Metadata channelMetadata = packet.getAudioMetadata()
					.getMetadata( MetadataType.CHANNEL_NAME );
			
			if( channelMetadata != null )
			{
				sb.append( "_" );
				sb.append( channelMetadata.getValue() );
			}
		}

		return sb.toString();
	}

	/**
	 * Constructs a baseband recorder for use in a processing chain. 
	 */
	public ComplexBufferWaveRecorder getBasebandRecorder( String channelName )
	{
		StringBuilder sb = new StringBuilder();
        sb.append( SystemProperties.getInstance()
        					.getApplicationFolder( "recordings" ) );
        sb.append( File.separator );
        sb.append( channelName );
        sb.append(  "_baseband" );

        return new ComplexBufferWaveRecorder( mThreadPoolManager, 
        		AUDIO_SAMPLE_RATE, sb.toString() );
	}
}
