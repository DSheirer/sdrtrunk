/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2017 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.source.recording;

import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Recording implements Comparable<Recording>, ISourceEventProcessor
{
	private RecordingConfiguration mConfiguration;
	
	private List<TunerChannelSource> mTunerChannels = new ArrayList<TunerChannelSource>();
	
	private long mCenterFrequency;
	
	private SettingsManager mSettingsManager;
	
	public Recording( SettingsManager settingsManager,
					  RecordingConfiguration configuration )
	{
		mSettingsManager = settingsManager;
		mConfiguration = configuration;
		mCenterFrequency = mConfiguration.getCenterFrequency();
	}
	
	public void setAlias( String alias )
	{
		mConfiguration.setAlias( alias );
//		mSettingsManager.save();
	}
	
	public void setRecordingFile( File file ) throws SourceException
	{
		if( hasChannels() )
		{
			throw new SourceException( "Recording source - can't change "
				+ "recording file while channels are enabled against the "
				+ "current recording." );
		}
		
		mConfiguration.setFilePath( file.getAbsolutePath() );
//		mSettingsManager.save();
	}
	
	/**
	 * Indicates if this recording is currently playing/providing samples to
	 * tuner channel sources
	 */
	public boolean hasChannels()
	{
		return mTunerChannels.size() > 0;
	}
	
    public TunerChannelSource getChannel( TunerChannel channel )
    {
	    // TODO Auto-generated method stub
	    return null;
    }

	public RecordingConfiguration getRecordingConfiguration()
	{
		return mConfiguration;
	}
	
	public String toString()
	{
		return mConfiguration.getAlias();
	}

	@Override
    public void process(SourceEvent event ) throws SourceException
    {
		if( event.getEvent() == SourceEvent.Event.NOTIFICATION_FREQUENCY_CHANGE )
		{
			long frequency = event.getValue().longValue();
			
			mConfiguration.setCenterFrequency( frequency );
			
//			mSettingsManager.save();

			mCenterFrequency = frequency;

			for( TunerChannelSource channel: mTunerChannels )
			{
				channel.process( event );
			}
		}
    }

	@Override
    public int compareTo( Recording other )
    {
	    return getRecordingConfiguration().getAlias()
	    		.compareTo( other.getRecordingConfiguration().getAlias() );
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Recording)) return false;
        return compareTo((Recording) o) == 0;
    }
}
